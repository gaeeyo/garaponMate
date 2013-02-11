package jp.syoboi.android.garaponmate.data;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jp.syoboi.android.garaponmate.App;

public class CacheFile {

	static final String TAG = "CacheFile";

	int			mMaxCount;
	File		mIndexFile;
	File		mDir;
	Handler		mHandler = new Handler();

	LinkedHashMap<String, Long>	mItems;

	Runnable	mPostSave = new Runnable() {
		@Override
		public void run() {
			saveIndex();
		}
	};

	public CacheFile(File dir, int maxCount) {
		mDir = dir;
		mIndexFile = new File(dir, "index");
		mMaxCount = maxCount;
	}

	public synchronized File alloc(String key) {
		ensureIndex();
		File file = get(key);
		if (file == null) {
			if (App.DEBUG) {
				Log.v(TAG, "alloc key:" + key);
			}
			mHandler.removeCallbacks(mPostSave);
			mHandler.postDelayed(mPostSave, 5000);

			long newId = System.currentTimeMillis();

			for (;newId < Long.MAX_VALUE; newId++) {
				boolean exists = false;
				for (Long x: mItems.values()) {
					if (x == newId) {
						exists = true;
						break;
					}
				}
				if (!exists) {
					break;
				}
			}
			mItems.put(key, newId);
			file = getFile(newId);
		}
		return file;
	}

	public synchronized File get(String key) {
		ensureIndex();
		Long id = mItems.get(key);
		if (id == null) {
			return null;
		}
		else {
			return getFile(id);
		}
	}

	File getFile(long id) {
		return new File(mDir, String.valueOf(id));
	}

	void ensureIndex() {
		if (mItems == null) {
			mItems = new LinkedHashMap<String,Long>();
			loadIndex();
		}
	}

	/**
	 * キャッシュディレクトリでインデックスに記録されていないゴミを削除する
	 */
	void deleteUntrackedFile() {
		HashSet<Long> ids = new HashSet<Long>();
		for (Long id: mItems.values()) {
			ids.add(id);
		}

		File [] files = mDir.listFiles();
		if (files != null) {
			for (File f: files) {
				try {
					Long id = Long.parseLong(f.getName(), 10);
					if (!ids.contains(id)) {
						if (App.DEBUG) {
							Log.v(TAG, "キャッシュのゴミ削除: " + f.getPath());
						}
						f.delete();
					}
				}
				catch (NumberFormatException e) {
					;
				}
			}
		}
	}

	void loadIndex() {
		Log.v(TAG, "インデックス読み込み");
		File indexFile = mIndexFile;
		try {
			FileInputStream fis = new FileInputStream(indexFile);

			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						new GZIPInputStream(fis),
						"UTF-8"));

				String line;
				while ((line = in.readLine()) != null) {
					int pos = line.indexOf("\t");
					if (pos != -1) {
						try {
							Long id = Long.valueOf(line.substring(0, pos));
							String url = line.substring(pos + 1);
							mItems.put(url, id);
						}
						catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}
			finally {
				if (fis != null) {
					fis.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.v(TAG, "インデックスのサイズ: " + mItems.size());
		deleteUntrackedFile();
	}

	/**
	 * 溜まったキャッシュを削除
	 * mItems から不要になったデータを取り除き、取り除かれたキャッシュのidの配列を返す
	 * @return
	 */
	synchronized long [] trim() {
		long [] removeItems = null;
		if (mItems.size() > mMaxCount) {

			int deleteCount = Math.max(mMaxCount / 10, 5);
			removeItems = new long [deleteCount];
			String [] urls = new String [deleteCount];

			Iterator<String> keys = mItems.keySet().iterator();
			for (int j=0; j<deleteCount; j++) {
				String url = keys.next();
				if (App.DEBUG) {
					Log.v(TAG, "キャッシュ削除予定 url:" + url);
				}
				urls[j] = url;
			}

			for (int j=0; j<deleteCount; j++) {
				removeItems[j] = mItems.remove(urls[j]);
			}

		}
		return removeItems;
	}

	synchronized LinkedHashMap<String, Long> cloneItems() {
		return new LinkedHashMap<String, Long>(mItems);
	}

	void saveIndex() {
		if (App.DEBUG) {
			Log.v(TAG, "インデックス保存");
		}

		final long [] deletedIds = trim();
		final LinkedHashMap<String, Long> items = cloneItems();

		// スレッドでファイルの物理削除とキャッシュのインデックスを出力
		new Thread() {
			@Override
			public void run() {
				// キャッシュを削除
				if (deletedIds != null) {
					for (long id: deletedIds) {
						if (App.DEBUG) {
							Log.v(TAG, "キャッシュを削除 id:" + id);
						}
						try {
							File f = getFile(id);
							f.delete();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				// キャッシュのインデックスを書き出し
				try {
					File indexFile = mIndexFile;
					indexFile.getParentFile().mkdirs();

					FileOutputStream fos = new FileOutputStream(indexFile);
					OutputStreamWriter osw = null;
					try {
						osw = new OutputStreamWriter(
								new GZIPOutputStream(fos), "UTF-8");
						for (String url: items.keySet()) {
							Long id = items.get(url);
							osw.write(String.valueOf(id));
							osw.write("\t");
							osw.write(url);
							osw.write("\n");
						}
					}
					finally {
						if (osw != null) {
							osw.close();
						}
						if (fos != null) {
							fos.close();
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

}
