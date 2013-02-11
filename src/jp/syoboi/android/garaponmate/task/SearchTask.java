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

public class SearchTask extends AsyncTask<Object, Object, Object> {

	private static final long CACHE_LIFE_TIME = 1 * DateUtils.MINUTE_IN_MILLIS;

	private Context			mContext;
	private SearchParam 	mSearchParam;
//	private boolean			mLoadFromCache;
	private File			mCacheFile;

	public SearchTask(Context context, SearchParam param, boolean loadFromCache) {
		mContext = context;
		mSearchParam = param;
//		mLoadFromCache = loadFromCache;

		if (loadFromCache && param.id != 0 && param.page == 1) {
			mCacheFile = App.getSearchResultCache(mContext, param.id);
		}
	}

	@Override
	protected Object doInBackground(Object... params) {
		try {
			SearchParam param = mSearchParam;

			if (mCacheFile != null) {
				if (mCacheFile.exists()) {
					try {
						Object obj = Utils.objectFromFile(mCacheFile);
						if (obj instanceof SearchResult) {
							SearchResult sr = (SearchResult)obj;
							long now = System.currentTimeMillis();
							if (now - CACHE_LIFE_TIME <= sr.timestamp && sr.timestamp < now + CACHE_LIFE_TIME) {
								return obj;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			SearchResult sr = GaraponClientUtils.search(param);

			if (mCacheFile != null) {
				sr.timestamp = System.currentTimeMillis();
				Utils.objectToFile(mCacheFile, sr);
			}

			return sr;
		} catch (Throwable t) {
			t.printStackTrace();
			return t;
		}
	}



}
