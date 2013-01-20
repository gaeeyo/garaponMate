package jp.syoboi.android.garaponmate.task;

import android.os.AsyncTask;

import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.client.SearchParam;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;

public class SearchTask extends AsyncTask<Object, Object, Object> {

	private SearchParam mSearchParam;

	public SearchTask(SearchParam param) {
		mSearchParam = param;
	}

	@Override
	protected Object doInBackground(Object... params) {
		try {
			SearchParam param = mSearchParam;

			SearchResult sr = GaraponClientUtils.search(param);

			return sr;
		} catch (Throwable t) {
			t.printStackTrace();
			return t;
		}
	}

}
