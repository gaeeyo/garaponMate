package jp.syoboi.android.garaponmate;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.SparseIntArray;
import android.widget.Toast;

import java.io.File;

import jp.syoboi.android.garaponmate.activity.LoginActivity;
import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.data.ChList;
import jp.syoboi.android.garaponmate.data.GenreGroupList;
import jp.syoboi.android.garaponmate.data.ImageLoader;
import jp.syoboi.android.garaponmate.data.SearchParamList;

import com.deploygate.sdk.DeployGate;

public class App extends Application {

	public static final String TAG = "App";

	public static boolean DEBUG = false;
	public static String VERSION = "?";

	public static final String PKG = "jp.syoboi.android.garaponmate";

	// Broadcast
	public static final String ACTION_PLAYER_ACTIVITY_FINISH = PKG + ".playerActivity.finish";
	public static final String ACTION_PLAYER_ACTIVITY_FULLSCREEN = PKG + ".playerActivity.fullScreen";
	public static final String ACTION_HISTORY_UPDATED = PKG + ".historyUpdated";

	// LocalBroadcastManager
	public static final String ACTION_PLAY = PKG + ".play";
	public static final String ACTION_STOP = PKG + ".stop";
	public static final String ACTION_REFRESH = PKG + ".refresh";

	// Extras
	public static final String EXTRA_PROGRAM = "program";
	public static final String EXTRA_SEARCH_PARAM = "searchParam";

	public static final int PLAYER_WEBVIEW = 0;
	public static final int PLAYER_VIDEOVIEW = 1;
	public static final int PLAYER_POPUP = 2;
	public static final int PLAYER_EXTERNAL = 3;

	private static SearchParamList 	sSearchParamList;
	private static ChList			sChList;
	private static SparseIntArray	sPlayerResIdToPlayerId = getPlayerResIdToPlayerIdMap();

	public static final String SEARCH_QUERY_PREFIX_CAPTION = "caption:";

	private ImageLoader 		mImageLoader;

	public static App from(Context context) {
		return (App)context.getApplicationContext();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		DeployGate.install(this, null, true);

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
		GaraponClient.setVersion(Prefs.getGtvVer());
	}

	public synchronized ImageLoader getImageLoader() {
		if (mImageLoader == null) {
			mImageLoader = new ImageLoader(this, getImageCacheDir(), 300);
		}
		return mImageLoader;
	}

	public File getImageCacheDir() {
		File f = new File(getCacheDir(), "image");
		f.mkdirs();
		return f;
	}

	public Toast showToast(CharSequence text) {
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		toast.show();
		return toast;
	}

	public File getSearchResultCacheDir() {
		File dir = new File(getCacheDir(), "search");
		dir.mkdirs();
		return dir;
	}

	public File getSearchResultCacheFile(long id) {
		return new File(getSearchResultCacheDir(), String.valueOf(id));
	}

	public void deleteSearchResultCacheFile(long id) {
		File f = getSearchResultCacheFile(id);
		f.delete();
	}

	public void clearSeachResultCache() {
		try {
			File dir = getSearchResultCacheDir();
			File [] files = dir.listFiles();
			if (files != null) {
				for (File f: files) {
					f.delete();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static SearchParamList getSearchParamList() {
		return sSearchParamList;
	}

	public static ChList getChList() {
		return sChList;
	}

	private static SparseIntArray getPlayerResIdToPlayerIdMap() {
		SparseIntArray a = new SparseIntArray();
		a.append(R.id.playWebView, PLAYER_WEBVIEW);
		a.append(R.id.playVideoView, PLAYER_VIDEOVIEW);
		a.append(R.id.playPopup, PLAYER_POPUP);
		a.append(R.id.playExternal, PLAYER_EXTERNAL);
		return a;
	}

	public static int playerResIdToPlayerId(int menuId, int fallback) {
		return sPlayerResIdToPlayerId.get(menuId, fallback);
	}

	public static int playerIdToResId(int playerId, int fallback) {
		for (int j=0; j<sPlayerResIdToPlayerId.size(); j++) {
			if (sPlayerResIdToPlayerId.valueAt(j) == playerId) {
				return sPlayerResIdToPlayerId.keyAt(playerId);
			}
		}
		return fallback;
	}

	/**
	 * 未認証ならLoginActivityに飛ばしてfinishする
	 * @param a
	 * @return
	 */
	public static boolean forwardLoginActivity(Activity a) {
		if (!Prefs.isAuthorized()) {
			LoginActivity.startActivity(a);
			a.finish();
			return true;
		}
		return false;
	}

	public void logout() {
		Prefs.logout();
		clearSeachResultCache();
	}
}
