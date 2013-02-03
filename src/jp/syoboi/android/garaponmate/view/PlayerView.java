package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.Locale;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.client.GaraponClient.ApiResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.Program;

public class PlayerView extends RelativeLayout implements PlayerViewCallback {

	private static final String TAG = "PlayerView";

	private static final int INTERVAL = 500;
	private static final int CHANGE_FULLSCREEN_DELAY = 3000;

	private static final int [] PLAYER_BUTTONS = { R.id.pause, R.id.previous, R.id.rew, R.id.ff, R.id.next };

	Handler			mHandler = new Handler();
	View			mOverlay;
	View			mToolbar;
	ImageView		mFavorite;
	ImageButton		mPauseButton;
	boolean			mPause;
	FrameLayout		mPlayerViewContainer;
	int				mDuration;
	SeekBar			mSeekBar;
	int				mCurPos;
	boolean			mUseVideoView;
	TextView		mTime;
	TextView		mTitle;
	TextView		mMessage;
	boolean			mFullScreen;
	boolean			mAutoFullScreen;
	Program			mProgram;

	PlayerViewInterface	mPlayer;

	public PlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (isInEditMode()) {
			return;
		}

		inflate(context, R.layout.player_controls, this);

		mSeekBar = (SeekBar) findViewById(R.id.seekBar);
		mPlayerViewContainer = (FrameLayout)findViewById(R.id.playerViewContainer);
		mTime = (TextView) findViewById(R.id.time);
		mTitle = (TextView) findViewById(R.id.title);
		mMessage = (TextView) findViewById(R.id.message);
		mFavorite = (ImageView) findViewById(R.id.favorite);
		mFavorite.setEnabled(false);
		setMessage(null);

		findViewById(R.id.close).setOnClickListener(mOnClickListener);
		findViewById(R.id.returnFromFullScreen).setOnClickListener(mOnClickListener);

		mFavorite.setOnClickListener(mOnClickListener);

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

		setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
			@Override
			public void onSystemUiVisibilityChange(int visibility) {
				final int FS_FLAGS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
				if (mFullScreen) {
					if ((visibility & FS_FLAGS) == 0) {
						// フルスクリーンが解除された
						cancelFullScreen();
						startFullScreenDelay();
					}
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


	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if (mFullScreen) {
				cancelFullScreen();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (!mFullScreen) {
				startFullScreenDelay();
			}
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	public void setAutoFullScreen(boolean fullScreen) {
		mAutoFullScreen = fullScreen;
		if (fullScreen) {
			if (!mPause) {
				startFullScreenDelay();
			}
		} else {
			cancelFullScreen();
		}
	}

	public boolean isFullScreen() {
		return mFullScreen;
	}

	protected void performClose() {
		Context context = getContext();
		if (context instanceof MainActivity) {
			((MainActivity) context).closePlayer();
		}
	}

	protected void performReturnFromFullScreen() {
		Context context = getContext();
		if (context instanceof MainActivity) {
			((MainActivity) context).expandPlayer(false);
		}
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
						mTime.setText(getTimeStr(mCurPos) + " / "
								+ getTimeStr(mDuration));
					}
				}
				if (!mPause) {

					mHandler.postDelayed(mIntervalRunnable, INTERVAL - (curPos % 1000) );
				}
			}
		}
	};

	String getTimeStr(int millis) {
		int sec = millis / 1000;
		if (sec > 60*60) {
			int min = sec / 60;
			return String.format(Locale.ENGLISH, "%d:%02d:%02d",
					min / 60, min % 60, sec % 60);
		}
		return String.format(Locale.ENGLISH, "%d:%02d", sec / 60, sec % 60);
	}

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

	public boolean isToolbarShown() {
		return mToolbar.getVisibility() == View.VISIBLE;
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
			case R.id.returnFromFullScreen:
				performReturnFromFullScreen();
				break;
			case R.id.close:
				performClose();
				break;
			case R.id.favorite:
				performFavorite();
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

	protected void performFavorite() {
		if (mProgram != null) {
			mFavorite.setEnabled(false);

			final String gtvid = mProgram.gtvid;
			final boolean setFavorite = !mProgram.hasFlag(Program.FLAG_FAVORITE);

			new AsyncTask<Object,Object,Object>() {
				@Override
				protected Object doInBackground(Object... params) {
					try {
						return GaraponClientUtils.favorite(gtvid, setFavorite);
					} catch (Exception e) {
						return e;
					}
				}
				@Override
				protected void onPostExecute(Object result) {
					mFavorite.setEnabled(true);
					if (result instanceof ApiResult) {
						ApiResult r = (ApiResult) result;
						if (r.status == GaraponClient.STATUS_SUCCESS) {
							if (gtvid.equals(mProgram.gtvid)) {
								if (setFavorite) {
									mProgram.addFlag(Program.FLAG_FAVORITE);
								} else {
									mProgram.clearFlag(Program.FLAG_FAVORITE);
								}
							}
						}
						updateControls();
					}
					else if (result instanceof Exception) {
						Exception e = (Exception) result;
						onMessage(e.toString());
					}
				}
			}
			.execute();
		}
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
			mPlayer.destroy();
			mPlayer = null;
		}
		mPlayerViewContainer.removeAllViews();
	}

	public void setVideo(Program p, int playerId) {
		mProgram = p;
		startFullScreenDelay();
		mTitle.setText(p.title);
		mDuration = (int) p.duration;
		if (p.duration != 0) {
			mTime.setText(getTimeStr(mDuration));
		} else {
			mTime.setText(null);
		}
		setVideoInternal(p.gtvid, playerId);
		updateControls();
	}

	void setVideoInternal(final String id, int playerId) {
		Log.v(TAG, "setVideoInternal id:" + id);

		boolean useVideoView = (playerId == App.PLAYER_VIDEOVIEW);
//		if (mPlayer != null && useVideoView != mUseVideoView) {
			mUseVideoView = useVideoView;
			destroy();
//		}

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

	PlayerViewInterface createPlayerWebView() {
		return new PlayerWebView(getContext());
	}

	PlayerViewInterface createPlayerVideoView() {
		return new PlayerVideoView(getContext(), this);
	}

	public void jump(int sec) {
		if (mPlayer != null) {
			mPlayer.jump(sec);
		}
	}

	public void play() {
		startFullScreenDelay();
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

//	public void stop() {
//		if (mPlayer != null) {
//			mPlayer.stop();
//		}
//	}

	public View getPlayerView() {
		if (mPlayer != null) {
			return mPlayer.getView();
		}
		return null;
	}

	@Override
	public void onMessage(String message) {
		cancelFullScreen();
		setMessage(message);
	}

	void setMessage(String message) {
		if (TextUtils.isEmpty(message)) {
			mMessage.setVisibility(View.GONE);
		} else {
			mMessage.setVisibility(View.VISIBLE);
			mMessage.setText(message);
		}
	}

	Runnable mChangeFullScreenRunnable = new Runnable() {
		@Override
		public void run() {
			setFullScreen(true);
		};
	};

	/**
	 * フルスクリーンへの自動的な移行を開始
	 */
	void startFullScreenDelay() {
		mHandler.removeCallbacks(mChangeFullScreenRunnable);
		if (mAutoFullScreen) {
			mHandler.postDelayed(mChangeFullScreenRunnable, CHANGE_FULLSCREEN_DELAY);
		}
	}

	/**
	 * フルスクリーンへの移行をキャンセル
	 */
	void cancelFullScreen() {
		mHandler.removeCallbacks(mChangeFullScreenRunnable);
		if (mFullScreen) {
			setFullScreen(false);
		}
	}

	/**
	 * フルスクリーン設定
	 * @param fullScreen
	 */
	public void setFullScreen(boolean fullScreen) {
		int FS_FLAGS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LOW_PROFILE;
		View view = this;

		int systemUiVisibility = view.getSystemUiVisibility();
		if (fullScreen) {
			systemUiVisibility |= FS_FLAGS;
			mFullScreen = true;
			showToolbar(false);
		} else {
			if (mFullScreen || (systemUiVisibility & FS_FLAGS) != 0) {
				systemUiVisibility &= ~FS_FLAGS;
				mFullScreen = false;
				showToolbar(true);
			}
		}
		view.setSystemUiVisibility(systemUiVisibility);
	}

	public void updateControls() {
		if (mProgram == null) {
			mFavorite.setImageResource(R.drawable.ic_star_off);
			mFavorite.setEnabled(false);
		} else {
			mFavorite.setEnabled(true);
			boolean favorited = mProgram.hasFlag(Program.FLAG_FAVORITE);
			mFavorite.setImageResource(
					favorited ? R.drawable.ic_star_on : R.drawable.ic_star_off);
		}
	}
}
