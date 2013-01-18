package jp.syoboi.android.garaponmate.fragment;

import android.app.ListFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.Collections;
import java.util.Comparator;

import jp.syoboi.android.garaponmate.GaraponClient.Program;
import jp.syoboi.android.garaponmate.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.GaraponClientUtils;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.view.BroadcastingView;
import jp.syoboi.android.garaponmate.view.BroadcastingView.OnBroadcastingViewListener;

public class SummaryFragment extends ListFragment {

	BroadcastingView	mBcView;
	View				mProgress;
	RefreshTask			mRefreshTask;
	Handler				mHandler = new Handler();
	long				mPrevAutoRefreshTime;

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
				if (getActivity() instanceof MainActivity) {
					((MainActivity)getActivity()).setPlayerPage(p);
				}
			}

			@Override
			public void onClickChannel(Program p) {

			}
		});

		getListView().addHeaderView(mBcView);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
		setListAdapter(adapter);

		refresh();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	public void setSelected(String gtvid) {
		mBcView.setSelected(gtvid);
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
					ErrorDialogFragment f = ErrorDialogFragment.newInstance(getString(R.string.error),
							(Throwable)result);
					f.show(getFragmentManager(), "dialog");
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
		mRefreshTask.execute(getActivity());
	}

	private class RefreshTask extends AsyncTask<Object,Object,Object> {

		@Override
		protected Object doInBackground(Object... params) {
			try {
				Context context = (Context) params[0];

				SearchResult sr = GaraponClientUtils.searchNowBroadcasting();

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

