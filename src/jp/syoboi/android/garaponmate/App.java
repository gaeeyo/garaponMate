package jp.syoboi.android.garaponmate;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class App extends Application {

	public static boolean DEBUG = false;
	public static String VERSION = "?";

	@Override
	public void onCreate() {
		super.onCreate();

		DEBUG = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

		PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			VERSION = packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		Prefs.init(this);
		GaraponClient.init(this);
	}
}
