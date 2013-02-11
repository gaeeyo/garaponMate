package jp.syoboi.android.garaponmate.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.fragment.NowBroadcastingFragment;
import jp.syoboi.android.garaponmate.fragment.SearchListFragment;



public class MainPagerAdapter extends FragmentPagerAdapter {

	private static final int PAGE_NOWBROADCASTING = 0;
	private static final int PAGE_SEARCH = 1;
	private static final int PAGE_WEB = 2;

	private static final int PAGE_COUNT = 2;

	private Context mContext;

	public MainPagerAdapter(FragmentManager fm, Context context) {
		super(fm);
		mContext = context.getApplicationContext();
	}

	@Override
	public Fragment getItem(int position) {
		Fragment f = null;

		switch (position) {
		case PAGE_NOWBROADCASTING:
			f = new NowBroadcastingFragment();
			break;
		case PAGE_SEARCH:
			f = new SearchListFragment();
			break;
		case PAGE_WEB:
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
		}

		return super.getPageTitle(position);
	}

	@Override
	public int getCount() {
		return PAGE_COUNT;
	}
}
