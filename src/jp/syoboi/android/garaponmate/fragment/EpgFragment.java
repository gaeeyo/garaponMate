package jp.syoboi.android.garaponmate.fragment;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashSet;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.view.EpgView;
import jp.syoboi.android.garaponmate.view.EpgView.OnScrollListener;

public class EpgFragment extends Fragment {

	static final String TAG = "EgpFragment";

	HashSet<Long>	mLoadedTimes = new HashSet<Long>();
	EpgView			mEpgView;
	View			mProgress;
	TextView		mDateView;
	GetEpgTask		mTask;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.epg_fragment, null);
		mEpgView = (EpgView) v.findViewById(R.id.epgView);
		mProgress = v.findViewById(R.id.progress);
		mDateView = (TextView) v.findViewById(R.id.date);

		mEpgView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrolled(EpgView v) {
				Log.d(TAG, "onScrolled");
				updateDateText();
				refresh(v.getFirstVisibleTime());
			}
		});

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		updateDateText();
		refresh(System.currentTimeMillis());
	}

	private void updateDateText() {
		long date = mEpgView.getFirstVisibleTime();
		String text = DateUtils.formatDateTime(getActivity(), date,
				DateUtils.FORMAT_ABBREV_ALL
				| DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY);

		mDateView.setText(text);
	}

	boolean refresh(long time) {
		Time t = new Time();

		t.set(time);

		if (t.hour < 5) {
			t.set(t.toMillis(true) - DateUtils.DAY_IN_MILLIS);
		}
		t.hour = 5;
		t.minute = 0;
		t.second = 0;

		final long start = t.toMillis(true);

		if (mLoadedTimes.contains(start)) {
			return false;
		}

		if (mTask != null) {
			return false;
		}

		mProgress.setVisibility(View.VISIBLE);
		mTask = new GetEpgTask(start) {
			@Override
			protected void onProgressUpdate(Object... values) {
				super.onProgressUpdate(values);
				if (values[0] instanceof SearchResult) {
					SearchResult sr = (SearchResult) values[0];
					mEpgView.addData(sr);
				}
			}

			@Override
			protected void onPostExecute(Object result) {
				mLoadedTimes.add(start);
				mTask = null;
				mProgress.setVisibility(View.VISIBLE);
				mProgress.setVisibility(View.GONE);
				loadNext();
			}
			@Override
			protected void onCancelled(Object result) {
				mProgress.setVisibility(View.GONE);
				mTask = null;
			}
		};
		mTask.execute();
		return true;
	}

	void loadNext() {
		long time = mEpgView.getFirstVisibleTime();
		if (refresh(time)) {
			return;
		}
		if (refresh(time - DateUtils.DAY_IN_MILLIS)) {
			refresh(time);
			return;
		}
		if (refresh(time + DateUtils.DAY_IN_MILLIS)) {
			refresh(time);
			return;
		}
	}

	static class GetEpgTask extends AsyncTask<Object, Object, Object> {

		final long mEpgStart;

		public GetEpgTask(long start) {
			mEpgStart = start;
		}

		@Override
		protected Object doInBackground(Object... params) {

			SearchParam sp = new SearchParam();

			try {
				sp.searchTime = SearchParam.STIME_START;
				sp.sdate = mEpgStart - (1 * DateUtils.HOUR_IN_MILLIS);
				sp.edate = mEpgStart + DateUtils.DAY_IN_MILLIS;
				sp.count = SearchParam.COUNT_MAX;

				int total = 0;
				int hit = -1;

				do {
					SearchResult sr = GaraponClientUtils.search(sp);
					publishProgress(sr);

					if (hit == -1) {
						hit = sr.hit;
					}

					total += sr.program.size();

					if (sr.program.size() == 0 || sr.hit <= 0) {
						break;
					}
					sp.page++;

				} while (total < hit);

				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return e;
			}
		}

	}
}
