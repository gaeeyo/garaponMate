package jp.syoboi.android.garaponmate.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.SyoboiClient;

public class SettingActivity extends PreferenceActivity {

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

//		findPreference("searchListSettings").setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			@Override
//			public boolean onPreferenceClick(Preference preference) {
//				startActivity(new Intent(SettingActivity.this, ProgSearchListActivity.class));
//				return true;
//			}
//		});
	}
}
