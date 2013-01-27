package jp.syoboi.android.garaponmate.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.view.PopupPlayerView;

public class PlayerService extends Service {
	private static final String TAG = "PlayerService";

	private static final String ACTION_BASE = "jp.syoboi.android.garaponmate.action.";

	public static final String ACTION_SET_VIDEO = ACTION_BASE + "setVideo";
	public static final String ACTION_PLAY = ACTION_BASE + "play";
	public static final String ACTION_PAUSE = ACTION_BASE + "pause";
	public static final String ACTION_FF = ACTION_BASE + "ff";
	public static final String ACTION_REW = ACTION_BASE + "rew";
	public static final String ACTION_RETURN_FROM_FULLSCREEN = ACTION_BASE + "returnFromFullScreen";

	PopupPlayerView	mPlayerView;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		if (intent == null) {

		} else {
			String action = intent.getAction();

			Log.v(TAG, "onStart action:" + action);

			if (ACTION_SET_VIDEO.equals(action)) {
				ensurePlayer();
				Program p = (Program) intent.getSerializableExtra(App.EXTRA_PROGRAM);
				mPlayerView.setVideo(p, App.PLAYER_VIDEOVIEW);
			}
			else if (ACTION_RETURN_FROM_FULLSCREEN.equals(action)) {
				if (mPlayerView != null) {
					mPlayerView.setMaximize(false);
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	void ensurePlayer() {
		if (mPlayerView == null) {
			mPlayerView = new PopupPlayerView(this, null);
			WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
					WindowManager.LayoutParams.MATCH_PARENT,
					WindowManager.LayoutParams.MATCH_PARENT,
					WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
					| WindowManager.LayoutParams.FLAG_FULLSCREEN
					| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
					| WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,

					PixelFormat.TRANSLUCENT);
			lp.gravity = Gravity.LEFT | Gravity.TOP;

			wm.addView(mPlayerView, lp);
		}
	}

	public void closePlayer() {
		if (mPlayerView != null) {
			mPlayerView.destroy();

			WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.removeView(mPlayerView);
		}
		stopSelf();
	}
}
