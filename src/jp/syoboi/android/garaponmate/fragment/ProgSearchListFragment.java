package jp.syoboi.android.garaponmate.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.ProgSearch;
import jp.syoboi.android.garaponmate.data.ProgSearchList;

public class ProgSearchListFragment extends ListFragment {
	private static final int REQUEST_EDIT = 1;

	ProgSearchList				mProgSearchList;
	ArrayAdapter<ProgSearch>	mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.fragment_prog_search_list, null);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ContextMenuInfo cmi = item.getMenuInfo();
		if (cmi instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)cmi;
			int position = acmi.position;
			if (0 <= position && position < mProgSearchList.size()) {
				mProgSearchList.remove(position);
				mAdapter.notifyDataSetChanged();
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		View view = getView();
		view.findViewById(R.id.add).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addNew();
			}
		});

//		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
//			@Override
//			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
//					int arg2, long arg3) {
//
//				arg0.showContextMenu();
//				return true;
//			}
//		});
		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {

				getActivity().getMenuInflater().inflate(R.menu.prog_search_menu, menu);

			}
		});
		mProgSearchList = ProgSearchList.getInstance(getActivity());

		mAdapter = new ArrayAdapter<ProgSearch>(getActivity(),
				android.R.layout.simple_list_item_1,
				mProgSearchList);
		setListAdapter(mAdapter);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_EDIT:
			if (resultCode == Activity.RESULT_OK) {
				int idx = data.getIntExtra(ProgSearchEditFragment.EXTRA_INDEX, -1);
				ProgSearch ps = (ProgSearch)data.getSerializableExtra(ProgSearchEditFragment.EXTRA_PROGSEARCH);

				if (idx == -1) {
					if (!ps.isEmpty()) {
						mProgSearchList.add(ps);
					}
				} else {
					if (ps.isEmpty()) {
						mProgSearchList.remove(idx);
					} else {
						mProgSearchList.set(idx, ps);
					}
				}
				mAdapter.notifyDataSetChanged();
				Prefs.setSearch(mProgSearchList);
			}
			break;
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		ProgSearch ps = mAdapter.getItem(position);
		ProgSearchEditFragment f = ProgSearchEditFragment.newInstance(position, ps);
		f.setTargetFragment(this, REQUEST_EDIT);
		f.show(getFragmentManager(), "edit");
	}

	void addNew() {
		ProgSearchEditFragment f = ProgSearchEditFragment.newInstance(-1, new ProgSearch());
		f.setTargetFragment(this, REQUEST_EDIT);
		f.show(getFragmentManager(), "edit");
	}
}
