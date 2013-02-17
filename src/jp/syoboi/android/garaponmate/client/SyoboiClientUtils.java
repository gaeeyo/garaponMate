package jp.syoboi.android.garaponmate.client;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.client.SyoboiClient.Histories;
import jp.syoboi.android.garaponmate.client.SyoboiClient.History;
import jp.syoboi.android.garaponmate.data.Program;

public class SyoboiClientUtils {

	private static final String TAG = "SyoboiClientUtils";

	static Histories sHistories = new Histories();
	static SyncTask sSyncTask;

	public synchronized static Histories getHistories(Context context) {
		if (sHistories == null) {
			sHistories = new Histories();
		}
		return sHistories;
	}


	public static void sendPlayAsync(Context context, final Program p, final int msec) {
		final Context appContext = context.getApplicationContext();

		String token = Prefs.getSyoboiToken();
		if (TextUtils.isEmpty(token)) {
			return ;
		}

		new Thread() {
			@Override
			public void run() {
				super.run();
				try {
					SyoboiClient.sendPlay(p, msec);
					syncHistories(appContext);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static void syncHistories(Context context) {

		if (sSyncTask == null) {

			final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
			Histories histories = getHistories(context);

			String token = Prefs.getSyoboiToken();
			if (TextUtils.isEmpty(token)) {
				return;
			}
			if (App.DEBUG) {
				Log.d(TAG, "syncHistories");
			}
			sSyncTask = new SyncTask(histories) {
				@Override
				protected void onPostExecute(Object result) {
					sSyncTask = null;
					if (result != null) {
						Intent intent = new Intent(App.ACTION_HISTORY_UPDATED);
						lbm.sendBroadcast(intent);
					}
				}
				@Override
				protected void onCancelled(Object result) {
					sSyncTask = null;
				}
			};
			sSyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private static class SyncTask extends AsyncTask<Object, Object, Object> {

		Histories mHistories;

		public SyncTask(Histories histories) {
			mHistories = histories;
		}

		@Override
		protected Object doInBackground(Object... params) {
			long watchedAt = mHistories.getWatchedAtMax();
			int updated = 0;
			try {
				ArrayList<History> list = SyoboiClient.fetchHistories(watchedAt);
				if (App.DEBUG) {
					Log.d(TAG, "SyncTask result.size:" + list.size());
				}
				updated = list.size();
				mHistories.merge(list);

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return updated;
		}

	}
}
