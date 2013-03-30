package jp.syoboi.android.garaponmate.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.SyoboiClient;

public class SettingActivity extends PreferenceActivity {

	private static final int DLG_CONFIRM_LOGOUT = 1;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);


		findPreference(Prefs.USE_SYOBOI_SERVER).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				CheckBoxPreference cbp = (CheckBoxPreference) preference;
				if (cbp.isChecked()) {
					cbp.setChecked(false);

					Intent intent = new Intent(Intent.ACTION_VIEW,
							Uri.parse(SyoboiClient.AUTH_URL));
					startActivity(intent);
				}
				return false;
			}
		});

		findPreference("logout").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				showDialog(DLG_CONFIRM_LOGOUT);
				return true;
			}
		});

//		findPreference("searchListSettings").setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			@Override
//			public boolean onPreferenceClick(Preference preference) {
//				startActivity(new Intent(SettingActivity.this, ProgSearchListActivity.class));
//				return true;
//			}
//		});
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DLG_CONFIRM_LOGOUT:
			return createLogoutDialog();
		}

		return super.onCreateDialog(id);
	}

	/**
	 * ログアウトの確認ダイアログ
	 * @return
	 */
	Dialog createLogoutDialog() {
		AlertDialog dlg = new AlertDialog.Builder(this)
			.setMessage(R.string.confirmLogout)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					App.from(SettingActivity.this).logout();
					MainActivity.startActivity(SettingActivity.this);
					finish();
				}
			})
			.setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener)null)
			.create();
		return dlg;
	}
}
