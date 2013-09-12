package jp.syoboi.android.garaponmate.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentStatePagerAdapter;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.fragment.EmptyFragment;
import jp.syoboi.android.garaponmate.fragment.EpgFragment;
import jp.syoboi.android.garaponmate.fragment.GaraponWebFragment;
import jp.syoboi.android.garaponmate.fragment.SearchListFragment;



public class MainPagerAdapter extends FragmentStatePagerAdapter {

	public static final int PAGE_NOWBROADCASTING = 0;
	public static final int PAGE_SEARCH = 1;
	public static final int PAGE_WEB = 2;

	public static final int PAGE_COUNT = 3;
	public static final int PAGE_EMPTY = PAGE_COUNT;

	private Context mContext;
	private boolean	mEnableDummyPage;

	public MainPagerAdapter(FragmentManager fm, Context context) {
		super(fm);
		mContext = context.getApplicationContext();
	}

	public void setEnableDummyPage(boolean enable) {
		if (mEnableDummyPage != enable) {
			mEnableDummyPage = enable;
			notifyDataSetChanged();
		}
	}

	@Override
	public Fragment getItem(int position) {
		Fragment f = null;

		switch (position) {
		case PAGE_NOWBROADCASTING:
//			f = new NowBroadcastingFragment();
			f = new EpgFragment();
			break;
		case PAGE_SEARCH:
			f = new SearchListFragment();
			break;
		case PAGE_WEB:
			f = new GaraponWebFragment();
			break;
		case PAGE_EMPTY:
			f = new EmptyFragment();
			break;
		}
		return f;
	}

	@Override
	public CharSequence getPageTitle(int position) {

		switch (position) {
		case PAGE_NOWBROADCASTING:
			return mContext.getString(R.string.nowBroadcasting);
		case PAGE_SEARCH:
			return mContext.getString(R.string.search);
		case PAGE_WEB:
			return mContext.getString(R.string.browser);
		}

		return super.getPageTitle(position);
	}

	@Override
	public int getCount() {
		if (mEnableDummyPage) {
			return PAGE_COUNT + 1;
		}
		return PAGE_COUNT;
	}
}
