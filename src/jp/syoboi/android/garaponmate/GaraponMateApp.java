package jp.syoboi.android.garaponmate;

import android.app.Application;

public class GaraponMateApp extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Prefs.init(this);
		GaraponClient.init(this);
	}
}
