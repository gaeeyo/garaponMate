package jp.syoboi.android.garaponmate.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;

import java.util.Collections;
import java.util.Comparator;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.fragment.base.MainBaseFragment;
import jp.syoboi.android.garaponmate.view.BroadcastingView;
import jp.syoboi.android.garaponmate.view.BroadcastingView.OnBroadcastingViewListener;
import jp.syoboi.android.garaponmate.view.PlayerView;

public class NowBroadcastingFragment extends MainBaseFragment {


	BroadcastingView	mBcView;
	View				mProgress;
	RefreshTask			mRefreshTask;
	long				mPrevAutoRefreshTime;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return View.inflate(getActivity(), R.layout.fragment_now_broadcasting, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mBcView = new BroadcastingView(getActivity(), null);
		View v = getView();
		mProgress = v.findViewById(android.R.id.progress);
		mProgress.setVisibility(View.GONE);

		mBcView.setOnListener(new OnBroadcastingViewListener() {
			@Override
			public void onExpire() {
				long now = System.currentTimeMillis();
				if (now >= mPrevAutoRefreshTime + 59 * DateUtils.SECOND_IN_MILLIS) {
					reload();
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
					param.video = SearchParam.VIDEO_ALL;
					((MainActivity)getActivity()).search(param);
				}
			}
		});

		getListView().addOnLayoutChangeListener(new OnLayoutChangeListener() {

			@Override
			public void onLayoutChange(View v, int left, int top, int right,
					int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
				int newHeight = bottom - top;
				int oldHeight = oldBottom - oldTop;
				if (oldHeight != newHeight) {
					mBcView.adjustHeight(newHeight);
				}
			}
		});
		getListView().addHeaderView(mBcView);
//		getListView().addFooterView(View.inflate(getActivity(), R.layout.dummy_row, null),
//				null, false);

		// 検索リストのヘッダー

		registerForContextMenu(getListView());

		ArrayAdapter<String> dummyAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_list_item_1);
		setListAdapter(dummyAdapter);

		reload();
	}

	@Override
	public void onDestroyView() {
		setListAdapter(null);
		mBcView = null;
		super.onDestroyView();
	}

	@Override
	public void onResume() {
		super.onResume();
		updatePlaying();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_now_broadcasting, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.reload:
			reload();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		if (menuInfo instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
			if (acmi.targetView == mBcView) {
				Program p = (Program)mBcView.getTag();
				inflateProgramMenu(getActivity(), menu, v, menuInfo, p);
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
//			if (acmi.targetView == mBcView) {
//				Program p = (Program)mBcView.getTag();
//				execCommand(item.getItemId(), p);
//			}
//			else
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
	public void onReceiveLocalBroadcast(Context context, Intent intent) {
		super.onReceiveLocalBroadcast(context, intent);

		if (isResumed()) {
			String action = intent.getAction();
			if (App.ACTION_REFRESH.equals(action)) {
				reload();
			} else {
				updatePlaying();
			}
		}
	}

	void updatePlaying() {
		if (mBcView != null) {
			mBcView.setSelected(PlayerView.getLatestPlayingId());
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
		getListView().requestLayout();
	}

	@Override
	public void reload() {
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
					showError((Throwable)result);
				}
				else if (result instanceof SearchResult) {
					SearchResult sr = (SearchResult)result;

					if (mBcView != null) {
						mBcView.setItems(sr.program);
					}
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
		mRefreshTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

