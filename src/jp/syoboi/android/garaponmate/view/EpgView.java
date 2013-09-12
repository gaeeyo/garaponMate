package jp.syoboi.android.garaponmate.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;

import jp.syoboi.a2chMate.text.BatchSpannableStringBuilder;
import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.client.GaraponClient.Ch;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.data.ChList;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.utils.Utils;

public class EpgView extends ViewGroup {

	static final int 	BLOCK_LINE_COUNT = 12;
	static EpgItemList	EMPTY_ITEM = new EpgItemList();

	static Time			mTmpTime = new Time();
	static final int	SCROLL_NONE = 0;
	static final int 	SCROLL_X = 1;
	static final int 	SCROLL_Y = 2;
	static final int 	SCROLL_XY = 3;


	final TextPaint						mTextPaint = new TextPaint();
	final Paint							mPaint = new Paint();
	final BoringLayout.Metrics 			mMetrics = new BoringLayout.Metrics();
	final SparseArray<EpgItemList>		mItems = new SparseArray<EpgItemList>();
	final BatchSpannableStringBuilder	mBSB = new BatchSpannableStringBuilder();
	final SparseIntArray				mChToColumn = new SparseIntArray();


	ChList			mChList;

	float	mStrokeWidth;
	long	mScrollTopTime;
	int		mScrollTopOffset;
	int		mTimeColumnWidth;
	int 	mColumnWidth;
	int		mLineHeight;
	float	mScale = 1.0f;
	float	mScaleMin = 1.0f;
	int		mScrollX;
	int		mScrollMode;
	Matrix	mMatrix = new Matrix();
	GestureDetector			mGestureDetector;
	ScaleGestureDetector	mScaleGestureDetector;
	MyScroller	mScroller;
	OnScrollListener	mListener;

	@SuppressWarnings("deprecation")
	public EpgView(Context context, AttributeSet attrs) {
		super(context, attrs);

		DisplayMetrics dm = getResources().getDisplayMetrics();

		mTextPaint.setTextSize(14 * dm.scaledDensity);
		mTextPaint.setAntiAlias(true);
		mTextPaint.setSubpixelText(true);

		mPaint.setAntiAlias(true);

		mStrokeWidth = Math.round(Math.max(1, 1f * dm.density));
		mScroller = new MyScroller(context);
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		Time t = new Time();
		t.set(Utils.currentTimeMillisJp());
		t.minute = 0;
		t.second = 0;

		mScrollTopTime = t.toMillis(true);
		mScrollTopOffset = 0;

		setWillNotDraw(false);

		mScaleGestureDetector = new ScaleGestureDetector(getContext(), new OnScaleGestureListener() {

			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {
			}

			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				return true;
			}

			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				float scale = mScale * detector.getScaleFactor();
				if (scale < mScaleMin) {
					scale = mScaleMin;
				}
				if (mScale != scale) {
					// 30 / 30 = [fx] 0.5
					int width = getWidth();
					int height = getHeight();
					float fx = detector.getFocusX() / width;
					float fy = detector.getFocusY() / height;
					// (30*1.2 - 30*1)=36-30=6
					float mx = (width * scale - width * mScale) * fx;
					float my = (height * scale - height * mScale) * fy;
					mScale = scale;
					scrollByDistanceX(mx / scale);
					scrollByDistanceY(my / scale);
					invalidate();
				}
				return true;
			}
		});

		mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}

			@Override
			public void onShowPress(MotionEvent e) {

			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {

				if (mScrollMode == SCROLL_NONE) {
					float dx = Math.abs(distanceX);
					float dy = Math.abs(distanceY);
					if (dy > dx * 2) {
						mScrollMode = SCROLL_Y;
					}
					else if (dx > dy * 2) {
						mScrollMode = SCROLL_X;
						getParent().requestDisallowInterceptTouchEvent(true);
					}
				}

				if ((mScrollMode & SCROLL_Y) != 0) {
					if (Math.abs(distanceY) > 0) {
						scrollByDistanceY(distanceY / mScale);
					}
				}
				if ((mScrollMode & SCROLL_X) != 0) {
					if (Math.abs(distanceX) > 0) {
						scrollByDistanceX(distanceX);
					}
				}
				return true;
			}

			@Override
			public void onLongPress(MotionEvent e) {

			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {

				mScroller.fling(0, 0, (int) velocityX, (int) velocityY,
						Integer.MIN_VALUE, Integer.MAX_VALUE,
						Integer.MIN_VALUE, Integer.MAX_VALUE);
				invalidate();
				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				if (!mScroller.isFinished()) {
					mScroller.forceFinished(false);
				}
				return true;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				mScale = 1.0f;
				invalidate();
				return true;
			}
		});
		updateMetrics();
	}

	public long getFirstVisibleTime() {
		return mScrollTopTime;
	}

	public void setOnScrollListener(OnScrollListener listener) {
		mListener = listener;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateMetrics();
	}

	void updateMetrics() {
		DisplayMetrics dm = getResources().getDisplayMetrics();

		mLineHeight = (int) Math.ceil(mTextPaint.descent() - mTextPaint.ascent());
		mTimeColumnWidth = (int) (24 * dm.scaledDensity);

		int tableWidth = getWidth() - mTimeColumnWidth;
		mColumnWidth = tableWidth / Math.max(2, (int)(tableWidth / (148 * dm.density)));


		if (mColumnWidth > 0 && mChToColumn.size() > 0) {
			boolean scaleIsMin = mScale == mScaleMin;
			float minColumnWidth = (float)tableWidth / mChToColumn.size();
			mScaleMin = minColumnWidth / mColumnWidth;

			if (scaleIsMin) {
				mScale = mScaleMin;
			}
		}

		EMPTY_ITEM.blockHeight = mLineHeight * BLOCK_LINE_COUNT;
		invalidate();
	}

	public void addData(SearchResult sr) {
		int lineHeight = mLineHeight;

		int curBlock = -1;
		EpgItemList curList = null;

		for (Program p: sr.program) {
			int startBlock = timeToBlock(p.startdate);

			if (curBlock != startBlock) {
				curBlock = startBlock;
				curList = mItems.get(curBlock);

				if (curList == null) {
					curList = new EpgItemList();
					curList.blockHeight = lineHeight * BLOCK_LINE_COUNT;
					mItems.put(curBlock, curList);
				}
			}
			curList.add(p);

			int endBlock = timeToBlock(p.startdate + p.duration);
			if (startBlock != endBlock) {
				for (int block=startBlock + 1; block<=endBlock; block++) {
					EpgItemList list = mItems.get(block);
					if (list == null) {
						list = new EpgItemList();
						list.blockHeight = lineHeight * BLOCK_LINE_COUNT;
						mItems.put(block, list);
					}
					list.add(p);
				}
			}
		}

		mChList = App.getChList();

		mChToColumn.clear();
		int column = 0;
		for (Ch ch: mChList.toArray(true)) {
			mChToColumn.put(ch.ch, column);
			column++;
		}
		updateMetrics();

		invalidate();
	}

	/**
	 * 1時間単位al
	 * @param time
	 * @return
	 */
	static int timeToBlock(long time) {
//		final Time t = mTmpTime;
//		t.set(time);
//		return t.hour
//				+ t.monthDay * 24
//				+ t.month * 24 * 31
//				+ t.year * 24 * 31 * 12;
		return (int) ((time - Utils.TIMEZONE_JP_OFFSET) / DateUtils.HOUR_IN_MILLIS);
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mChList == null) {
			return;
		}

		int saveCount = canvas.save();
