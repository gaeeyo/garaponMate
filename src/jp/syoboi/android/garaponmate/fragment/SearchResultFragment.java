package jp.syoboi.android.garaponmate.fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.adapter.ProgramAdapter;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.fragment.base.MainBaseFragment;
import jp.syoboi.android.garaponmate.task.SearchTask;
import jp.syoboi.android.garaponmate.view.LoadingRowWrapper;
import jp.syoboi.android.garaponmate.view.PlayerView;

public class SearchResultFragment extends MainBaseFragment {

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
		args.putSerializable(App.EXTRA_SEARCH_PARAM, searchParam);

		SearchResultFragment f = new SearchResultFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		mSearchParam = (SearchParam) args.getSerializable(App.EXTRA_SEARCH_PARAM);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_search_result, null);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		setListAdapter(null);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

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
		getListView().addFooterView(View.inflate(getActivity(), R.layout.dummy_row, null),
				null, false);

		if (mAdapter == null) {
			mAdapter = new ProgramAdapter(getActivity());
			mAdapter.setHighlightMatcher(mSearchParam.createMatcher());
			setListAdapter(mAdapter);
		} else {
			setListAdapter(mAdapter);
			updateLoadingRow();
		}

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

		registerForContextMenu(getListView());

		updateTitle();
		mSearchParam.page = mPage;
		mSearchParam.count = PAGE_COUNT;
	}

	@Override
	public void onReceiveLocalBroadcast(Context context, Intent intent) {
		super.onReceiveLocalBroadcast(context, intent);
		updatePlaying();

		String action = intent.getAction();
		if (App.ACTION_HISTORY_UPDATED.equals(action)) {
			if (mAdapter != null) {
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updatePlaying();
	}

	void updatePlaying() {
		if (mAdapter != null) {
			mAdapter.setSelection(PlayerView.getLatestPlayingId());
		}
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
			playVideo((Program)item);
		}
		if (v == mHeader) {
			SearchParamEditFragment f = SearchParamEditFragment.newInstance(mSearchParam);
			f.setTargetFragment(this, REQUEST_EDIT);
			f.show(getFragmentManager(), "searchParamDialog");
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		if (acmi.targetView.getParent() == getListView()) {
			Object obj = getListView().getItemAtPosition(acmi.position);
			if (obj instanceof Program) {
				inflateProgramMenu(getActivity(), menu, v, menuInfo, (Program)obj);
			}
		}
	}

//	@Override
//	public boolean onContextItemSelected(MenuItem item) {
//		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)item.getMenuInfo();
//		if (acmi.targetView.getParent() == getListView()) {
//			Object obj = getListView().getItemAtPosition(acmi.position);
//			if (obj instanceof Program) {
//				execCommand(item.getItemId(), (Program)obj);
//			}
//			return true;
//		}
//		return super.onContextItemSelected(item);
//	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_EDIT:
			if (resultCode == Activity.RESULT_OK) {
				mSearchParam = (SearchParam) data.getSerializableExtra(SearchParamEditFragment.EXTRA_SEARCH_PARAM);
				mPage = 1;
				mAdapter.clear();
				updateTitle();
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

	void updateTitle() {
		mTitle.setText(GaraponClientUtils.formatSearchParam(getActivity(), mSearchParam));
	}

	void updateLoadingRow() {
		if (mPage == -1) {
			mLoadingRow.setMessage(getString(R.string.searchHitCount, mAdapter.getCount()));
		}
	}

	void loadNext() {
		if (mPage == -1 || mSearchTask != null) {
			return;
		}

		mSearchParam.page = mPage;
		mLoadingRow.setLoading();

		mSearchTask = new SearchTask(getActivity(), mSearchParam, true) {
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
					}
				}
				updateLoadingRow();
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

	@Override
	public Animator onCreateAnimator(int transit, boolean enter,
			int nextAnim) {

		AnimatorSet set = new AnimatorSet();
		float fromScale = 1.5f;

		if ((transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN && !enter)
			|| (transit == FragmentTransaction.TRANSIT_FRAGMENT_CLOSE && enter)) {
			fromScale = 0.5f;
		}

		if (enter) {
			set.playTogether(makeAnimation(fromScale, 1, 0, 1));
		} else {
			set.playTogether(makeAnimation(1, fromScale, 1, 0));
		}
		return set;
	}

	public Animator[] makeAnimation(float fromScale, float toScale,
			float fromAlpha, float toAlpha) {
		return new Animator[] {
				ObjectAnimator.ofFloat(getView(), "scaleX", fromScale, toScale),
				ObjectAnimator.ofFloat(getView(), "scaleY", fromScale, toScale),
				ObjectAnimator.ofFloat(getView(), "alpha", fromAlpha, toAlpha) };
	}
}
