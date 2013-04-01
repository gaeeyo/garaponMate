package jp.syoboi.android.garaponmate.data;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.data.ImageLoader.Queue.QueueItem;
import jp.syoboi.android.garaponmate.view.MyImageViewInterface;

public class ImageLoader {

	static final String TAG = "ImageCache";

	public static final int PRIORITY_PHOTO = 0;
	public static final int PRIORITY_PHOTO_PREFETCH = 1;
	public static final int PRIORITY_USER_ICON = 2;

	static final int DOWNLOAD_THREAD_COUNT = 2;
	LoadTask[] mTasks = new LoadTask[DOWNLOAD_THREAD_COUNT];
	public static boolean WAIT_CALLBACK;

	CacheFile 	mCacheFile;
	Resources	mResources;

	Queue mQueue = new Queue();
	BitmapCache	mMemCache;

	public ImageLoader(Context c, File dir, int maxCount) {
		mCacheFile = new CacheFile(dir, maxCount);
		mResources = c.getApplicationContext().getResources();

		int cacheSize = 4 * 1024 * 1024;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			cacheSize = (int)(Runtime.getRuntime().maxMemory() * 15 / 100);
		}
		cacheSize = Math.min(cacheSize, 10 * 1024 * 1024);

		mMemCache = new BitmapCache(cacheSize);
	}

	public void clearMemCache() {
		mMemCache.clear();
	}

	public void removeImage(MyImageViewInterface iv) {
		Drawable d = iv.getDrawable();
		if (d instanceof ImageLoaderDrawable) {
			ImageLoaderDrawable ild = (ImageLoaderDrawable)d;
			QueueItem item = mQueue.remove(ild);

			if (App.DEBUG) {
				assert(item != null);
			}
		}
	}

    public boolean loadImage(MyImageViewInterface iv, String url, int priority,
    		int maxWidth, int maxHeight, boolean rounded, int loadingImageId, ProgressBar progress) {
    	return loadImage(iv, url, priority, maxWidth, maxHeight, rounded ? "r" : "", loadingImageId, progress);
    }

    public boolean loadImage(MyImageViewInterface iv, String url, int priority,
    		int maxWidth, int maxHeight, String option, int loadingImageId, ProgressBar progress) {

        removeImage(iv);

        if (TextUtils.isEmpty(url)) {
        	if (progress != null) {
        		progress.setVisibility(View.INVISIBLE);
        	}
        	if (loadingImageId == 0) {
        		iv.setImageDrawable(null);
        	} else {
        		iv.setImageResource(loadingImageId);
        	}
            return false;
        }

        String cacheKey = getCacheKey(url, maxWidth, maxHeight, option);
        Bitmap bmp = mMemCache.get(cacheKey);
        if (bmp != null) {
        	// メモリ上のキャッシュがあればすぐに表示
        	if (progress != null) {
        		progress.setVisibility(View.INVISIBLE);
        	}
        	iv.onLoaded(bmp);
            iv.setImageDrawable(new BitmapDrawable(mResources, bmp));
            return false;
        } else {
        	// 読み込み/デコードキューに積む
        	if (progress != null) {
        		progress.setVisibility(View.VISIBLE);
        	}
            ImageLoaderDrawable loaderDrawable;
            if (loadingImageId != 0) {
            	loaderDrawable = new ImageLoaderDrawable(mResources.getDrawable(loadingImageId));
            } else {
            	loaderDrawable = new ImageLoaderDrawable(null);
            }
            loaderDrawable.setClient(iv);

            // 低解像度のキャッシュがあれば仮に設定しておく
            bmp = mMemCache.getThumbnail(url);
            if (bmp != null) {
            	loaderDrawable.setDrawable(new BitmapDrawable(mResources,bmp));
            }

            iv.setImageDrawable(loaderDrawable);
            mQueue.add(loaderDrawable, url, priority, maxWidth, maxHeight, option, iv, progress);
            startLoad();
            return true;
        }
    }

    public boolean isMemCached(String url) {
    	String cacheKey = getCacheKey(url, 0, 0, "");
    	return mMemCache.get(cacheKey) != null;
    }

    public boolean isCached(String url) {
    	String cacheKey = getCacheKey(url, 0, 0, "");
    	if (mMemCache.get(cacheKey) != null) {
    		return true;
    	}
    	return mCacheFile.get(url) != null;
    }

    public File getFile(String url) {
    	return mCacheFile.get(url);
    }

    public void loadPhoto(MyImageViewInterface iv, String url, int loadingImageId, ProgressBar progress) {
    	loadImage(iv, url, PRIORITY_PHOTO, 0, 0,
    			"", loadingImageId, progress);
    }

	void startLoad() {
		// キューから次にダウンロードするアイテムを探す
		QueueItem item = getNextQueue();

		if (item != null) {
			final int slot = findEmptySlot();
			if (slot != -1) {
//				Log.v(TAG, "タスクを開始");
				mTasks[slot] = new LoadTask(mCacheFile, item) {
					@Override
					protected void onPostExecute(Bitmap bmp) {
						super.onPostExecute(bmp);
						mTasks[slot] = null;
						onLoaded(this, bmp, downloaded);
					}
					@Override
					protected void onCancelled() {
						super.onCancelled();
						mTasks[slot] = null;
						if (mQueue.size() > 0) {
							startLoad();
						}
					}
				};
				mTasks[slot].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
		else {
//			Log.v(TAG, "キューは空です");
		}
	}

	void onLoaded(LoadTask task, Bitmap bmp, boolean downloaded) {

		QueueItem item = task.item;

		if (bmp != null) {
		    mMemCache.put(item.getCacheKey(), bmp);
		}

		for (int j=mQueue.size() - 1; j>=0; j--) {
			QueueItem i = mQueue.get(j);
			if (item.isSameSpec(i)) {
				MyImageViewInterface client = i.listener.getClient();
				if (client != null) {
					client.onLoaded(bmp);
				}
				if (bmp != null) {
					i.listener.setDrawable(new BitmapDrawable(mResources, bmp));
				}
				if (i.progress != null) {
					ProgressBar progress = i.progress.get();
					if (progress != null) {
						progress.setVisibility(View.INVISIBLE);
					}
					i.progress = null;
				}

				mQueue.removeAt(j);
			}
		}
		if (mQueue.size() > 0) {
			startLoad();
		}
	}

	QueueItem getNextQueue() {
		while (true) {
			QueueItem i = getNextQueue_internal();
			if (i == null) {
				return null;
			}
			MyImageViewInterface ii = i.ii.get();
			if (ii == null || ii.getDrawable() != i.listener) {
				// 無効な読み込み待ちを削除
				mQueue.remove(i.listener);
				continue;
			}
			return i;
		}
	}

	QueueItem getNextQueue_internal() {
		QueueItem best = null;
		for (QueueItem i : mQueue.mQueue) {
			if (!isLoading(i.url)) {
				if (best == null || i.priority < best.priority) {
					best = i;
				}
			}
		}
		return best;
	}

	public boolean isLoading(String url) {
		for (LoadTask t : mTasks) {
			if (t != null && TextUtils.equals(t.item.url, url)) {
				return true;
			}
		}
		return false;
	}

	int findEmptySlot() {
		for (int j = 0; j < mTasks.length; j++) {
			if (mTasks[j] == null) {
				return j;
			}
		}
		return -1;
	}

	public static class Item {
		public int id;
		public Bitmap bitmap;
		public boolean saved;
	}

	public static class Queue {
		private final ArrayList<QueueItem> mQueue = new ArrayList<QueueItem>();

		public int size() {
			return mQueue.size();
		}

		public void removeAt(int j) {
			mQueue.remove(j);
		}

		public QueueItem get(int index) {
			return mQueue.get(index);
		}

		public QueueItem remove(ImageLoaderDrawable onImageLoader) {
			ArrayList<QueueItem> queue = mQueue;
			for (int j = 0, count = queue.size(); j < count; j++) {
				if (queue.get(j).listener == onImageLoader) {
//					Log.v(TAG, "キューを破棄");
					return queue.remove(j);
				}
			}
			return null;
		}

		public boolean containsUrl(String url) {
			for (QueueItem i: mQueue) {
				if (TextUtils.equals(i.url, url)) {
					return true;
				}
			}
			return false;
		}

		public void add(ImageLoaderDrawable onImageLoader, String url,
				int priority, int maxWidth, int maxHeight, String option,
				MyImageViewInterface ii,  ProgressBar progress) {

			remove(onImageLoader);
			mQueue.add(new QueueItem(onImageLoader, url, priority, maxWidth ,maxHeight, option, ii, progress));
		}


		public static class QueueItem {
			public String url;
			public ImageLoaderDrawable listener;
			public int priority;
			public int maxWidth;
			public int maxHeight;
            public String option;
            public WeakReference<MyImageViewInterface> ii;
            public WeakReference<ProgressBar> progress;

            public QueueItem(ImageLoaderDrawable listener, String url,
            		int priority, int maxWidth, int maxHeight, String option,
            		MyImageViewInterface ii, ProgressBar progress) {

                this.url = url;
                this.listener = listener;
                this.priority = priority;
                this.maxWidth = maxWidth;
                this.maxHeight = maxHeight;
                this.option = option;
                if (ii != null) {
                	this.ii = new WeakReference<MyImageViewInterface>(ii);
                }
                if (progress != null) {
                	this.progress = new WeakReference<ProgressBar>(progress);
                }
            }

            public boolean isSameSpec(QueueItem i) {
                return url.equals(i.url) && maxWidth == i.maxWidth
                        && maxHeight == i.maxHeight && TextUtils.equals(option, i.option);
            }

            public String getCacheKey() {
                return ImageLoader.getCacheKey(url, maxWidth, maxHeight, option);
            }
		}
	}

	private static class LoadTask extends AsyncTask<Object, Object, Bitmap> {

		static final Object DECODING = new Object();

		private final CacheFile 		mCacheFile;
		private final QueueItem		item;
		public boolean			downloaded;

		public LoadTask(CacheFile cache, QueueItem item) {
			mCacheFile = cache;
			this.item = item;
		}

		@Override
		protected Bitmap doInBackground(Object... params) {

			QueueItem item = this.item;
			File f = mCacheFile.alloc(item.url);

			try {
				Bitmap bmp = null;
				if (!f.exists()) {
					if (downloadToFile(item.url, f)) {
						downloaded = true;
						bmp = decodeBitmap(f.getPath(), item.maxWidth, item.maxHeight);
					}
				} else {
					bmp = decodeBitmap(f.getPath(), item.maxWidth, item.maxHeight);
				}

                if (bmp != null) {
            		Bitmap effected = effect(bmp, item.option);
            		if (effected != null && effected != bmp) {
                		bmp.recycle();
                		bmp = effected;
            		}
                }

                for (int j=0; j<20 && ImageLoader.WAIT_CALLBACK; j++) {
                	try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
				return bmp;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}

			return null;
		}

		/**
		 *
		 * @param bmp
		 * @param option
		 * 	"r" 画像の角を丸める
		 *  "border <radius> <borderWidth> <borderColor>"
		 * @return
		 */
		Bitmap effect(Bitmap bmp, String option) {

//			if (option.startsWith("border")) {
//				String [] params = option.split(" ");
//				float radius = Float.valueOf(params[1]);
//				float borderWidth = Float.valueOf(params[2]);
//				int borderColor = Integer.valueOf(params[3]);
//	        	Bitmap rounded = EffectUtils.round(bmp, radius,
//	        			borderWidth, borderColor);
//	        	return rounded;
//			}
//			else if ("r".equals(option)) {
//				return EffectUtils.round(bmp, bmp.getWidth() / 9);
//			}
			return null;
		}


		Bitmap decodeBitmap(String filename, int maxWidth, int maxHeight) {
			synchronized (DECODING) {
				if (isCancelled()) {
					return null;
				}
				if (maxWidth > 0 && maxHeight > 0) {
					final BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					BitmapFactory.decodeFile(filename, options);

					options.inSampleSize = calculateInSampleSize(options, maxWidth,
							maxHeight);

					options.inJustDecodeBounds = false;
					Bitmap bmp = BitmapFactory.decodeFile(filename, options);
					if (bmp != null && bmp.getWidth() > maxWidth && bmp.getHeight() > maxHeight) {
						int scaledWidth = maxWidth;
						int scaledHeight = Math.round((float)bmp.getHeight() * maxWidth / bmp.getWidth());

						if (scaledHeight > maxHeight) {
							scaledHeight = maxHeight;
							scaledWidth = Math.round((float)bmp.getWidth() * maxHeight / bmp.getHeight());
						}

						Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, scaledWidth, scaledHeight, true);
						bmp.recycle();
						bmp = scaledBmp;
					}
					//Log.v(TAG, "decodeBitmap(scaled) width:" + bmp.getWidth());

					return bmp;
				} else {
					Bitmap bmp = BitmapFactory.decodeFile(filename);
					//Log.v(TAG, "decodeBitmap width:" + bmp.getWidth());
					return bmp;
				}
			}
		}

		boolean downloadToFile(String url, File f) {
			f.getParentFile().mkdirs();

			File tmpFile = new File(f.getPath() + ".tmp");
			long start = System.currentTimeMillis();
			if (App.DEBUG) {
				Log.v(TAG, "読み込み開始: " + url + " => " + tmpFile.getPath());
			}
			HttpURLConnection connection;
			try {
				long size = 0;
				byte[] buf = new byte[4096];

				connection = (HttpURLConnection) new URL(url).openConnection();

				if (connection.getResponseCode() != 200) {
					throw new IOException("HTTP Status " + connection.getResponseCode());
				}

//				connection.setRequestProperty("User-Agent", AppVersion.USER_AGENT);
				InputStream is = new BufferedInputStream(
						connection.getInputStream());
				FileOutputStream fos = new FileOutputStream(tmpFile);

				boolean success = false;
				try {
					int readSize;
					while ((readSize = is.read(buf)) != -1) {
						fos.write(buf, 0, readSize);
						size += readSize;
					}
					success = true;
				} finally {
					is.close();
					fos.close();
				}

				long time = System.currentTimeMillis() - start;
				if (success) {
					if (App.DEBUG) {
						Log.v(TAG, "読み込み完了 "
								+ String.format("%3.1fKB %.1fsec %3.0fkbps", size/1024f,
										time/1000f,
										size/(time/1000f)/1024f));
					}
					tmpFile.renameTo(f);
				} else {
					if (App.DEBUG) {
						Log.v(TAG, "読み込みエラー "
								+ String.format("%3.1fKB %.1fsec %3.0fkbps", size/1024f,
										time/1000f,
										size/(time/1000f)/1024f));
					}
					tmpFile.delete();
				}
				return success;

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		public static int calculateInSampleSize(BitmapFactory.Options options,
				int reqWidth, int reqHeight) {
			// Raw height and width of image
			final int height = options.outHeight;
			final int width = options.outWidth;
			int inSampleSize = 1;

			if (height > reqHeight || width > reqWidth) {
				if (width > height) {
					inSampleSize = Math.round((float) height
							/ (float) reqHeight);
				} else {
					inSampleSize = Math.round((float) width / (float) reqWidth);
				}

				// This offers some additional logic in case the image has a
				// strange
				// aspect ratio. For example, a panorama may have a much larger
				// width than height. In these cases the total pixels might
				// still
				// end up being too large to fit comfortably in memory, so we
				// should
				// be more aggressive with sample down the image (=larger
				// inSampleSize).

				final float totalPixels = width * height;

				// Anything more than 2x the requested pixels we'll sample down
				// further.
				final float totalReqPixelsCap = reqWidth * reqHeight * 2;

				while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
					inSampleSize++;
				}
			}
			return inSampleSize;
		}

	}

	public static class ImageLoaderDrawable extends Drawable {
		public Drawable	mDrawable;
		int				mAlpha = 255;
		Rect			mBounds = new Rect();
		WeakReference<MyImageViewInterface>	mClient;

		public ImageLoaderDrawable(Drawable loading) {
			mDrawable = loading;
		}

		public void setClient(MyImageViewInterface client) {
			mClient = new WeakReference<MyImageViewInterface>(client);
		}

		public MyImageViewInterface getClient() {
			return (mClient != null ? mClient.get() : null);
		}

		@Override
		public void draw(Canvas canvas) {
			if (mDrawable != null) {
				mDrawable.draw(canvas);
			}
		}

		@Override
		public void setBounds(int left, int top, int right, int bottom) {
			if (mDrawable != null) {
				mBounds.set(left, top, right, bottom);
				mDrawable.setBounds(left, top, right, bottom);
			}
			super.setBounds(left, top, right, bottom);
		}

		@Override
		public int getIntrinsicWidth() {
			if (mDrawable != null) {
				return mDrawable.getIntrinsicWidth();
			}
			return -1;
		}

		@Override
		public int getIntrinsicHeight() {
			if (mDrawable != null) {
				return mDrawable.getIntrinsicHeight();
			}
			return -1;
		}

		public void setDrawable(Drawable d) {
			d.setBounds(getBounds());
			d.setAlpha(mAlpha);
			mDrawable = d;
			if (mDrawable != null) {
				mDrawable.setBounds(mBounds);
			}
			invalidateSelf();
		}

		public Bitmap getBitmap() {
			if (mDrawable instanceof BitmapDrawable) {
				return ((BitmapDrawable) mDrawable).getBitmap();
			}
			return null;
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}

		@Override
		public void setAlpha(int alpha) {
			mAlpha = alpha;
			if (mDrawable != null) {
				mDrawable.setAlpha(alpha);
			}
		}
	}

	static StringBuilder sCacheKeyBuf = new StringBuilder();

	/**
	 * キャッシュのキーに使う文字列を生成
	 * "(url)#(width << 16|height)(option)" 形式
	 * @param url
	 * @param width
	 * @param height
	 * @param option
	 * @return
	 */
    public synchronized static String getCacheKey(String url, int width, int height,
            String option) {

    	final StringBuilder buf = sCacheKeyBuf;
    	buf.delete(0, buf.length());
    	buf.append(url);
    	buf.append('#');
    	buf.append(width << 16 | height);
    	buf.append(option);
    	return buf.toString();
    }

    static class BitmapCache {
    	final LruCache<String, Bitmap>		mCache;
    	final HashMap<String, String>		mThumbMap = new HashMap<String,String>();

		public BitmapCache(int maxSize) {
			mCache = new LruCache<String, Bitmap>(maxSize) {
				@Override
				protected void entryRemoved(boolean evicted, String key,
						Bitmap oldValue, Bitmap newValue) {
					super.entryRemoved(evicted, key, oldValue, newValue);

					if (newValue == null) {
						String fullKey = getFullKey(key);
						if (fullKey != null) {
							mThumbMap.remove(fullKey);
						}
					}
				}

				@Override
				protected int sizeOf(String key, Bitmap value) {
					return value.getRowBytes() * value.getHeight();
				};
			};
		}

		public Bitmap get(String key) {
			return mCache.get(key);
		}

		public Bitmap getThumbnail(String url) {
			String fullKey = getFullKey(url);
			if (fullKey == null) {
				fullKey = url;
			}

			if (fullKey != null) {
				String key = mThumbMap.get(fullKey);
				if (key != null) {
					return mCache.get(key);
				}
			}
			return null;
		}

		public void put(String key, Bitmap bmp) {
			mCache.put(key, bmp);

			String fullKey = getFullKey(key);
			if (fullKey != null) {
				mThumbMap.put(fullKey, key);
			}
		}

		String getFullKey(String key) {
			int pos = key.indexOf("=s");
			if (pos != -1) {
				return key.substring(0, pos);
			}
			return null;
		}

		public void clear() {
			mCache.evictAll();
		}

    }
}
