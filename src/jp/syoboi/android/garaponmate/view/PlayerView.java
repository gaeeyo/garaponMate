package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.client.GaraponClient.ApiResult;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.client.SyoboiClientUtils;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.task.SearchTask;

public class PlayerView extends RelativeLayout implements PlayerViewCallback {

	private static final String TAG = "PlayerView";

	private static final int INTERVAL = 500;
	private static final int CHANGE_FULLSCREEN_DELAY = 5 * 1000;
	private static final int SEND_PLAY_DELAY = 10*1000;

	public static Program	sLatestProgram;

	Handler			mHandler = new Handler();
	View			mKeyGuard;
	ImageView		mFavorite;
	FrameLayout		mPlayerViewContainer;
	TextView		mTitle;
	TextView		mMessage;
	TextView		mBufferingView;
	View			mTitleBar;
	SearchTask		mSearchTask;
	PlayerOverlay	mPlayerOverlay;

	boolean			mPause;
	boolean			mUseVideoView;
	boolean			mFullScreen;
	boolean			mAutoFullScreen;
	Program			mProgram;

	PlayerViewInterface	mPlayer;

	public PlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (isInEditMode()) {
			return;
		}

		inflate(context, R.layout.player_view, this);

		mPlayerOverlay = (PlayerOverlay) findViewById(R.id.playerOverlay);

		mPlayerViewContainer = (FrameLayout)findViewById(R.id.playerViewContainer);
		mTitle = (TextView) findViewById(R.id.title);
		mMessage = (TextView) findViewById(R.id.message);
		mFavorite = (ImageView) findViewById(R.id.favorite);
		mBufferingView = (TextView) findViewById(R.id.buffering);
		mTitleBar = findViewById(R.id.playerTitlebar);
		mFavorite.setEnabled(false);
		setMessage(null);

		mPlayerOverlay.setPlayer(this);

		findViewById(R.id.close).setOnClickListener(mOnClickListener);
		findViewById(R.id.returnFromFullScreen).setOnClickListener(mOnClickListener);

		mFavorite.setOnClickListener(mOnClickListener);


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

		// オーバーレイ
		mKeyGuard = findViewById(R.id.playerKeyGuard);
	}


	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			cancelFullScreen();
			break;
		case MotionEvent.ACTION_UP:
			if (!mFullScreen) {
				startFullScreenDelay();
			}
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	void togglePause() {
		mPause = !mPause;
		if (mPause) {
			pause();
		} else {
			play();
		}
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



	public void showToolbar(boolean show) {
		if (show) {
			mPlayerOverlay.setVisibility(View.VISIBLE);
			mTitleBar.setVisibility(View.VISIBLE);
		} else {
			mTitleBar.setVisibility(View.GONE);
			mPlayerOverlay.setVisibility(View.GONE);
		}
	}

	public boolean isToolbarShown() {
		return mTitleBar.getVisibility() == View.VISIBLE;
	}

	View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
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
			.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}


	public void onPause() {
		mPlayerOverlay.onPause();

		if (mPlayer != null) {
			int pos = mPlayer.getCurrentPos();
			if (pos >= 0) {
				SyoboiClientUtils.sendPlayAsync(getContext(), mProgram, pos);
			}
			mPlayer.onPause();
		}
	}

	public void onResume() {
		mPlayerOverlay.onResume();

		if (mPlayer != null) {
			mPlayer.onResume();
		}
	}


	public void destroy() {
		destroyInternal(true);
	}

	void destroyInternal(boolean notification) {
		if (notification) {
			sLatestProgram = null;
			LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
			lbm.sendBroadcast(new Intent(App.ACTION_STOP));
		}

		mPlayerOverlay.onPause();
		if (mPlayer != null) {
			mPlayer.destroy();
			mPlayer = null;
		}
		mPlayerViewContainer.removeAllViews();
	}

	public void setVideo(Program p, int playerId) {
		setProgram(p);
		startFullScreenDelay();
		setVideoInternal(p.gtvid, playerId);
		updateControls();

		// 通知
		sLatestProgram = p;
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
		Intent intent = new Intent(App.ACTION_PLAY);
		intent.putExtra(App.EXTRA_PROGRAM, p);
		lbm.sendBroadcast(intent);

		SyoboiClientUtils.sendPlayAsync(getContext(), p, 0);

		cancelSearchTask();
		startGetProgramDetailTask(p);
	}

	/**
	 * 番組詳細を取得するタスクをキャンセル
	 */
	void cancelSearchTask() {
		if (mSearchTask != null) {
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
	}

	/**
	 * 番組詳細を取得するタスクを開始
	 * @param p
	 */
	void startGetProgramDetailTask(Program p) {
		SearchParam sp = new SearchParam();
		sp.gtvid = p.gtvid;
		sp.count = 1;
		sp.searchType = SearchParam.STYPE_CAPTION;
		sp.video = SearchParam.VIDEO_ALL;

		mSearchTask = new SearchTask(getContext(), sp, false) {
			@Override
			protected void onPostExecute(Object result) {
				if (result instanceof SearchResult) {
					SearchResult sr = (SearchResult) result;
					if (sr.program.size() > 0) {
						mProgram = sr.program.get(0);
						mPlayerOverlay.setProgramDetail(mProgram);
					}
				}
				else if (result instanceof Exception) {
					App.from(getContext()).showToast(String.valueOf(result));
				}
				mSearchTask = null;
			}
		};
		mSearchTask.execute();
	}


	void setProgram(Program p) {
		mProgram = p;

		mTitle.setText(p.title);
		mPlayerOverlay.setProgram(p);
	}

	/**
	 * Playerにビデオを設定
	 * @param id
	 * @param playerId
	 */
	void setVideoInternal(final String id, int playerId) {
		if (App.DEBUG) {
			Log.d(TAG, "setVideoInternal id:" + id);
		}

		boolean useVideoView = (playerId == App.PLAYER_VIDEOVIEW);
		onMessage(null);
		onBuffering(0, 0);
//		if (mPlayer != null && useVideoView != mUseVideoView) {
			mUseVideoView = useVideoView;
			destroyInternal(false);
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
		mPlayer.setVideo(id);
		mPause = false;
	}

	PlayerViewInterface createPlayerWebView() {
		return new PlayerWebView2(getContext(), this);
	}

	PlayerViewInterface createPlayerVideoView() {
		return new PlayerVideoView(getContext(), this);
	}


	public void seek(int msec) {
		if (mPlayer != null) {
			mPlayer.seek(msec);
		}
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
	}

	public void pause() {
		if (mPlayer != null) {
			mPlayer.pause();
		}
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
		mPlayerOverlay.onPlayerStateChanged();
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
		mPlayerOverlay.onPlayerStateChanged();
	}

	public static String getLatestPlayingId() {
		return sLatestProgram != null ? sLatestProgram.gtvid : null;
	}


	int mBufferingPos;
	int mBufferingMax;


	@Override
	public void onBuffering(int pos, int max) {
		mBufferingPos = pos;
		mBufferingMax = max;
		mHandler.removeCallbacks(mUpdateBufferingRunnable);
		mHandler.post(mUpdateBufferingRunnable);
	}

	Runnable mUpdateBufferingRunnable = new Runnable() {

		@Override
		public void run() {
			if (mBufferingPos >= mBufferingMax || mBufferingMax == 0) {
				mBufferingView.setVisibility(View.INVISIBLE);
			} else {
				mBufferingView.setText("Buffering " + (mBufferingPos * 100 / mBufferingMax) + "%");
				mBufferingView.setVisibility(View.VISIBLE);
			}
		}
	};
}
