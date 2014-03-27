package jp.syoboi.android.garaponmate.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.SearchRecentSuggestions;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.SyoboiClient;
import jp.syoboi.android.garaponmate.provider.MySearchRecentSuggestionsProvider;

public class SettingActivity extends PreferenceActivity {

	private static final int DLG_CONFIRM_LOGOUT = 1;
	private static final int DLG_OSS = 2;

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

		findPreference("clearSearchHistory").setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				preference.setEnabled(false);

		        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
		        		SettingActivity.this,
		                MySearchRecentSuggestionsProvider.AUTHORITY,
		                MySearchRecentSuggestionsProvider.MODE);
		        suggestions.clearHistory();
		        App.from(getApplicationContext()).showToast(
		        		getString(R.string.clearSearchHistoryCompleted));

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
		
		findPreference("oss").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				showDialog(DLG_OSS);
				return true;
			}
		});
		
		Preference ver = findPreference("version");
		PackageInfo pi;
		try {
			pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			ver.setSummary(pi.versionName 
					+ " (" + pi.versionCode + ") "
					+ DateUtils.formatDateTime(this, pi.lastUpdateTime,
							DateUtils.FORMAT_SHOW_WEEKDAY
							| DateUtils.FORMAT_SHOW_DATE
							| DateUtils.FORMAT_SHOW_WEEKDAY
							| DateUtils.FORMAT_SHOW_TIME));
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DLG_CONFIRM_LOGOUT:
			return createLogoutDialog();
		case DLG_OSS:
			return createOssDialog();
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
	
	/**
	 * OSSダイアログ
	 */
	Dialog createOssDialog() {
		Dialog dlg = new Dialog(this);
		WebView webView = new WebView(dlg.getContext());
		dlg.setTitle(R.string.openSourceLicense);
		
		webView.getSettings().setAllowFileAccess(true);
		webView.loadUrl("file:///android_asset/oss.html");

		dlg.setContentView(webView,
				new ViewGroup.LayoutParams(
						LayoutParams.MATCH_PARENT, 
						ViewGroup.LayoutParams.WRAP_CONTENT));
		
		return dlg;
	}
}
