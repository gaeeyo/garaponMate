package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
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

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.utils.Utils;

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
	int				mBorderColor;
	int				mTimeTextColor;
	float			mBorderWidth;
	float			mTimeBarTextSize;
	Time			mTmpTime = new Time();
	String			mSelectedGtvid;
	OnBroadcastingViewListener	mListener;

	public BroadcastingView(Context context, AttributeSet attrs) {
		super(context, attrs);

		View v = inflate(context, R.layout.broadcasting, null);
		addView(v);

		Resources res = context.getResources();
		mTimeBarColor = res.getColor(R.color.timeBarColor);
		mBroadcastTimeBarColor = res.getColor(R.color.broadcastTimeBarColor);
		mTimeBarHeight = res.getDimensionPixelSize(R.dimen.timeBarHeight);
		mNowStartedBackgroundColor = res.getColor(R.color.nowStartedBackgroundColor);
		mSelectedBackgroundColor = res.getColor(R.color.selectedColor);
		mBorderColor = res.getColor(R.color.timetableBorderColor);
		mBorderWidth = res.getDimensionPixelSize(R.dimen.timeBorderWidth);
		mTimeBarTextSize = res.getDimensionPixelSize(R.dimen.timeBarTextSize);
		mTimeTextColor = res.getColor(R.color.timeColor);

		mTable = (TableLayout) v.findViewById(R.id.broadcastingTable);
		mPaint.setAntiAlias(true);
		mPaint.setTypeface(Typeface.DEFAULT_BOLD);
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
			row.findViewById(R.id.title).setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					int idx = mTable.indexOfChild(row);
					return mListener.onLongClickProgram(mItems.get(idx));
				}
			});
			mTable.addView(row);
		}

		Time t = mTmpTime;
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
		Time t = mTmpTime;

		long range = (maxEnd - minStart);
		minStart = minStart / DateUtils.HOUR_IN_MILLIS * DateUtils.HOUR_IN_MILLIS;
		maxEnd = maxEnd / DateUtils.HOUR_IN_MILLIS * DateUtils.HOUR_IN_MILLIS + DateUtils.HOUR_IN_MILLIS;

		minStart -= range / 8;
		maxEnd += range / 8;
		range = maxEnd - minStart;

		// hour
		t.set(minStart + DateUtils.HOUR_IN_MILLIS - 1);
		t.minute = 0;
		t.second = 0;
		int hourLineStartHour = t.hour;
		long hourLineStart = t.toMillis(true);

		final Paint paint = mPaint;
		long now = Utils.currentTimeMillisJp();
		int tableTop = mTable.getTop();

		paint.setStyle(Style.FILL);

		int barBottomMargin = Math.round(1 * getResources().getDisplayMetrics().density);
		for (int j=0; j<childCount; j++) {
			Program p = mItems.get(j);

			long startOffset = p.startdate - minStart;
			int left = displayLeft + Math.round((float)displayWidth * startOffset / range);
			int right = displayLeft + Math.round((float)displayWidth * (p.startdate - minStart + p.duration) / range);
			int nowRight = displayLeft + Math.round((float)displayWidth * (now - minStart) / range);

			View child = mTable.getChildAt(j);
			int top = child.getTop() + tableTop;
			int bottom = child.getBottom() + tableTop - barBottomMargin;

			if (TextUtils.equals(mSelectedGtvid, p.gtvid)) {
				paint.setColor(mSelectedBackgroundColor);
				canvas.drawRect(mTable.getLeft(), top, mTable.getRight(),
						top + child.getHeight(), paint);
			}

			// 放送中の番組の背景色を変える
			if (p.startdate <= now && now < p.startdate + 1 * DateUtils.MINUTE_IN_MILLIS) {
				paint.setColor(mNowStartedBackgroundColor);
				canvas.drawRect(displayLeft, top, left + displayWidth,
						top + child.getHeight(), paint);
			}

			// 棒
			paint.setColor(mTimeBarColor);
			canvas.drawRect(left, bottom - mTimeBarHeight,
					right, bottom - 1, paint);
			paint.setColor(mBroadcastTimeBarColor);
			canvas.drawRect(left, bottom - mTimeBarHeight,
					nowRight, bottom - 1, paint);



			if (p.startdate + p.duration < now) {

			}
		}

		// 時間のラベルを描画
		{

			int bottom = mTable.getTop() - 1;

			int hour = hourLineStartHour;
			paint.setTextSize(mTimeBarTextSize);
			paint.setStrokeWidth(mBorderWidth);

			for (long x=hourLineStart; x<maxEnd; x += DateUtils.HOUR_IN_MILLIS, hour++) {
				int left = displayLeft + Math.round((float)displayWidth * (x - minStart) / range);

				paint.setStyle(Style.FILL);
				paint.setColor(mTimeTextColor);
				canvas.drawText(String.valueOf(hour % 24),
						left + mBorderWidth * 2, bottom - 1, paint);

				paint.setColor(mBorderColor);
				canvas.drawRect(left, bottom - mTimeBarHeight,
						left + mBorderWidth, bottom, paint);

//				paint.setColor(0x88ffffff);
				paint.setColor(0x44000000);
				for (int j=0; j<childCount; j++) {
					Program p = mItems.get(j);
					if (p.startdate <= x && x <= p.startdate + p.duration) {
						int childBottom = mTable.getChildAt(j).getBottom() + tableTop - barBottomMargin;
						canvas.drawRect(left,
								childBottom - mTimeBarHeight,
								left + mBorderWidth, childBottom - 1, paint);
					}
				}
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
			long now = Utils.currentTimeMillisJp();
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
		public boolean onLongClickProgram(Program p);
		public void onClickChannel(Program p);
	}
}
