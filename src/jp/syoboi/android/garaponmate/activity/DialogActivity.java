package jp.syoboi.android.garaponmate.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.service.DownloadService;

public class DialogActivity extends Activity {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		showDialog(1);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {

		Intent i = getIntent();
		String action = i.getAction();

		if (App.ACTION_DOWNLOAD_NOTIFICATION_CLICKED.equals(action)) {
			return onDownloadNotificationClicked();
		}
		else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
			Bundle args = getIntent().getExtras();
			final long [] downloadIds = args.getLongArray(
					DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);

			Dialog dlg = new AlertDialog.Builder(this)
				.setMessage(R.string.confirmCancelDownload)
				.setPositiveButton(android.R.string.yes, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancelDownload(downloadIds);
						finish();
					}
				})
				.setNeutralButton(android.R.string.no, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.create();

			dlg.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					finish();
				}
			});

			return dlg;
		}
		else {
			finish();
		}

		return super.onCreateDialog(id);
	}

	Dialog onDownloadNotificationClicked() {
		Bundle args = getIntent().getExtras();

		final Program p = (Program) args.getSerializable(App.EXTRA_PROGRAM);

		Dialog dlg = new AlertDialog.Builder(this)
			.setMessage(R.string.confirmCancelDownload)
			.setPositiveButton(android.R.string.yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent i = new Intent(DialogActivity.this, DownloadService.class);
					i.setAction(Intent.ACTION_DELETE);
					i.putExtra(App.EXTRA_PROGRAM, p);
					startService(i);
					finish();
				}
			})
			.setNeutralButton(android.R.string.no, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.create();

		dlg.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});
		return dlg;
	}

	void cancelDownload(long [] ids) {
		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		dm.remove(ids);
	}
}
