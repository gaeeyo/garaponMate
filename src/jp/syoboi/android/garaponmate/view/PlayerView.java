package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;

public class PlayerView extends RelativeLayout {

	private static final String TAG = "PlayerView";

	private static final int INTERVAL = 500;

	private static final int [] PLAYER_BUTTONS = { R.id.pause, R.id.previous, R.id.rew, R.id.ff, R.id.next };

	Handler			mHandler = new Handler();
	View			mOverlay;
	View			mToolbar;
	ImageButton		mPauseButton;
	boolean			mPause;
	FrameLayout		mPlayerViewContainer;
	int				mDuration;
	SeekBar			mSeekBar;
	int				mCurPos;
	boolean			mUseVideoView;

	PlayerInterface	mPlayer;

	public PlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (isInEditMode()) {
			return;
		}

		inflate(context, R.layout.player_controls, this);

		mSeekBar = (SeekBar) findViewById(R.id.seekBar);
		mPlayerViewContainer = (FrameLayout)findViewById(R.id.playerViewContainer);

		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					mPlayer.seek(progress);
				}
			}
		});

		// ボタン
		mToolbar = findViewById(R.id.playerToolbar);
		for (int id: PLAYER_BUTTONS) {
			findViewById(id).setOnClickListener(mOnClickListener);
		}

		mPauseButton = (ImageButton)findViewById(R.id.pause);

		// オーバーレイ
		mOverlay = findViewById(R.id.playerViewOverlay);
	}

	Runnable	mIntervalRunnable = new Runnable() {
		@Override
		public void run() {
			if (mPlayer != null) {
				int duration = mPlayer.getDuration();
				if (mDuration != duration) {
					mDuration = duration;
					if (duration == 0) {
						mSeekBar.setVisibility(View.GONE);
					} else {
						mSeekBar.setVisibility(View.VISIBLE);
						mSeekBar.setMax(duration);
					}
				}
				int curPos = mPlayer.getCurrentPos();
				if (mCurPos != curPos) {
					mCurPos = curPos;
					if (mDuration > 0) {
						mSeekBar.setProgress(curPos);
					}
				}
				if (!mPause) {
					mHandler.postDelayed(mIntervalRunnable, INTERVAL);
				}
			}
		}
	};

	public void showToolbar(boolean show) {
		if (show) {
			mToolbar.setVisibility(View.VISIBLE);
			if (mDuration > 0) {
				mSeekBar.setVisibility(View.VISIBLE);
			}
		} else {
			mToolbar.setVisibility(View.GONE);
			mSeekBar.setVisibility(View.GONE);
		}
	}

	View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.pause:
				mPause = !mPause;
				if (mPause) {
					pause();
				} else {
					play();
				}
				updatePauseButton();
				break;
			case R.id.previous:
				jump(-15);
				break;
			case R.id.rew:
				jump(-15);
				break;
			case R.id.ff:
				jump(30);
				break;
			case R.id.next:
				jump(30);
				break;
			};
		}
	};

	void updatePauseButton() {
		mPauseButton.setImageResource(
				mPause
				? R.drawable.ic_media_play
				: R.drawable.ic_media_pause);
	}

	public void onPause() {
		mHandler.removeCallbacks(mIntervalRunnable);
		if (mPlayer != null) {
			mPlayer.onPause();
		}
	}

	public void onResume() {
		if (mPlayer != null) {
			mPlayer.onResume();
		}
		if (!mPause) {
			mHandler.postDelayed(mIntervalRunnable, INTERVAL);
		}
	}

	public void destroy() {
		mHandler.removeCallbacks(mIntervalRunnable);
		if (mPlayer != null) {
			if (mPlayer != null) {
				mPlayer.destroy();
				mPlayerViewContainer.removeAllViews();
				mPlayer = null;
			}
		}
	}

	public void setVideo(final String id) {

		boolean useVideoView = Prefs.useVideoView();
		if (mPlayer != null && useVideoView != mUseVideoView) {
			mUseVideoView = useVideoView;
			destroy();
		}

		if (mPlayer == null) {
			if (useVideoView) {
				mPlayer = createPlayerVideoView();
			} else {
				mPlayer = createPlayerWebView();
			}
			mPlayerViewContainer.addView(mPlayer.getView(),
					new FrameLayout.LayoutParams(
							LayoutParams.MATCH_PARENT,
							LayoutParams.MATCH_PARENT,
							Gravity.CENTER));
		}
		mSeekBar.setVisibility(View.GONE);
		mPlayer.setVideo(id);

		mHandler.postDelayed(mIntervalRunnable, INTERVAL);
	}

	PlayerInterface createPlayerWebView() {
		return new PlayerWebView(getContext());
	}

	PlayerInterface createPlayerVideoView() {
		return new PlayerVideoView(getContext());
	}

	public void jump(int sec) {
		if (mPlayer != null) {
			mPlayer.jump(sec);
		}
	}

	public void play() {
		if (mPlayer != null) {
			mPlayer.play();
		}
		mHandler.postDelayed(mIntervalRunnable, INTERVAL);
	}

	public void pause() {
		if (mPlayer != null) {
			mPlayer.pause();
		}
		mHandler.removeCallbacks(mIntervalRunnable, INTERVAL);
	}

	public void stop() {
		if (mPlayer != null) {
			mPlayer.stop();
		}
	}

	public static interface PlayerInterface {
		public void setVideo(String id);
		public void play();
		public void stop();
		public void pause();
		public void onPause();
		public void onResume();
		public void destroy();
		public void seek(int msec);
		public int getDuration();
		public int getCurrentPos();
		public void jump(int msec);
		public View getView();
	}
}
