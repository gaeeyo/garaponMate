package jp.syoboi.android.garaponmate.fragment.base;

import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;

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
}
