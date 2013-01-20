package jp.syoboi.android.garaponmate.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.adapter.ProgramAdapter;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.client.SearchParam;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.task.SearchTask;
import jp.syoboi.android.garaponmate.view.LoadingRowWrapper;

public class SearchResultFragment extends ListFragment {

	private static final String TAG = "SearchResultFragment";

	private static final int REQUEST_EDIT = 1;

	private static final int PAGE_COUNT = 20;
	private static final int PREFETCH_COUNT = PAGE_COUNT / 2;

	View				mHeader;
	TextView			mTitle;
	LoadingRowWrapper	mLoadingRow;
	SearchParam			mSearchParam;
	ProgramAdapter		mAdapter;
	int					mPage = 1;
	boolean				mHasError;
	SearchTask			mSearchTask;

	public static SearchResultFragment newInstance(SearchParam searchParam) {
		Bundle args = new Bundle();
		args.putSerializable("searchParam", searchParam);

		SearchResultFragment f = new SearchResultFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		mSearchParam = (SearchParam) args.getSerializable("searchParam");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_search_result, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		View v = getView();
		View loadingRow = View.inflate(getActivity(), R.layout.loading_row, null);
		mLoadingRow = new LoadingRowWrapper((ViewSwitcher)loadingRow);
		mLoadingRow.getMesageView().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loadNext();
			}
		});

		mHeader = View.inflate(getActivity(), R.layout.search_result_header, null);
		mTitle = (TextView) mHeader.findViewById(android.R.id.text1);

		getListView().addHeaderView(mHeader);
		getListView().addFooterView(mLoadingRow.getView());

		mAdapter = new ProgramAdapter(getActivity());
		setListAdapter(mAdapter);

		getListView().setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem + visibleItemCount >= totalItemCount - PREFETCH_COUNT) {
					if (!mHasError) {
						loadNext();
					}
				}
			}
		});


		mSearchParam.page = mPage;
		mSearchParam.count = PAGE_COUNT;
	}

	@Override
	public void onDestroy() {
		cancelSearchTask();
		super.onDestroy();
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Object item = l.getItemAtPosition(position);
		if (item instanceof Program) {
			Program p = (Program)item;
			if (getActivity() instanceof MainActivity) {
				((MainActivity)getActivity()).playVideo(p);
			}
		}
		if (v == mHeader) {
			SearchParamEditFragment f = SearchParamEditFragment.newInstance(mSearchParam);
			f.setTargetFragment(this, REQUEST_EDIT);
			f.show(getFragmentManager(), "searchParamDialog");
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_EDIT:
			if (resultCode == Activity.RESULT_OK) {
				mSearchParam = (SearchParam) data.getSerializableExtra(SearchParamEditFragment.EXTRA_SEARCH_PARAM);
				mPage = 1;
				mAdapter.clear();
				cancelSearchTask();
				loadNext();
			}
			break;
		}
	}

	void cancelSearchTask() {
		if (mSearchTask != null) {
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
	}

	public void setVideo(String id) {
		mAdapter.setSelection(id);
	}

	void loadNext() {
		if (mPage == -1 || mSearchTask != null) {
			return;
		}

		mSearchParam.page = mPage;
		mTitle.setText(GaraponClientUtils.formatSearchParam(getActivity(), mSearchParam));
		mLoadingRow.setLoading();
		mSearchTask = new SearchTask(mSearchParam) {
			@Override
			protected void onPostExecute(Object result) {
				super.onPostExecute(result);
				finishTask();

				if (result instanceof Throwable) {
					mHasError = true;
					mLoadingRow.setMessage((Throwable)result);
				}
				else if (result instanceof SearchResult) {
					mHasError = false;
					SearchResult sr = (SearchResult)result;
					if (mPage == 1) {
						mAdapter.setItems(sr.program);
					} else {
						mAdapter.addItems(sr.program);
					}
					if (sr.program.size() > 0) {
						mPage++;
					} else {
						mPage = -1;
						mLoadingRow.setMessage(getString(R.string.searchHitCount, mAdapter.getCount()));
						//getListView().removeFooterView(mLoadingRow.getView());
					}
				}
			}
			@Override
			protected void onCancelled() {
				super.onCancelled();
				finishTask();
			}
			void finishTask() {
				mSearchTask = null;
			}
		};
		mSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
