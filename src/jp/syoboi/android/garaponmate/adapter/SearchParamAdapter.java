package jp.syoboi.android.garaponmate.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.client.SearchParam;

public class SearchParamAdapter extends ArrayAdapter<SearchParam> {

	private boolean mOnNotifyDataSetChanged;

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

	void setItems() {
		clear();
		for (SearchParam p: App.getSearchParamList().items()) {
			add(p);
		}
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		if (v == null) {
			v = View.inflate(getContext(), android.R.layout.simple_list_item_1, null);
		}
		TextView tv = (TextView) v;
		SearchParam p = getItem(position);
		tv.setText(GaraponClientUtils.formatSearchParam(getContext(), p));

		return v;
	}
}