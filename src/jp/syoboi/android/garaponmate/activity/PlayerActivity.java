package jp.syoboi.android.garaponmate.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.service.PlayerService;

public class PlayerActivity extends Activity {

	private static final String TAG = "PlayerActivity";

	static IntentFilter sIntentFilter;

	static {
		sIntentFilter = new IntentFilter();
		sIntentFilter.addAction(App.ACTION_PLAYER_ACTIVITY_FINISH);
		sIntentFilter.addAction(App.ACTION_PLAYER_ACTIVITY_FULLSCREEN);
	}

	BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null) {
				return;
			}
			String action = intent.getAction();

			if (action.equals(App.ACTION_PLAYER_ACTIVITY_FINISH)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						finish();
						overridePendingTransition(0, 0);
					}
				});
			}
			else if (action.equals(App.ACTION_PLAYER_ACTIVITY_FULLSCREEN)) {
				final boolean fullScreen = intent.getBooleanExtra("fullScreen", false);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setFullScreen(fullScreen);
					};
				});
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		registerReceiver(mReceiver, sIntentFilter);
	}

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		sendReturnFromFullScreen();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		sendReturnFromFullScreen();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	void sendReturnFromFullScreen() {
		if (!isFinishing()) {
			Intent i = new Intent(this, PlayerService.class);
			i.setAction(PlayerService.ACTION_RETURN_FROM_FULLSCREEN);
			startService(i);
			finish();
			overridePendingTransition(0, 0);
		}
	}

	public void setFullScreen(boolean fullscreen) {
		int FS_FLAGS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LOW_PROFILE;
		View view = getWindow().getDecorView();

		int systemUiVisibility = view.getSystemUiVisibility();
		if (fullscreen) {
			systemUiVisibility |= FS_FLAGS;
		} else {
			systemUiVisibility &= ~FS_FLAGS;
		}
		view.setSystemUiVisibility(systemUiVisibility);
	}
}
