package jp.syoboi.android.garaponmate.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import java.util.Collections;
import java.util.Comparator;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.adapter.SearchParamAdapter;
import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.client.SearchParam;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.fragment.base.MainBaseFragment;
import jp.syoboi.android.garaponmate.view.BroadcastingView;
import jp.syoboi.android.garaponmate.view.BroadcastingView.OnBroadcastingViewListener;

public class SummaryFragment extends MainBaseFragment {

	private static final int REQUEST_NEW = 1;

	BroadcastingView	mBcView;
	View				mProgress;
	RefreshTask			mRefreshTask;
	Handler				mHandler = new Handler();
	long				mPrevAutoRefreshTime;
	SearchParamAdapter	mAdapter;
	View				mSearchListHeader;
	View				mSearchListEmpty;


	DataSetObserver		mDataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			if (mAdapter != null) {
				mAdapter.notifyDataSetChanged();
				updateSearchListEmpty();
			}
		};
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.fragment_summary, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		View v = getView();
		mProgress = v.findViewById(android.R.id.progress);
		mProgress.setVisibility(View.GONE);

		mBcView = new BroadcastingView(getActivity(), null);
		mBcView.setOnListener(new OnBroadcastingViewListener() {
			@Override
			public void onExpire() {
				long now = System.currentTimeMillis();
				if (now >= mPrevAutoRefreshTime + 59 * DateUtils.SECOND_IN_MILLIS) {
					refresh();
				}
				mPrevAutoRefreshTime = now;
			}

			@Override
			public void onClickProgram(Program p) {
				playVideo(p);
			}

			@Override
			public boolean onLongClickProgram(Program p) {
				mBcView.setTag(p);
				mBcView.showContextMenu();
				return true;
			}

			@Override
			public void onClickChannel(Program p) {
				if (getActivity() instanceof MainActivity) {
					SearchParam param = new SearchParam();
					param.comment = getString(R.string.searchCh, p.ch.bc);
					param.ch = p.ch.ch;
					((MainActivity)getActivity()).search(param);
				}
			}
		});

		getListView().addHeaderView(mBcView);

		// 検索リストのヘッダー
		mSearchListHeader = View.inflate(getActivity(), R.layout.search_list_header, null);
		mSearchListEmpty = mSearchListHeader.findViewById(R.id.searchListEmpty);
		getListView().addHeaderView(mSearchListHeader);
		getListView().addFooterView(View.inflate(getActivity(), R.layout.dummy_row, null),
				null, false);

		registerForContextMenu(getListView());

		App.getSearchParamList().registerDataSetObserver(mDataSetObserver);

		mAdapter = new SearchParamAdapter(getActivity());
		setListAdapter(mAdapter);

		updateSearchListEmpty();
		refresh();
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
			if (acmi.targetView == mBcView) {
				Program p = (Program)mBcView.getTag();
				inflateProgramMenu(menu, v, menuInfo, p);

				MenuItem mi = menu.findItem(R.id.download);
				if (mi != null) {
					mi.setVisible(false);
				}
			}
			else if (v == getListView()) {
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
			if (acmi.targetView == mBcView) {
				Program p = (Program)mBcView.getTag();
				execCommand(item.getItemId(), p);
			}
			else if (acmi.targetView.getParent() == getListView()) {
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

	public void setVideo(String gtvid) {
		mBcView.setSelected(gtvid);
	}

	void updateSearchListEmpty() {
		mSearchListEmpty.setVisibility(mAdapter.getCount() > 0 ? View.GONE : View.VISIBLE);
		getListView().requestLayout();
	}

	void refresh() {
		if (mRefreshTask != null) {
			return;
		}

		mProgress.setVisibility(View.VISIBLE);

		mRefreshTask = new RefreshTask() {
			@Override
			protected void onPostExecute(Object result) {
				finish();
				if (isCancelled()) {
					return;
				}

				if (result instanceof Throwable) {
					ErrorDialogFragment.show(getFragmentManager(),
							(Throwable)result);
				}
				else if (result instanceof SearchResult) {
					SearchResult sr = (SearchResult)result;

					mBcView.setItems(sr.program);
//					mWebView.loadDataWithBaseURL(Prefs.getBaseUrl() + SPECIAL_PAGE_PATH,
//							(String)result, "text/html", "utf-8",
//							Prefs.getBaseUrl() + SPECIAL_PAGE_PATH);
				}
			}

			@Override
			protected void onCancelled() {
				finish();
			}

			void finish() {
				mProgress.setVisibility(View.GONE);
				mRefreshTask = null;
			}

		};
		mRefreshTask.execute();
	}

	private class RefreshTask extends AsyncTask<Object,Object,Object> {

		@Override
		protected Object doInBackground(Object... params) {
			try {
				SearchResult sr = GaraponClientUtils.searchNowBroadcasting();

				if (sr.status == GaraponClient.STATUS_SUCCESS) {
					App.getChList().setCh(sr.ch.values());
				}

				Collections.sort(sr.program, new Comparator<Program>() {
					@Override
					public int compare(Program lhs, Program rhs) {
						return rhs.ch.ch - lhs.ch.ch;
					}
				});

				return sr;
			} catch (Throwable t) {
				t.printStackTrace();
				return t;
			}
		}
	}
}

