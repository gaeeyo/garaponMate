package jp.syoboi.android.garaponmate.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.utils.Utils;

public class MySearchSuggestionsProvider extends ContentProvider {

	private static final String TAG = "MySearchSuggestionsProvider";

	private static Semaphore sGaraponSearchSemaphore = new Semaphore(1);

	private Thread		mGaraponSearchThread;

	private WeakHashMap<String,SearchResult> mSearchCache = new WeakHashMap<String,SearchResult>();

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		String query = (selectionArgs != null && selectionArgs.length > 0)
				? selectionArgs[0] : null;

		if (App.DEBUG) {
			Log.d(TAG, "query " + query);
		}

		MyMatrixCursor matrix = new MyMatrixCursor(new String [] {
			BaseColumns._ID,
			SearchManager.SUGGEST_COLUMN_ICON_1,
			SearchManager.SUGGEST_COLUMN_TEXT_1,
			SearchManager.SUGGEST_COLUMN_TEXT_2,
			SearchManager.SUGGEST_COLUMN_QUERY,
		});

		// 履歴を検索して後ろに追加
		Uri uriRecent = Uri.parse(
				"content://" + MySearchRecentSuggestionsProvider.AUTHORITY
				+ uri.getPath() + "?" + uri.getQuery() + uri.getFragment());

		Cursor recent = getContext().getContentResolver().query(
				uriRecent, projection, selection, selectionArgs, sortOrder);

		MyCursor cur = new MyCursor(matrix, recent);


		// 字幕を検索するクエリを追加
		if (!TextUtils.isEmpty(query)) {
			matrix.addRow(new Object [] { "1",
					R.drawable.ic_menu_search_caption,
					getContext().getString(R.string.searchCaptionFmt, query),
					"",
					App.SEARCH_QUERY_PREFIX_CAPTION + query });

			SearchResult sr = getSearchCache(query);
			if (sr != null) {
				setSearchResult(query, cur, sr);
			}
			else {
				// 番組を検索
				synchronized (TAG) {
					if (!TextUtils.isEmpty(query)) {
						cancelGaraponSearch();
						mGaraponSearchThread = garaponSearch(cur, query);
						mGaraponSearchThread.start();
					} else {
						cancelGaraponSearch();
					}
				}
			}
		}
		return cur;
	}

	void cancelGaraponSearch() {
		if (mGaraponSearchThread != null) {
			mGaraponSearchThread.interrupt();
			mGaraponSearchThread = null;
		}
	}

	void putSearchCache(String query, SearchResult result) {
		synchronized (mSearchCache) {
			mSearchCache.put(query, result);
		}
	}

	SearchResult getSearchCache(String query) {
		synchronized (mSearchCache) {
			return mSearchCache.get(query);
		}
	}

	Thread garaponSearch(final MyCursor cur, final String query) {


		cur.getExtras().putBoolean(SearchManager.CURSOR_EXTRA_KEY_IN_PROGRESS, true);

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					SearchParam sp = new SearchParam();
					sp.keyword = query;
					sp.count = 25;

					sGaraponSearchSemaphore.acquire();
					try {
						SearchResult sr = getSearchCache(query);
						if (sr == null) {
							sr = GaraponClientUtils.search(sp);
							putSearchCache(query, sr);
						}
						synchronized (TAG) {
							if (!Thread.interrupted()) {
								cur.getExtras().putBoolean(SearchManager.CURSOR_EXTRA_KEY_IN_PROGRESS, false);

								setSearchResult(query, cur, sr);
								cur.matrix.notifyChange();
							}
						}

					} finally {
						sGaraponSearchSemaphore.release();
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		};

		return t;
	}

	/**
	 * 検索結果をcursorに追加
	 * @param cur
	 * @param sr
	 */
	void setSearchResult(String query, MyCursor cur, SearchResult sr) {

		long id = 2;
		for (Program p: sr.program) {
			String title = Utils.createSearchTitle(p.title);
			if (cur.addTitle(title)) {
				String description = null;
				int matchPos = p.description.indexOf(query);
				if (matchPos != -1) {
					description = p.description;
					int start = Math.max(0, matchPos - 3);
					int end = Math.min(description.length(), matchPos + 100);
					if (start > 0) {
						description = "…" + description.substring(start, end);
					} else {
						description = description.substring(start, end);
					}
				}

				Object [] row = {
					id++,
					android.R.drawable.ic_menu_search,
					title,
					description,
					title,
				};
				cur.matrix.addRow(row);
			}
		}
	}

	private static class MyMatrixCursor extends MatrixCursor {

		public MyMatrixCursor(String[] columnNames) {
			super(columnNames);
		}
		public void notifyChange() {
			onChange(false);
		}
	}

	private static class MyCursor extends MergeCursor {

		private Bundle mExtras;
		public MyMatrixCursor matrix;
		public Cursor recent;

		private HashSet<String> mTitles;

		public MyCursor(MyMatrixCursor matrix, Cursor recent) {
			super(new Cursor [] { matrix, recent });
			this.matrix = matrix;
			this.recent = recent;

			mTitles = new HashSet<String>();
			int columnText1 = recent.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1);
			while (recent.moveToNext()) {
				mTitles.add(recent.getString(columnText1));
			}
			recent.moveToFirst();
		}

		@Override
		public Bundle getExtras() {
			if (mExtras == null) {
				mExtras = new Bundle();
			}
			return mExtras;
		}

		public boolean addTitle(String title) {
			return mTitles.add(title);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

}
