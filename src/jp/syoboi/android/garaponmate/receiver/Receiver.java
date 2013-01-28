package jp.syoboi.android.garaponmate.receiver;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import jp.syoboi.android.garaponmate.activity.DialogActivity;

public class Receiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		String action = intent.getAction();
		if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
			intent.setClass(context, DialogActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_NO_HISTORY);
			context.startActivity(intent);
		}
	}

}
