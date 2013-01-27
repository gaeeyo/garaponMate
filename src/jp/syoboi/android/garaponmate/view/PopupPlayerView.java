package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.WindowManager;
import android.widget.FrameLayout;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.activity.PlayerActivity;
import jp.syoboi.android.garaponmate.service.PlayerService;

public class PopupPlayerView extends PlayerView {

	private static final String TAG = "PopupPlayerView";

	ScaleGestureDetector	mScaleGesutureDetector;
	GestureDetector			mGestureDetector;
	boolean					mMaximize;
	int						mWidth;
	int						mHeight;
	int						mX;
	int						mY;
	WindowManager 			mWindowManager;
	int						mMinWidth;
	int						mMaxWidth;

	public PopupPlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		showToolbar(false);

		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		mMinWidth = (int)((48*3) * dm.density);
		mMaxWidth = Math.min(dm.widthPixels, dm.heightPixels);
		setBackgroundColor(0xff000000);

		mGestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				setMaximize(true);
				return false;
			}

			@Override
			public void onShowPress(MotionEvent e) {
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
				return false;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}
		});

		mScaleGesutureDetector = new ScaleGestureDetector(context, new OnScaleGestureListener() {

			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {

			}

			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				return true;
			}

			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				int oldWidth = mWidth;
				int oldHeight = mHeight;
				mWidth *= detector.getScaleFactor();
				if (mWidth < mMinWidth) {
					mWidth = mMinWidth;
				}
				if (mWidth > mMaxWidth) {
					mWidth = mMaxWidth;
				}
				mHeight = mWidth * 9 / 16;
				mX += (oldWidth - mWidth) / 2;
				mY += (oldHeight - mHeight) / 2;
				updatePopupPosition();
				return true;
			}
		});
	}

	private PointF mDragStartTouchPos = new PointF();
	private PointF mDragStartWindowPos = new PointF();
	private boolean	mDragging;


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mMaximize) {
			mDragging = false;
			return false;
		}
		else {
			float x = event.getRawX();
			float y = event.getRawY();
			int pointerCount = event.getPointerCount();
			if (pointerCount > 1) {
				mDragging = false;
			}

			boolean h1 = mScaleGesutureDetector.onTouchEvent(event);
			boolean h2 = mScaleGesutureDetector.isInProgress()
					|| mGestureDetector.onTouchEvent(event);

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				updatePopupPosition();
				mDragStartTouchPos.set(x, y);
				mDragStartWindowPos.set(mX, mY);
				mDragging = true;
				break;
			case MotionEvent.ACTION_MOVE:
				if (mDragging){
					mX = (int)(mDragStartWindowPos.x + (x - mDragStartTouchPos.x));
					mY = (int)(mDragStartWindowPos.y + (y - mDragStartTouchPos.y));
					updatePopupPosition();
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mDragging = false;
				break;
			}
			return h1 || h2;
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		updatePopupPosition();
	}

	@Override
	protected void onDetachedFromWindow() {
		mWindowManager = null;
		super.onDetachedFromWindow();
	}

	@Override
	protected void performClose() {
		//super.performClose();
//		stop();
		sendPlayerActivityFinish();

		Context context = getContext();
		if (context instanceof PlayerService) {
			((PlayerService)context).closePlayer();
		}
	}

	@Override
	protected void performReturnFromFullScreen() {
		// super.onReturnFromFullScreen();
		setMaximize(false);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	void updatePopupPosition() {
		if (mWidth == 0) {
			DisplayMetrics dm = getResources().getDisplayMetrics();
			int minWidth = Math.min(dm.widthPixels, dm.heightPixels);
			mX = (int)(minWidth * 0.1);
			mY = mX;
			mWidth = (int)(minWidth * 0.8);
			mHeight = (int)((minWidth * 0.8 * 9 / 16));
		}

		WindowManager.LayoutParams lp = (WindowManager.LayoutParams)getLayoutParams();
		DisplayMetrics dm = getResources().getDisplayMetrics();

		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;

		if (mX < 0) {
			mX = 0;
		} else if (mX + mWidth > screenWidth) {
			mX = screenWidth - mWidth;
		}
		if (mY < 0) {
			mY = 0;
		} else if (mY + mHeight > screenHeight) {
			mY = screenHeight - mHeight;
		}

		lp.width = mWidth;
		lp.height = mHeight;
		lp.x = mX;
		lp.y = mY;
		if (mWindowManager != null) {
			mWindowManager.updateViewLayout(this, lp);
		}
	}

	public void setMaximize(boolean maximize) {

		if (mMaximize != maximize) {

			mMaximize = maximize;
			showToolbar(maximize);
			setAutoFullScreen(maximize);

			if (maximize) {
				WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
				lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
				lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
				lp.x = 0;
				lp.y = 0;
				if (mWindowManager != null) {
					mWindowManager.updateViewLayout(this, lp);
				}

				Log.v(TAG, "startActivity");
				Intent intent = new Intent(getContext(), PlayerActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_NO_ANIMATION);
				getContext().startActivity(intent);

			} else {
				Log.v(TAG, "finish playerActivity");

				sendPlayerActivityFinish();

				updatePopupPosition();
			}
		}
	}

	void sendPlayerActivityFinish() {
		Intent intent = new Intent(App.ACTION_PLAYER_ACTIVITY_FINISH);
		getContext().sendBroadcast(intent);
	}

	@Override
	public void setFullScreen(boolean fullScreen) {
		super.setFullScreen(fullScreen);

		Intent i = new Intent(App.ACTION_PLAYER_ACTIVITY_FULLSCREEN);
		i.putExtra("fullScreen", fullScreen);
		getContext().sendBroadcast(i);
	}
}
