package jp.syoboi.android.garaponmate;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import java.io.File;

import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.data.ChList;
import jp.syoboi.android.garaponmate.data.GenreGroupList;
import jp.syoboi.android.garaponmate.data.ProgManager;
import jp.syoboi.android.garaponmate.data.SearchParamList;

public class App extends Application {

	public static boolean DEBUG = false;
	public static String VERSION = "?";

	private static SearchParamList 	sSearchParamList;
	private static ChList			sChList;

	@Override
	public void onCreate() {
		super.onCreate();

		GenreGroupList.getInstance(this);

		DEBUG = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

		PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			VERSION = packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		sSearchParamList = new SearchParamList(new File(getFilesDir(), "searchParamList.json"));
		sChList = new ChList(new File(getFilesDir(), "chList.json"));

		Prefs.init(this);
		GaraponClient.init(this);

		File dir = getExternalCacheDir();
		ProgManager.init(this, new File(dir, "prog"));
	}

	public static SearchParamList getSearchParamList() {
		return sSearchParamList;
	}

	public static ChList getChList() {
		return sChList;
	}
}
