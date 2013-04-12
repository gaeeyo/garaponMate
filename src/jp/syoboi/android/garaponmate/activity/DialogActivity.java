package jp.syoboi.android.garaponmate.activity;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

public class DialogActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {

//		Intent i = getIntent();
//		String action = i.getAction();

		return super.onCreateDialog(id);
	}
}
