package jp.syoboi.android.garaponmate.fragment.base;

import android.app.DownloadManager;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.format.Time;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;

import java.io.File;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.fragment.ErrorDialogFragment;

public class MainBaseFragment extends ListFragment {

	protected void playVideo(Program p) {
		if (getActivity() instanceof MainActivity) {
			((MainActivity)getActivity()).playVideo(p);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		int playerId = Prefs.getPlayerId();
		int resId = App.playerIdToResId(playerId, -1);
		if (resId != -1) {
			MenuItem mi = menu.findItem(resId);
			if (mi != null) mi.setVisible(false);
		}
	}

	protected void execCommand(int id, Program p) {
		switch (id) {
		case R.id.share:
			shareProgram(p);
			break;
		case R.id.download:
			downloadProgram(p);
			break;
		case R.id.playWebView:
		case R.id.playVideoView:
		case R.id.playPopup:
		case R.id.playExternal:
			if (getActivity() instanceof MainActivity) {
				MainActivity activity = (MainActivity) getActivity();
				int playerId = App.playerResIdToPlayerId(id, App.PLAYER_WEBVIEW);
				activity.playVideo(p, playerId);
			}
			break;
		}

	}

	protected void shareProgram(Program p) {
		StringBuilder sb = new StringBuilder();
		sb.append(p.title)
		.append(' ')
		.append("http://site.garapon.tv/g?g=" + p.gtvid)
		.append(' ')
		.append(p.ch.bc_tags)
		.append(' ')
		.append("#ガラポンTV");

		shareText(sb.toString());
	}

	protected void shareText(String text) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, text);
		try {
			startActivityForResult(i, 0);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
			ErrorDialogFragment.show(getFragmentManager(), e);
		}
	}

	protected void downloadProgram(Program p) {
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
		downloadUrl(url, file, p.title);
	}

	protected void downloadUrl(String url, File path, String title) {
		DownloadManager dm = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
		req.allowScanningByMediaScanner();
		req.setTitle(title);
		req.setDescription(path.getPath());
		req.setDestinationUri(Uri.fromFile(path));
		dm.enqueue(req);
	}
}