package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.List;

import jp.syoboi.android.garaponmate.GaraponClient.Program;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.Utils;

public class BroadcastingView extends FrameLayout {

	private static final String TAG = "BroadcastingView";

	TableLayout		mTable;
	List<Program>	mItems;
	Paint			mPaint = new Paint();
	View			mBottomTitle;
	int				mTimeBarColor;
	int				mBroadcastTimeBarColor;
	int				mTimeBarHeight;
	int				mNowStartedBackgroundColor;
	int				mSelectedBackgroundColor;
	String			mSelectedGtvid;
	OnBroadcastingViewListener	mListener;

	public BroadcastingView(Context context, AttributeSet attrs) {
		super(context, attrs);

		View v = inflate(context, R.layout.broadcasting, null);
		addView(v);

		Resources res = context.getResources();
		mTimeBarColor = res.getColor(R.color.timeBarColor);
		mBroadcastTimeBarColor = res.getColor(R.color.broadcastTimeBarColor);
		mTimeBarHeight = res.getDimensionPixelOffset(R.dimen.timeBarHeight);
		mNowStartedBackgroundColor = res.getColor(R.color.nowStartedBackgroundColor);
		mSelectedBackgroundColor = res.getColor(R.color.selectedColor);

		mTable = (TableLayout) v.findViewById(R.id.broadcastingTable);
		mPaint.setAntiAlias(true);
		setWillNotDraw(false);
	}

	public void setItems(List<Program> items) {
		mItems = items;
		updateViews();
	}

	public void setOnListener(OnBroadcastingViewListener listener) {
		mListener = listener;
	}

	public void setSelected(String gtvid) {
		mSelectedGtvid = gtvid;
		invalidate();
	}

	void updateViews() {
		mBottomTitle = null;

		while (mTable.getChildCount() > mItems.size()) {
			mTable.removeViewAt(0);
		}
		while (mTable.getChildCount() < mItems.size()) {
			final View row = View.inflate(getContext(), R.layout.broadcasting_row, null);
			row.findViewById(R.id.ch).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					int idx = mTable.indexOfChild(row);
					mListener.onClickChannel(mItems.get(idx));
				}
			});
			row.findViewById(R.id.title).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					int idx = mTable.indexOfChild(row);
					mListener.onClickProgram(mItems.get(idx));
				}
			});
			mTable.addView(row);
		}

		Time t = new Time();
		SpannableStringBuilder ssb = new SpannableStringBuilder();

		for (int j=mItems.size()-1; j>=0; j--) {
			Program p = mItems.get(j);
			View row = mTable.getChildAt(j);

			TextView chName = (TextView) row.findViewById(R.id.chName);
			TextView time = (TextView) row.findViewById(R.id.time);
			TextView title = (TextView) row.findViewById(R.id.title);


			long min = p.duration / 1000 / 60;

			t.set(p.startdate);

			ssb
			.delete(0, ssb.length())
			.append(t.format("%H:%M"))
			.append(String.format(" (%d:%02d)", min / 60, min % 60));

			chName.setText(Utils.convertCoolTitle(p.ch.bc));
			time.setText(ssb.toString());
			title.setText(Utils.convertCoolTitle(p.title));
			mBottomTitle = title;
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		drawTimeBar(canvas);
		super.dispatchDraw(canvas);
	}

	void drawTimeBar(Canvas canvas) {

		if (mTable == null || mItems == null || mBottomTitle == null) {
			return;
		}

		int childCount = Math.min(mItems.size(), mTable.getChildCount());
		if (childCount == 0) {
			return;
		}
		int displayWidth = mBottomTitle.getMeasuredWidth();
		int displayLeft = mBottomTitle.getLeft();

		long minStart = Long.MAX_VALUE;
		long maxEnd = Long.MIN_VALUE;
		for (int j=0; j<childCount; j++) {
			Program p = mItems.get(j);
			long end = p.startdate + p.duration;

			if (p.startdate < minStart) {
				minStart = p.startdate;
			}
			if (end > maxEnd) {
				maxEnd = end;
			}
		}

		long range = (maxEnd - minStart);
		minStart -= range / 20;
		maxEnd += range / 20;
		range = maxEnd - minStart;

		final Paint paint = mPaint;
		long now = System.currentTimeMillis();
		int topOffset = mTable.getTop();

		for (int j=0; j<childCount; j++) {
			Program p = mItems.get(j);

			long startOffset = p.startdate - minStart;
			int left = displayLeft + Math.round((float)displayWidth * startOffset / range);
			int right = left + Math.round((float)displayWidth * p.duration / range);
			int nowRight = left + Math.round((float)displayWidth * (now - p.startdate) / range);

			View child = mTable.getChildAt(j);
			int top = child.getTop() + topOffset;

			if (TextUtils.equals(mSelectedGtvid, p.gtvid)) {
				paint.setColor(mSelectedBackgroundColor);
				canvas.drawRect(mTable.getLeft(), top, mTable.getRight(),
						top + child.getHeight(), paint);
			}

			if (p.startdate <= now && now < p.startdate + 1 * DateUtils.MINUTE_IN_MILLIS) {
				paint.setColor(mNowStartedBackgroundColor);
				canvas.drawRect(displayLeft, top, left + displayWidth,
						top + child.getHeight(), paint);
			}

			paint.setColor(mTimeBarColor);
			canvas.drawRect(left, top, right, top + mTimeBarHeight, paint);
			paint.setColor(mBroadcastTimeBarColor);
			canvas.drawRect(left, top, nowRight, top + mTimeBarHeight, paint);

			if (p.startdate + p.duration < now) {

			}
		}

		long sec = 60 * 1000 - (now % (60 * 1000));
		Log.v(TAG, "post invalidate " + sec + "msec");
		postInvalidateDelayed(sec);
		if (checkExpire()) {
			if (mListener != null) {
				mListener.onExpire();
			}
		}
	}

	boolean checkExpire() {
		if (mItems != null) {
			long now = System.currentTimeMillis();
			for (Program p: mItems) {
				if (p.startdate + p.duration < now) {
					return true;
				}
			}
		}
		return false;
	}

	public static interface OnBroadcastingViewListener {
		public void onExpire();
		public void onClickProgram(Program p);
		public void onClickChannel(Program p);
	}
}
