package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ListView;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.adapter.CaptionAdapter;
import jp.syoboi.android.garaponmate.data.Caption;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.utils.Utils;

public class PlayerCaptionView extends FrameLayout {

	private static final String TAG = "PlayerCaptionView";

	ListView	mList;
	CaptionAdapter	mAdapter;
	Program		mProgram;
	PlayerView	mPlayer;
	Handler		mHandler = new Handler();

	int			mCurIndex;
	int			mCurCount;
	int			mNextTime;
	int			mIncInterval;
	boolean		mVisible;
	boolean		mAllowAutoScroll = true;

	public PlayerCaptionView(Context context, AttributeSet attrs, PlayerView player) {
		super(context, attrs);
		inflate(context, R.layout.player_caption_view, this);
		mPlayer = player;
		mList = (ListView) findViewById(R.id.captionList);
		mList.setFocusableInTouchMode(true);
		mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> lv, View arg1, int position,
					long arg3) {
				Object obj = lv.getItemAtPosition(position);
				if (obj instanceof Caption) {
					Caption caption = (Caption) obj;
					mPlayer.seek(caption.time);
				}
			}
		});
		mList.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				mAllowAutoScroll = false;
				for (int j=mList.getCount()-1; j>=0; j--) {
					View child = mList.getChildAt(j);
					if (child instanceof Checkable) {
						if (((Checkable)child).isChecked()) {
							mAllowAutoScroll = true;
						}
					}
				}
			}
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

			}
		});
	}

	public void setProgram(Program p) {
		mProgram = p;

		if (mAdapter == null) {
			mAdapter = new CaptionAdapter(getContext());
			mList.setAdapter(mAdapter);
		}
		mAdapter.clear();
		if (p.caption != null) {
			for (Caption caption: p.caption) {
				mAdapter.add(caption);
			}
		}
		scrollNow();
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		mVisible = (visibility == View.VISIBLE);
		if (mVisible) {
			scrollNow();
		} else {
			mAllowAutoScroll = true;
		}
	}

	boolean scrollNow() {
		if (App.DEBUG) {
			Log.d(TAG, "scrollNow");
		}

		mHandler.removeCallbacks(mScrollNext);
		mHandler.removeCallbacks(mScrollNow);

		int curTime = mPlayer.getPos();
//		if (curTime == 0) {
//			if (App.DEBUG) {
//				Log.d(TAG, "Seek not available");
//			}
//			return false;
//		}
		mCurIndex = 0;
		mNextTime = 0;

		for (int j=0; j<mAdapter.getCount(); j++) {
			Caption caption = mAdapter.getItem(j);
			if (caption.time > curTime) {
				mNextTime = caption.time;
				mCurIndex = j;
				if (mCurIndex > 0) {
					mCurIndex--;
				}
				break;
			}
		}
		if (mCurIndex >= mAdapter.getCount()
				|| mNextTime == 0) {
			if (App.DEBUG) {
				Log.d(TAG, "Seek caption failed");
			}
			return false;
		}

		scrollTo(mCurIndex);

		// 同じ時間の字幕がいくつあるか調べる
		mCurCount = 1;
		if (mCurIndex > 0) {
			int time = mAdapter.getItem(mCurIndex).time;
			for (int j=mCurIndex-1; j>=0; j--) {
				if (mAdapter.getItem(j).time/1000 == time / 1000) {
					mCurIndex = j;
					mCurCount++;
				} else {
					break;
				}
			}
		}
		if (mCurCount > 1) {
			mIncInterval = (mNextTime - curTime) / mCurCount;
			mHandler.postDelayed(mScrollNext, mIncInterval);
		} else {
			mHandler.postDelayed(mScrollNow, mNextTime - curTime);
		}
		if (App.DEBUG) {
			Log.d(TAG,
					String.format("mCurIndex:%d mCurCount:%d mIncInterval:%d mCurTime:%s mNextTime:%s",
							mCurIndex, mCurCount, mIncInterval,
							Utils.formatDuration(curTime),
							Utils.formatDuration(mNextTime)));
		}

		return true;
	}

	void scrollTo(int position) {
		mList.setItemChecked(mCurIndex, true);
		if (mAllowAutoScroll) {
			mList.smoothScrollToPosition(mCurIndex);
		}
	}

	Runnable mScrollNext = new Runnable() {
		@Override
		public void run() {
			if (mVisible) {
				mCurIndex++;
				if (mCurIndex < mAdapter.getCount()) {
					scrollTo(mCurIndex);
					mCurCount--;
					if (mCurCount > 0) {
						mHandler.postDelayed(this, mIncInterval);
					} else {
						mHandler.postDelayed(mScrollNow, mIncInterval);
					}
				}
			}
		}
	};
	Runnable mScrollNow = new Runnable() {
		@Override
		public void run() {
			if (mVisible) {
				scrollNow();
			}
		};
	};

}
