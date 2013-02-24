package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.Caption;
import jp.syoboi.android.garaponmate.data.Program;

public class PlayerOverlay extends FrameLayout {

	private static final int PAGE_DETAIL = 0;
	private static final int PAGE_CONTROLLER = 1;
	private static final int PAGE_CAPTION = 2;

	ViewPager				mViewPager;
	PagerTabStrip			mPagerTabStrip;
	PlayerControllerView	mController;
	PlayerDetailView		mDetail;
	PlayerCaptionView		mCaptionView;
	PlayerOverlayPagerAdapter	mPagerAdapter;

	Program					mProgram;
	Caption[]				mSearchCaptions;

	public PlayerOverlay(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setPlayer(PlayerView player) {

		Context context = getContext();
		inflate(context, R.layout.player_overlay, this);

		mViewPager = (ViewPager) findViewById(R.id.playerOverlayPager);

		float density = getResources().getDisplayMetrics().density;
		PagerTabStrip tabStrip = (PagerTabStrip) mViewPager.getChildAt(0);
		TextView tv = (TextView) tabStrip.getChildAt(1);
		tv.setShadowLayer(1, 0, (int)(1 * density), 0xff000000);

		mPagerAdapter = new PlayerOverlayPagerAdapter(
				context, player);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setCurrentItem(1);
	}

	public void onPause() {
		if (mController != null) {
			mController.onPause();
		}
	}

	public void onResume() {
		if (mController != null) {
			mController.onResume();
		}
	}

	public void onPlayerStateChanged() {
		if (mController != null) {
			mController.onPlayStateChanged();
		}
	}

	public void setProgram(Program p) {
		mProgram = p;
		mSearchCaptions = p.caption;
		if (mController != null) {
			mController.setProgram(p);
			mController.setCaptions(mSearchCaptions);
		}
		if (mDetail != null) {
			mDetail.setProgram(p);
		}
		if (mCaptionView != null) {
			mCaptionView.setProgram(p);
		}
		mPagerAdapter.notifyDataSetChanged();
	}

	public void setProgramDetail(Program p) {
		mProgram = p;
		if (mController != null) {
			mController.setProgram(p);
		}
		if (mDetail != null) {
			mDetail.setProgram(p);
		}
		if (mCaptionView != null) {
			mCaptionView.setProgram(p);
		}
		mPagerAdapter.notifyDataSetChanged();
	}

	private class PlayerOverlayPagerAdapter extends PagerAdapter {

		private Context	mContext;
		private PlayerView	mPlayer;

		public PlayerOverlayPagerAdapter(Context context, PlayerView player) {
			super();
			mContext = context;
			mPlayer = player;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			switch (position) {
			case PAGE_DETAIL:
				mDetail = new PlayerDetailView(mContext, null);
				container.addView(mDetail);
				mDetail.setProgram(mProgram);
				return mDetail;
			case PAGE_CONTROLLER:
				mController = new PlayerControllerView(mContext, null, mPlayer);
				container.addView(mController);
				mController.setProgram(mProgram);
				mController.setCaptions(mSearchCaptions);
				return mController;
			case PAGE_CAPTION:
				mCaptionView = new PlayerCaptionView(mContext, null, mPlayer);
				mCaptionView.setProgram(mProgram);
				container.addView(mCaptionView);
				return mCaptionView;
			}
			return super.instantiateItem(container, position);
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View)object);
			if (mController == object) {
				mController = null;
			}
			if (mDetail == object) {
				mDetail = null;
			}
			if (mCaptionView == object) {
				mCaptionView = null;
			}
		}

		@Override
		public int getCount() {
			int count = 2;
			if (mProgram != null
					&& mProgram.caption != null
					&& mProgram.caption.length > 0) {
				count++;
			}
			return count;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case PAGE_DETAIL:
				return mContext.getString(R.string.programDetail);
			case PAGE_CONTROLLER:
				return mContext.getString(R.string.play);
			case PAGE_CAPTION:
				return mContext.getString(R.string.caption);
			}
			return super.getPageTitle(position);
		}

	}
}
