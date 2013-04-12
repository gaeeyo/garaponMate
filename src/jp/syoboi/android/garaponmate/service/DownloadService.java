package jp.syoboi.android.garaponmate.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.DialogActivity;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.utils.Utils;

public class DownloadService extends Service {

	private static final String TAG = "DownloadService";

	static int 						sTaskId = 0;
	HashMap<String,DownloadTask>	mTaskMap = new HashMap<String,DownloadTask>();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (App.DEBUG) {
			Log.d(TAG, "onCreate");
		}
	}

	@Override
	public void onDestroy() {
		if (App.DEBUG) {
			Log.d(TAG, "onDestroy");
		}
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			if (Intent.ACTION_INSERT.equals(action)) {
				Program p = (Program) intent.getSerializableExtra(App.EXTRA_PROGRAM);

				final String id = p.gtvid;
				DownloadTask task = mTaskMap.get(id);
				if (task == null) {
					task = new DownloadTask(this, p, sTaskId++) {
						@Override
						protected void onPostExecute(Object result) {
							super.onPostExecute(result);
							mTaskMap.remove(id);
							stopOrContinue();
						}
						@Override
						protected void onCancelled(Object result) {
							super.onCancelled(result);
							mTaskMap.remove(id);
							stopOrContinue();
						}
					};
					mTaskMap.put(id, task);
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			}
			else if (Intent.ACTION_DELETE.equals(action)) {
				Program p = (Program) intent.getSerializableExtra(App.EXTRA_PROGRAM);
				final String id = p.gtvid;
				DownloadTask task = mTaskMap.get(id);
				if (task != null) {
					task.cancel(true);
				}
			}
		}
		stopOrContinue();
		return START_STICKY;
	}

	void stopOrContinue() {
		if (mTaskMap.size() == 0) {
			stopSelf();
		}
	}

	/**
	 * m3uファイルをダウンロードして、記述されているURLの配列を返す
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static ArrayList<String> downloadM3u(String url) throws IOException, InterruptedException {
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new URL(url).openStream(), "utf-8"));
		String line;

		ArrayList<String> urls = new ArrayList<String>();
		while ((line = br.readLine()) != null) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			if (line.startsWith("#")) {
				continue;
			}
			if (line.length() > 0) {
				urls.add(line);
			}
		}
		return urls;
	}


	/**
	 * 動画のダウンロードタスク
	 */
	private static class DownloadTask extends AsyncTask<Object,Object,Object> {

		private Program mProgram;
		private NotificationManager mNm;
		private Notification	mNotification;
		private Context			mContext;
		private PendingIntent	mPi;
		private long			mDownloadedSize = 0;
		private long			mTotalContentLength;
		private File			mDst;
		private long			mDownloadProgressUpdated;
		private int				mId;
		private Exception		mThreadException;
		private Queue<byte[]>	mBuffers = new ConcurrentLinkedQueue<byte[]>();

		public DownloadTask(Context context, Program p, int id) {
			mId = id;
			mProgram = p;
			mContext = context;
			mNm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
			mDst = getDstFile(p);

			Intent intent = new Intent(mContext, DialogActivity.class);
			intent.setAction(App.ACTION_DOWNLOAD_NOTIFICATION_CLICKED);
			intent.putExtra(App.EXTRA_PROGRAM, mProgram);
			mPi = PendingIntent.getActivity(mContext, id, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);

		}

		@Override
		protected void onPostExecute(Object result) {
			if (App.DEBUG) {
				Log.d(TAG, "bufferes size:" + mBuffers.size());
			}
			mBuffers.clear();
			super.onPostExecute(result);
		}

		@Override
		protected Object doInBackground(Object... params) {

			showDownloadProgress();
			RandomAccessFile out = null;
			boolean success = false;
			long start = System.currentTimeMillis();

			ExecutorService svc = Executors.newFixedThreadPool(7);
			try {
				final String m3u = GaraponClientUtils.getM3uUrl(mProgram.gtvid);

				mDst.delete();
				out = new RandomAccessFile(mDst, "rw");

				List<String> urls = downloadM3u(m3u);

				List<FileInfo> files = prepareUrls(urls);

				try {
					for (final FileInfo file: files) {
						final RandomAccessFile outFile = out;
						svc.execute(new Runnable() {

							@Override
							public void run() {
								String tsUrl;
								try {
									tsUrl = new URI(m3u).resolve(file.url).toString();
									downloadFile(tsUrl, outFile, file.start, file.length);
								} catch (MalformedURLException e) {
									e.printStackTrace();
									mThreadException = e;
									cancel(true);
								} catch (IOException e) {
									e.printStackTrace();
									mThreadException = e;
									cancel(true);
								} catch (URISyntaxException e) {
									e.printStackTrace();
									mThreadException = e;
									cancel(true);
								}
							}
						});
					}
				} finally {
					svc.shutdown();
				}
				while (true) {
					if (svc.awaitTermination(500, TimeUnit.MILLISECONDS)) {
						break;
					}
				}

				long end = System.currentTimeMillis();

				Log.d(TAG, "ダウンロード時間:" + (end - start) / DateUtils.SECOND_IN_MILLIS + "sec" );

				if (!isCancelled()) {
					success = true;
					showSuccessNotification();
				}
				return null;
			} catch (Exception e) {
				if (mThreadException != null) {
					e = mThreadException;
				}
				mDst.delete();
				svc.shutdownNow();
				e.printStackTrace();
				showErrorNotification(e);
				return e;
			} finally {
				mNm.cancel(App.NOTIFY_ID_DOWNLOADING + mId);
				if (out != null) {
					try {
						out.close();
						if (!success) {
							mDst.delete();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private static class FileInfo {
			public final String url;
			public final int 	start;
			public final int 	length;
			public FileInfo(String url, int start, int length) {
				this.url = url;
				this.start = start;
				this.length = length;
			}
		}

		private List<FileInfo> prepareUrls(List<String> urls) throws Exception {

			ArrayList<FileInfo> results = new ArrayList<FileInfo>();
			Matcher m = Pattern.compile("start=(\\d+)&length=(\\d+)").matcher("");

			// ファイルサイズを計算
			long total = 0;
			for (String url: urls) {
				m.reset(url);
				if (!m.find()) {
					throw new Exception("想定外のTSのURL");
				}

				FileInfo fi = new FileInfo(url,
						Integer.parseInt(m.group(1), 10),
						Integer.parseInt(m.group(2), 10));
				results.add(fi);
				total += fi.length;
			}

			mTotalContentLength = total;
			if (App.DEBUG) {
				Log.d(TAG, "TotalContentLength: " + total);
			}
			return results;
		}

		/**
		 * エラー通知
		 * @param e
		 */
		@SuppressWarnings("deprecation")
		void showErrorNotification(Exception e) {
			Notification notification = new Notification(
					android.R.drawable.stat_notify_error,
					mContext.getString(R.string.downloadError),
					System.currentTimeMillis());

			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(mContext,
					mContext.getString(R.string.downloadError),
					String.valueOf(e.getMessage()),
					mPi);
			mNm.notify(App.NOTIFY_ID_DOWNLOAD_RESULT + mId, notification);
		}

		/**
		 * ダウンロード完了通知
		 */
		@SuppressWarnings("deprecation")
		void showSuccessNotification() {
			Notification notification = new Notification(
					android.R.drawable.stat_sys_download_done,
					mContext.getString(R.string.downloadComplete),
					System.currentTimeMillis());

			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setDataAndType(Uri.fromFile(mDst), "video/ts");

			PendingIntent pi = PendingIntent.getActivity(mContext, mId, i,
					PendingIntent.FLAG_ONE_SHOT);

			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(mContext,
					mContext.getString(R.string.downloadComplete),
					mDst.getName(),
					pi);
			mNm.notify(App.NOTIFY_ID_DOWNLOAD_RESULT + mId, notification);
		}

		/**
		 * 進捗通知
		 */
		@SuppressWarnings("deprecation")
		void showDownloadProgress() {
			final long MB = 1024*1024;
			long now = System.currentTimeMillis();
			if (now - mDownloadProgressUpdated > DateUtils.SECOND_IN_MILLIS) {
				mDownloadProgressUpdated = now;

				String msg;
				if (mTotalContentLength > 0) {
					msg = String.format(Locale.ENGLISH,
						"%d%% (%dMB/%dMB) %s",
						mDownloadedSize * 100 / mTotalContentLength,
						mDownloadedSize / MB, mTotalContentLength / MB,
						mDst.getName());
				} else {
					msg = mDst.getName();
				}

				if (mNotification == null) {
					mNotification = new Notification(
							android.R.drawable.stat_sys_download,
							mContext.getString(R.string.downloading),
							System.currentTimeMillis());
					mNotification.flags = Notification.FLAG_ONGOING_EVENT;
				}
				mNotification.setLatestEventInfo(mContext,
						mContext.getString(R.string.downloading),
						msg, mPi);

				mNm.notify(App.NOTIFY_ID_DOWNLOADING + mId, mNotification);
			}
		}

		private int downloadFile(String url, RandomAccessFile out, int start, int length) throws MalformedURLException, IOException {
//			FileChannel channel = out.getChannel();
			InputStream is = null;
			int totalReadSize = 0;
			try {
				if (App.DEBUG) {
					Log.d(TAG, "downloadFile url:" + url);
				}
				is = Utils.openConnection(url).getInputStream();

				byte [] buf = mBuffers.poll();
				if (buf == null || buf.length < length) {
					int bufSize = length + 4 * 1024;
					if (App.DEBUG) {
						Log.d(TAG, "alloc buf size:" + bufSize);
					}
					buf = new byte [bufSize];
				}

				int pos = 0;
				int readSize;
				while ((readSize = is.read(buf, pos, length - pos)) >= 0) {
					pos += readSize;
					if (pos >= length) {
						break;
					}
				}

				if (App.DEBUG) {
					Log.d(TAG, "readSize:" + pos + " length:" + length
							+ " " + (pos != length ? "ダウンロード失敗" : ""));
				}

				if (pos != length) {
					throw new IOException("取得したデータの長さが異常でした");
				}

//				channel.position(start);
//				channel.write(ByteBuffer.wrap(buf, 0, length));
				synchronized (out) {
					out.seek(start);
					out.write(buf, 0, length);
					mDownloadedSize += length;
				}
				showDownloadProgress();
				mBuffers.add(buf);

			} finally {
				if (is != null) {
					is.close();
				}
			}
			return totalReadSize;
		}

		static File getDstFile(Program p) {
			String fileName = getFilename(p);

			File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
			dir.mkdirs();
			File file = new File(dir, fileName + ".ts");
			return file;
		}

		static String getFilename(Program p) {
			String filename =  p.title;
			if (filename == null) {
				filename = p.gtvid;
			}

			Time t = new Time();
			t.set(p.startdate);
			String dateTimeText = t.format("%Y%m%d-%H%M");

			filename = dateTimeText + " " + filename.replaceAll("./:\\*\\?\\|<>", "_")
					+ " [" + p.ch.bc + "]";

			return filename;
		}

	}
}
