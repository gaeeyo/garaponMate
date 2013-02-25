package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.ListView;

import jp.syoboi.android.garaponmate.data.ImageLoader;

public class MyListView extends ListView {

	OnScrollListener	mOnScrollListener;

	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		super.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
				case SCROLL_STATE_IDLE:
				case SCROLL_STATE_TOUCH_SCROLL:
					ImageLoader.WAIT_CALLBACK = false;
					break;
				case SCROLL_STATE_FLING:
				default:
					ImageLoader.WAIT_CALLBACK = true;
					break;
				}
				if (mOnScrollListener != null) {
					mOnScrollListener.onScrollStateChanged(view, scrollState);
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (mOnScrollListener != null) {
					mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
				}

			}
		});
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		// super.setOnScrollListener(l);
		mOnScrollListener = l;
	}

}
