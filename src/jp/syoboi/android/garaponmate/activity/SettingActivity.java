package jp.syoboi.android.garaponmate.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import jp.syoboi.android.garaponmate.R;

public class SettingActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

//		findPreference("searchListSettings").setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			@Override
//			public boolean onPreferenceClick(Preference preference) {
//				startActivity(new Intent(SettingActivity.this, ProgSearchListActivity.class));
//				return true;
//			}
//		});
	}
}
