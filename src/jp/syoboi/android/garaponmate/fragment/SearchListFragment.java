package jp.syoboi.android.garaponmate.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import java.util.concurrent.CountDownLatch;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.adapter.SearchParamAdapter;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.fragment.base.MainBaseFragment;
import jp.syoboi.android.garaponmate.task.SearchTask;

public class SearchListFragment extends MainBaseFragment {

	private static final int REQUEST_NEW = 1;

	SearchParamAdapter	mAdapter;
	View				mSearchListHeader;
	RefreshTask			mRefreshTask;

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
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.fragment_summary, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_search_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.newSearch:
			newSearch();
			return true;
		case R.id.reload:
			reload();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// 検索リストのヘッダー
		mSearchListHeader = View.inflate(getActivity(), R.layout.search_new_row, null);
//		getListView().addHeaderView(mSearchListHeader);
//		getListView().addFooterView(View.inflate(getActivity(), R.layout.dummy_row, null),
//				null, false);

		registerForContextMenu(getListView());

		mAdapter = new SearchParamAdapter(getActivity());
		setListAdapter(mAdapter);

		if (savedInstanceState == null) {
			 refreshAll();
		}
	}

	@Override
	public void onDestroyView() {
		setListAdapter(null);
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if (mRefreshTask != null) {
			mRefreshTask.cancel(true);
			mRefreshTask = null;
		}
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
				} else {
					App.from(getActivity()).deleteSearchResultCacheFile(sp.id);
					refreshAll();
				}
			}
			break;
		}
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if (v == mSearchListHeader) {
			newSearch();
		}
		else {
			Object obj = l.getItemAtPosition(position);
			if (obj instanceof SearchParam) {
				search((SearchParam)obj);
			}
		}
	}

	@Override
	public void onReceiveLocalBroadcast(Context context, Intent intent) {
		super.onReceiveLocalBroadcast(context, intent);

		if (isResumed()) {
			String action = intent.getAction();
			if (App.ACTION_REFRESH.equals(action)) {
				refreshAll();
			}
			else if (App.ACTION_HISTORY_UPDATED.equals(action)) {
				if (mAdapter != null) {
					mAdapter.notifyDataSetChanged();
				}
			}
			else {
				if (mAdapter != null) {
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	void newSearch() {
		SearchParamEditFragment f = SearchParamEditFragment.newInstance(new SearchParam());
		f.setTargetFragment(this, REQUEST_NEW);
		f.show(getFragmentManager(), "editDialog");
	}

	void search(SearchParam sp) {
		if (getActivity() != null) {
			((MainActivity)getActivity()).search(sp);
		}
	}

	@Override
	public void reload() {
		super.reload();
		refreshAll();
	}

	void refreshAll() {
		if (mRefreshTask != null) {
			mRefreshTask.cancel(true);
		}

		mRefreshTask = new RefreshTask(getActivity()) {
			@Override
			protected void onProgressUpdate(Object... values) {
				super.onProgressUpdate(values);
				if (mAdapter != null && values != null) {
					if (values.length >= 2 && RefreshTask.P_START == values[0]) {
						SearchParam p = (SearchParam) values[1];
						mAdapter.setLoading(p.id, true);
					}
					else if (values.length >= 3 && RefreshTask.P_END == values[0]) {
						SearchParam p = (SearchParam) values[1];
						SearchResult sr = (SearchResult) values[2];
						mAdapter.setLoading(p.id, false);
						mAdapter.setSearchResult(p.id, sr);
					}
				}
			}
			@Override
			protected void onCancelled(Object result) {
				super.onCancelled(result);
				mRefreshTask = null;
			}
			@Override
			protected void onPostExecute(Object result) {
				super.onPostExecute(result);
				mRefreshTask = null;
			}
		};
		mRefreshTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}


	private static class RefreshTask extends AsyncTask<Object, Object, Object> {

		public static String P_START = "start";
		public static String P_END= "end";

		private Context mContext;

		public RefreshTask(Context context) {
			mContext = context.getApplicationContext();
		}

		@Override
		protected Object doInBackground(Object... params) {

			for (final SearchParam p: App.getSearchParamList().items()) {
				if (isCancelled()) {
					return null;
				}
				publishProgress(P_START, p);
				try {
					final CountDownLatch cdl = new CountDownLatch(1);

					SearchParam sp = p.clone();
					sp.edate = System.currentTimeMillis();
					sp.searchTime = SearchParam.STIME_START;
					sp.page = 1;

					SearchTask st = new SearchTask(mContext, sp, true) {
						@Override
						protected void onPostExecute(Object result) {
							cdl.countDown();
							if (result instanceof SearchResult) {
								RefreshTask.this.publishProgress(P_END, p, result);
							} else {
								RefreshTask.this.publishProgress(P_END, p, null);
							}
						};
						@Override
						protected void onCancelled(Object result) {
							super.onCancelled(result);
							cdl.countDown();
							publishProgress(P_END, p, null);
						}
					};
					st.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					cdl.await();

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return null;
		}
	}
}
