package jp.syoboi.android.garaponmate;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import java.io.File;

import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.data.ChList;
import jp.syoboi.android.garaponmate.data.GenreGroupList;
import jp.syoboi.android.garaponmate.data.SearchParamList;

public class App extends Application {

	public static boolean DEBUG = false;
	public static String VERSION = "?";

	public static final String PKG = "jp.syoboi.android.garaponmate";
	public static final String ACTION_PLAYER_ACTIVITY_FINISH = PKG + ".playerActivity.finish";
	public static final String ACTION_PLAYER_ACTIVITY_FULLSCREEN = PKG + ".playerActivity.fullScreen";

	public static final String EXTRA_PROGRAM = "program";

	public static final int PLAYER_WEBVIEW = 0;
	public static final int PLAYER_VIDEOVIEW = 1;
	public static final int PLAYER_POPUP = 2;
	public static final int PLAYER_EXTERNAL = 3;

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
	}

	public static SearchParamList getSearchParamList() {
		return sSearchParamList;
	}

	public static ChList getChList() {
		return sChList;
	}

	public static int PlayerResIdToPlayerId(int menuId) {
		switch (menuId) {
		case R.id.playVideoView:
			return PLAYER_VIDEOVIEW;
		case R.id.playPopup:
			return PLAYER_POPUP;
		case R.id.playExternal:
			return PLAYER_EXTERNAL;
		case R.id.playWebView:
			default:
			return PLAYER_WEBVIEW;
		}
	}
}
