package jp.syoboi.android.garaponmate.data;

import android.os.Handler;
import android.text.TextUtils;
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

	LinkedHashMap<String, CacheEntry>	mItems;

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

	public synchronized CacheEntry alloc(String url) {
		ensureIndex();
		CacheEntry entry = get(url);
		if (entry == null) {
			if (App.DEBUG) {
				Log.v(TAG, "alloc key:" + url);
			}
			mHandler.removeCallbacks(mPostSave);
			mHandler.postDelayed(mPostSave, 5000);

			long newId = System.currentTimeMillis();

			for (;newId < Long.MAX_VALUE; newId++) {
				boolean exists = false;
				for (CacheEntry e: mItems.values()) {
					if (e.id == newId) {
						exists = true;
						break;
					}
				}
				if (!exists) {
					break;
				}
			}
			entry = new CacheEntry(this, newId);
			mItems.put(url,  entry);
		}
		return entry;
	}

	public synchronized CacheEntry get(String url) {
		ensureIndex();
		return mItems.get(url);
	}

	File getFile(CacheEntry entry) {
		mDir.mkdirs();
		return new File(mDir, String.valueOf(entry.id));
	}

	void ensureIndex() {
		if (mItems == null) {
			mItems = new LinkedHashMap<String,CacheEntry>();
			loadIndex();
		}
	}

	/**
	 * キャッシュディレクトリでインデックスに記録されていないゴミを削除する
	 */
	void deleteUntrackedFile() {
		HashSet<Long> ids = new HashSet<Long>();
		for (CacheEntry entry: mItems.values()) {
			ids.add(entry.id);
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
					String [] values = line.split("\t");
					if (values.length >= 4) {
						try {
							CacheEntry entry = new CacheEntry(this,
									Long.parseLong(values[0], 10));
							entry.lastModified = values[2];
							entry.etag = values[3];

							String url = values[1];
							mItems.put(url, entry);
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
	synchronized CacheEntry [] trim() {
		CacheEntry [] removeItems = null;
		if (mItems.size() > mMaxCount) {

			int deleteCount = Math.max(mMaxCount / 10, 5);
			removeItems = new CacheEntry [deleteCount];
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

	synchronized LinkedHashMap<String, CacheEntry> cloneItems() {
		return new LinkedHashMap<String, CacheEntry>(mItems);
	}

	void saveIndex() {
		if (App.DEBUG) {
			Log.v(TAG, "インデックス保存");
		}

		final CacheEntry [] deletedEntries = trim();
		final LinkedHashMap<String, CacheEntry> items = cloneItems();

		// スレッドでファイルの物理削除とキャッシュのインデックスを出力
		new Thread() {
			@Override
			public void run() {
				// キャッシュを削除
				if (deletedEntries != null) {
					for (CacheEntry entry: deletedEntries) {
						if (App.DEBUG) {
							Log.v(TAG, "キャッシュを削除 id:" + entry);
						}
						try {
							File f = getFile(entry);
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
							CacheEntry entry = items.get(url);
							osw.write(String.valueOf(entry.id));
							osw.write("\t");
							osw.write(url);
							osw.write("\t");
							if (!TextUtils.isEmpty(entry.lastModified)) {
								osw.write(entry.lastModified);
							}
							osw.write("\t");
							if (!TextUtils.isEmpty(entry.etag)) {
								osw.write(entry.etag);
							}
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
