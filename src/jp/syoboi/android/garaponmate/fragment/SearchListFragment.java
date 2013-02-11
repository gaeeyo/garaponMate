package jp.syoboi.android.garaponmate.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.adapter.SearchParamAdapter;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.fragment.base.MainBaseFragment;

public class SearchListFragment extends MainBaseFragment {

	private static final int REQUEST_NEW = 1;

	SearchParamAdapter	mAdapter;
	View				mSearchListHeader;


	DataSetObserver		mDataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			if (mAdapter != null) {
				mAdapter.notifyDataSetChanged();
			}
		};
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.getSearchParamList().registerDataSetObserver(mDataSetObserver);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.fragment_summary, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// 検索リストのヘッダー
		mSearchListHeader = View.inflate(getActivity(), R.layout.search_new_row, null);
		getListView().addHeaderView(mSearchListHeader);
		getListView().addFooterView(View.inflate(getActivity(), R.layout.dummy_row, null),
				null, false);

		registerForContextMenu(getListView());

		mAdapter = new SearchParamAdapter(getActivity());
		setListAdapter(mAdapter);
	}

	@Override
	public void onDestroyView() {
		setListAdapter(null);
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		App.getSearchParamList().unregisterDataSetObserver(mDataSetObserver);
		super.onDestroy();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		if (menuInfo instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
			if (v == getListView()) {
				Object obj = getListView().getItemAtPosition(acmi.position);
				if (obj != null) {
					getActivity().getMenuInflater().inflate(R.menu.search_list_item_menu, menu);
				}
			}
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ContextMenuInfo cmi = item.getMenuInfo();
		if (cmi instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)cmi;
			if (acmi.targetView.getParent() == getListView()) {
				Object obj = getListView().getItemAtPosition(acmi.position);
				if (obj instanceof SearchParam) {
					SearchParam sp = (SearchParam)obj;
					App.getSearchParamList().removeById(sp.id);
				}
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_NEW:
			if (resultCode == Activity.RESULT_OK) {
				SearchParam sp = (SearchParam)data.getSerializableExtra(SearchParamEditFragment.EXTRA_SEARCH_PARAM);
				if (sp.id == 0) {
					search(sp);
				}
			}
			break;
		}
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if (v == mSearchListHeader) {
			SearchParamEditFragment f = SearchParamEditFragment.newInstance(new SearchParam());
			f.setTargetFragment(this, REQUEST_NEW);
			f.show(getFragmentManager(), "editDialog");
		}
		else {
			Object obj = l.getItemAtPosition(position);
			if (obj instanceof SearchParam) {
				search((SearchParam)obj);
			}
		}
	}

	void search(SearchParam sp) {
		if (getActivity() != null) {
			((MainActivity)getActivity()).search(sp);
		}
	}
}
