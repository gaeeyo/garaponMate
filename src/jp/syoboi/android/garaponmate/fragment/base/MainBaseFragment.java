package jp.syoboi.android.garaponmate.fragment.base;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

import java.io.File;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.fragment.ErrorDialogFragment;
import jp.syoboi.android.garaponmate.utils.Utils;

public class MainBaseFragment extends ListFragment {

	private static final IntentFilter sIntentFilter = new IntentFilter();

	static {
		sIntentFilter.addAction(App.ACTION_PLAY);
		sIntentFilter.addAction(App.ACTION_STOP);
		sIntentFilter.addAction(App.ACTION_REFRESH);
		sIntentFilter.addAction(App.ACTION_HISTORY_UPDATED);
	}

	private boolean 	mNeedReload;

	OnSharedPreferenceChangeListener	mPrefsChangeListener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			if (Prefs.USER.equals(key) || Prefs.PASSWORD.equals(key)) {
				if (isResumed()) {
					reload();
				} else {
					mNeedReload = true;
				}
			}
		}
	};

	private BroadcastReceiver	mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			onReceiveLocalBroadcast(context, intent);
		}
	};

	public void onReceiveLocalBroadcast(Context context, Intent intent) {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Prefs.getInstance().registerOnSharedPreferenceChangeListener(mPrefsChangeListener);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, sIntentFilter);
	}

	@Override
	public void onDestroy() {
		Prefs.getInstance().unregisterOnSharedPreferenceChangeListener(mPrefsChangeListener);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	protected void playVideo(Program p) {
		if (getActivity() instanceof MainActivity) {
			((MainActivity)getActivity()).playVideo(p);
		}
	}

	public CharSequence getTitle() {
		return "";
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mNeedReload) {
			mNeedReload = false;
			reload();
		}
	}

	public void reload() {

	}

	public static void inflateProgramMenu(final Activity activity, ContextMenu menu, View v, ContextMenuInfo menuInfo, final Program p) {
		if (p == null) {
			return;
		}

		// Program 関連のメニューを追加
		MenuInflater inflater = activity.getMenuInflater();
		inflater.inflate(R.menu.prog_item_menu, menu);

		// デフォルトのプレイヤーをメニューから隠す
		int playerId = Prefs.getPlayerId();
		int resId = App.playerIdToResId(playerId, -1);
		if (resId != -1) {
			MenuItem mi = menu.findItem(resId);
			if (mi != null) mi.setVisible(false);
		}


		final OnMenuItemClickListener onMenuClick = new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				execCommand(activity, item.getItemId(), p);
				return true;
			}
		};

		for (int id: new int [] { R.id.playWebView, R.id.playVideoView, R.id.playPopup, R.id.playExternal, R.id.share, R.id.download, R.id.search }) {
			MenuItem mi = menu.findItem(id);
			mi.setOnMenuItemClickListener(onMenuClick);
		}
	}

	protected static void execCommand(Activity activity, int id, Program p) {
		switch (id) {
		case R.id.share:
			shareProgram(activity, p);
			break;
		case R.id.download:
			downloadProgram(activity, p);
			break;
		case R.id.playWebView:
		case R.id.playVideoView:
		case R.id.playPopup:
		case R.id.playExternal:
			if (activity instanceof MainActivity) {
				MainActivity mainActivity = (MainActivity) activity;
				int playerId = App.playerResIdToPlayerId(id, App.PLAYER_WEBVIEW);
				mainActivity.playVideo(p, playerId);
			}
			break;
		case R.id.search:
			if (activity instanceof MainActivity) {
				MainActivity mainActivity = (MainActivity)activity;
				SearchParam sp = new SearchParam();
				sp.keyword = Utils.createSearchTitle(p.title);
				mainActivity.search(sp);
			}
			break;
		}

	}

	protected static void shareProgram(Activity activity, Program p) {
		StringBuilder sb = new StringBuilder();
		sb.append(p.title)
		.append(' ')
		.append("http://site.garapon.tv/g?g=" + p.gtvid)
		.append(' ')
		.append(p.ch.bc_tags)
		.append(' ')
		.append("#ガラポンTV");

		shareText(activity, sb.toString());
	}

	protected static void shareText(Activity activity, String text) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, text);
		try {
			activity.startActivityForResult(i, 0);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
			ErrorDialogFragment.show(activity.getFragmentManager(), e);
		}
	}

	protected static void downloadProgram(Activity activity, Program p) {
		String url = "http://" + Prefs.getGaraponHost() + "/cgi-bin/play/ts.cgi?file="
				+ p.ch.ch + "/" + p.gtvid + ".ts";

		String filename =  p.title;
		if (filename == null) {
			filename = p.gtvid;
		}
		Time t = new Time();
		t.set(p.startdate);
		String dateTimeText = t.format("%Y%m%d-%H%M");

		filename = dateTimeText + " " + filename.replaceAll("./:\\*\\?\\|<>", "_")
				+ " [" + p.ch.bc + "]";

		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
		File file = new File(dir, filename + ".ts");
		downloadUrl(activity, url, file, p.title);
	}

	protected static void downloadUrl(Activity activity, String url, File path, String title) {
		DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
		req.allowScanningByMediaScanner();
		req.setTitle(title);
		req.setDescription(path.getPath());
		req.setDestinationUri(Uri.fromFile(path));
		dm.enqueue(req);
	}
}