//		mMatrix.setScale(mScale, mScale);
//		canvas.setMatrix(mMatrix);
		canvas.translate(mScrollX, 0);
		canvas.scale(mScale, mScale);
		try {
			drawPrograms(canvas);
		} finally {
			canvas.restoreToCount(saveCount);
		}
	}

	void drawPrograms(Canvas canvas) {
		final int height = (int) (getHeight() / mScale);
		final int lineHeight = mLineHeight;
		final int lineMin = 5;

		TextPaint textPaint = mTextPaint;
		Paint paint = mPaint;
		int blockTop = mScrollTopOffset;
		long time = mScrollTopTime;

		int columnWidth = mColumnWidth;
		int ellipsizeWidth = lineHeight;

		float strokeOffset = mStrokeWidth / 2f;
		paint.setStrokeWidth(mStrokeWidth);
		paint.setStyle(Style.STROKE);

		int blockStart = -1;

		while (blockTop < height) {
			int block = timeToBlock(time);
			if (blockStart == -1) {
				blockStart = block;
			}

			EpgItemList curList = mItems.get(block);

			// 時間の枠を描画
			if (curList != null) {
				for (EpgItem ei: curList) {
					Program p = ei.program;

					int topOffset = 0;
					if (timeToBlock(p.startdate) != block) {
						if (block != blockStart) {
							continue;
						}
						topOffset = -1;
					}

					int column = mChToColumn.get(ei.program.ch.ch);
					int left = mTimeColumnWidth + column * columnWidth;
					int right = mTimeColumnWidth + (column + 1) * columnWidth;
					int startMin = (int) ((p.startdate - time) / DateUtils.MINUTE_IN_MILLIS);
					int endMin = (int) ((p.startdate + p.duration - time) / DateUtils.MINUTE_IN_MILLIS);

					int top = blockTop + (startMin * lineHeight) / lineMin + topOffset;
					int bottom = blockTop + (endMin * lineHeight) / lineMin;

					canvas.drawRect(left + strokeOffset,
							top + strokeOffset,
							right + strokeOffset,
							bottom + strokeOffset, paint);

					if (ei.layout == null || ei.layout.getWidth() != columnWidth) {
						Spanned text = createTitle(p);

//						BoringLayout bl  = new BoringLayout(text, paint, columnWidth, Alignment.ALIGN_NORMAL, 1, 0,
//								mMetrics, false);
						StaticLayout layout  = new StaticLayout(text, textPaint, columnWidth,
								Alignment.ALIGN_NORMAL, 1, 0, false);
						int maxLineCount = (bottom - top) / lineHeight;
						if (maxLineCount < 1) {
							maxLineCount = 1;
						}
						if (layout.getLineCount() >= maxLineCount) {
							int end = layout.getLineStart(maxLineCount);
//							Log.d("", "end:" + end + " text:" + text);
							layout = new StaticLayout(text, 0, end, textPaint, columnWidth,
									Alignment.ALIGN_NORMAL, 1, 0, false, TruncateAt.END,
									columnWidth - ellipsizeWidth);
						}
						ei.layout = layout;
					}
					int saveCount = canvas.save();


					canvas.translate(left, top);
//					canvas.clipRect(top, left, right, bottom);
					canvas.clipRect(0, 0, columnWidth, bottom - top);
					ei.layout.draw(canvas);
//					canvas.translate(-left, -top);

					canvas.restoreToCount(saveCount);
				}
			}

			time += DateUtils.HOUR_IN_MILLIS;
			blockTop += lineHeight * 12;
		}
	}


	public Spanned createTitle(Program p) {
		BatchSpannableStringBuilder sb = mBSB;
		Time t = mTmpTime;
		t.set(p.startdate);
		sb.clear();

		sb.start();
		if (t.minute < 10) {
			sb.append('0');
		}
		sb.append(t.minute)
		.setSpan(new BackgroundColorSpan(0xff000000))
		.setSpan(new ForegroundColorSpan(0xffffffff));

		sb.append(' ')
		.append(p.title).append(' ');

		sb.start()
		.append(p.description)
		.setSpan(new ForegroundColorSpan(0xff008800));

		return sb.toSpanned();
	}

	@Override
	public void computeScroll() {
		super.computeScroll();
		if (mScroller.computeScrollOffset()) {
			scrollByDistanceY(mScroller.getDistanceY());
			postInvalidate();
			if (mScroller.isFinished()) {
				if (mListener != null) {
					mListener.onScrolled(this);
				}
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		mScaleGestureDetector.onTouchEvent(event);
		if (mScaleGestureDetector.isInProgress()) {
			return true;
		}
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mScrollMode != SCROLL_NONE) {
				if (mScroller.isFinished()) {
					if (mListener != null) {
						mListener.onScrolled(this);
					}
				}
			}
			mScrollMode = SCROLL_NONE;
			break;
		}

		return mGestureDetector.onTouchEvent(event);
	}

	private void scrollByDistanceY(float distanceY) {
		int block = timeToBlock(mScrollTopTime);
		EpgItemList list = mItems.get(block);
		if (list == null) {
			list = EMPTY_ITEM;
		}

		mScrollTopOffset -= distanceY;
		if (mScrollTopOffset > 0) {
			mScrollTopTime -= DateUtils.HOUR_IN_MILLIS;
			mScrollTopOffset -= list.blockHeight;
		} else {
			if (mScrollTopOffset < -list.blockHeight) {
				mScrollTopTime += DateUtils.HOUR_IN_MILLIS;
				mScrollTopOffset += list.blockHeight;
			}
		}
		invalidate();
	}

	private void scrollByDistanceX(float distanceX) {
		int scrollX = (int) (mScrollX - distanceX);
		if (scrollX > 0) {
			scrollX = 0;
		}
		if (scrollX != mScrollX) {
			mScrollX = scrollX;
			invalidate();
		}
	}


	static class EpgItemList extends ArrayList<EpgItem> {
		private static final long serialVersionUID = 1L;
		public int blockHeight;

		public EpgItemList() {
		}

		public boolean exists(Program p) {
			final String gtvid = p.gtvid;
			for (EpgItem i: this) {
				if (gtvid.equals(i.program.gtvid)) {
					return true;
				}
			}
			return false;
		}

		public boolean add(Program p) {
			if (!exists(p)) {
				super.add(new EpgItem(p));
				return true;
			}
			return false;
		}
	}

	static class EpgItem {
		public Program program;
		public Layout	layout;
		public EpgItem(Program p) {
			this.program = p;
		}
	}

	static class MyScroller extends Scroller {

		int mPrevY;

		public MyScroller(Context context) {
			super(context);
		}

		@Override
		public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
			super.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
			mPrevY = 0;
		}

		public int getDistanceY() {
			int curY = getCurrY();
			int distance = mPrevY - curY;
			mPrevY = curY;
			return distance;
		}
	}

	public static interface OnScrollListener {
		public void onScrolled(EpgView v);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub

	}


}
