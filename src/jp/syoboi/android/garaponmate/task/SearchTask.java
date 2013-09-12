package jp.syoboi.android.garaponmate.task;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;

import java.io.File;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.utils.Utils;

/**
 * 検索タスク
 * @author naofumi
 *
 */
public class SearchTask extends AsyncTask<Object, Object, Object> {

	private static final long CACHE_LIFE_TIME = 1 * DateUtils.MINUTE_IN_MILLIS;

	private Context			mContext;
	private SearchParam 	mSearchParam;
//	private boolean			mLoadFromCache;
	private File			mCacheFile;

	public SearchTask(Context context, SearchParam param, boolean loadFromCache) {
		mContext = context;
		mSearchParam = param.clone();
//		mLoadFromCache = loadFromCache;

		if (loadFromCache && param.id != 0 && param.page == 1) {
			mCacheFile = App.from(mContext).getSearchResultCacheFile(param.id);
		}
	}

	@Override
	protected Object doInBackground(Object... params) {
		try {
			SearchParam param = mSearchParam;

			long now = Utils.currentTimeMillisJp();

			boolean setRange = false;


			SearchResult cache = getCache(mCacheFile);
			/*
			if (cache != null) {
				if (cache.program != null && cache.program.size() > 0
						&& param.rank == 0) {
					publishProgress(cache);
					// キャッシュがあるときはそのデータを使用する
					param.searchTime = SearchParam.STIME_END;
					param.sdate = cache.timestamp - 1 * DateUtils.HOUR_IN_MILLIS;
//					param.edate = System.currentTimeMillis() + 5 * DateUtils.MINUTE_IN_MILLIS;
					setRange = true;
				}
			}
			*/
			SearchResult sr = GaraponClientUtils.search(param);

			if (setRange) {
				sr.program.merge(cache.program);
				sr.program.trim(50);
			}

			if (mCacheFile != null) {
				sr.timestamp = now;
				Utils.objectToFile(mCacheFile, sr);
			}

			return sr;
		} catch (Throwable t) {
			t.printStackTrace();
			return t;
		}
	}

	SearchResult getCache(File file) {
		if (file != null && file.exists()) {
			try {
				Object obj = Utils.objectFromFile(mCacheFile);
				if (obj instanceof SearchResult) {
					return (SearchResult)obj;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
