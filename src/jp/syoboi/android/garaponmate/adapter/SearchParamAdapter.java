package jp.syoboi.android.garaponmate.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.HashMap;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.view.ProgramGridView;

public class SearchParamAdapter extends ArrayAdapter<SearchParam> {

	private boolean mOnNotifyDataSetChanged;
	private HashMap<Long, SearchResultItem>	mResultMap = new HashMap<Long, SearchResultItem>();

	public SearchParamAdapter(Context context) {
		super(context, 0);
		setItems();
	}

	@Override
	public void notifyDataSetChanged() {
		if (!mOnNotifyDataSetChanged) {
			mOnNotifyDataSetChanged = true;
			setItems();
			super.notifyDataSetChanged();
			mOnNotifyDataSetChanged = false;
		}
	}

	public void setSearchResult(long id, SearchResult searchResult) {
		SearchResultItem sri = mResultMap.get(id);
		if (sri == null) {
			sri = new SearchResultItem();
			sri.searchResult = searchResult;
			mResultMap.put(id, sri);
		} else {
			sri.searchResult = searchResult;
		}
		notifyDataSetChanged();
	}

	public void setLoading(long id, boolean isLoading) {
		SearchResultItem sri = mResultMap.get(id);
		if (sri == null) {
			sri = new SearchResultItem();
			sri.isLoading = isLoading;
			mResultMap.put(id, sri);
		} else {
			sri.isLoading = isLoading;
		}
		notifyDataSetChanged();
	}

	void setItems() {
		clear();
		for (SearchParam p: App.getSearchParamList().items()) {
			add(p);
		}
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		ViewHolder vh;
		if (v == null) {
			v = View.inflate(getContext(), R.layout.search_param_row, null);
			vh = new ViewHolder(v);
			v.setTag(vh);
		} else {
			vh = (ViewHolder) v.getTag();
		}

		SearchParam p = getItem(position);
		vh.bind(p, mResultMap.get(p.id));

		return v;
	}

	private static class SearchResultItem {
		public boolean isLoading;
		public SearchResult searchResult;
	}


	private static class ViewHolder {

		TextView		mText1;
		View			mProgress;
		ProgramGridView	mProgramGrid;

		ViewHolder(View root) {
			mText1 = (TextView) root.findViewById(android.R.id.text1);
			mProgress = root.findViewById(android.R.id.progress);
			mProgramGrid = (ProgramGridView) root.findViewById(R.id.programGrid);
		}

		void bind(SearchParam p, SearchResultItem i) {
			String text = GaraponClientUtils.formatSearchParam(mText1.getContext(), p);
			mText1.setText(text);

			mProgress.setVisibility((i != null && i.isLoading)
					? View.VISIBLE : View.GONE);

			// 検索結果の番組を表示
			if (i != null && i.searchResult != null && i.searchResult.program != null) {
				mProgramGrid.setPrograms(i.searchResult.program, p);
			} else {
				mProgramGrid.setPrograms(null, null);
			}
		}
	}
}
