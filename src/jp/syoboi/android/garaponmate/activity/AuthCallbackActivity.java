package jp.syoboi.android.garaponmate.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;

public class AuthCallbackActivity extends Activity {

//	private static final String TAG = "AuthCallbackActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		Uri uri = i.getData();
		String token = uri.getPathSegments().get(0);

		Toast.makeText(this, getString(R.string.loginCompleted), Toast.LENGTH_SHORT).show();

		Prefs.setSyoboiToken(token);

		startSettingActivity();
	}

	void startSettingActivity() {
		Intent i = new Intent(this, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		finish();
	}
}
