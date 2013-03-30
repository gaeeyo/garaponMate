package jp.syoboi.android.garaponmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.HashMap;

import jp.syoboi.android.garaponmate.data.ProgSearchList;

public class Prefs {
	@SuppressWarnings("unused")
	private static final String TAG = "Prefs";

	private static final String USER = "user";
	private static final String PASSWORD = "password";

	private static final String GTV_VER = "gtvver";
	public static final String IP_ADDR = "ipaddr";
	public static final String P_IP_ADDR = "pipaddr";
	public static final String G_IP_ADDR = "gipaddr";
	public static final String PORT = "port";
	public static final String TS_PORT = "port2";

	private static final String GTV_SESSION_ID = "gtvSessionId";
	private static final String GARAPON_AUTH = "garaponAuth";
	private static final String COMMON_SESSION_ID = "commonSessionId";
	private static final String PROG_SEARCH_LIST = "progSearchList";

	private static final String START_PAGE = "startPage";
	private static final String FULL_SCREEN = "fullScreen";

	public static final String USE_SYOBOI_SERVER = "useSyoboiServer";
	private static final String SYOBOI_TOKEN = "syoboiToken";

	//private static final String USE_VIDEO_VIEW = "useVideoView";	// 廃止
	private static final String PLAYER = "player";

	private static SharedPreferences sPrefs;

	public static SharedPreferences getInstance() {
		return sPrefs;
	}

	public static void init(Context context) {
		sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * 認証済み?
	 * @return
	 */
	public static boolean isAuthorized() {
		return !TextUtils.isEmpty(getUserId()) && !TextUtils.isEmpty(getPassword());
	}

	public static boolean isEmptyIpAdr() {
		return TextUtils.isEmpty(Prefs.getIpAdr()) || TextUtils.isEmpty(Prefs.getPort());
	}

	public static String getUserId() {
		return sPrefs.getString(USER, "");
	}

	public static String getPassword() {
		return sPrefs.getString(PASSWORD, "");
	}

	public static void setUser(String userId, String pass) {
		Editor editor = sPrefs.edit();

		editor.putString(USER, userId)
		.putString(PASSWORD, pass);

		for (String key2: new String [] {
				IP_ADDR, P_IP_ADDR, G_IP_ADDR, PORT, TS_PORT,
				GARAPON_AUTH, GTV_SESSION_ID }) {
			editor.remove(key2);
		}

		editor.commit();
	}

	private static String getIpAdr() {
		return sPrefs.getString(IP_ADDR, "");
	}

	private static String getPort() {
		return sPrefs.getString(PORT, "80");
	}

	private static String getTsPort() {
		return sPrefs.getString(TS_PORT, "");
	}

	private static boolean isGlobalAccess() {
		return TextUtils.equals(getIpAdr(), sPrefs.getString(G_IP_ADDR, ""));
	}

	public static String getGaraponHost() {
		if (isGlobalAccess()) {
			return getIpAdr() + ":" + getPort();
		} else {
			return getIpAdr();
		}
	}

	public static String getGaraponTsHost() {
		if (isGlobalAccess()) {
			return getIpAdr() + ":" + getTsPort();
		} else {
			return getIpAdr();
		}
	}

	public static void setAuthResult(HashMap<String,String> result) {
		sPrefs.edit()
		.putString(IP_ADDR, result.get("ipaddr"))
		.putString(PORT, result.get("port"))
		.putString(P_IP_ADDR, result.get("pipaddr"))
		.putString(G_IP_ADDR, result.get("gipaddr"))
		.putString(TS_PORT, result.get("port2"))
		.putString(GTV_VER, result.get("gtvver"))
		.commit();
	}

	public static String getGtvVer() {
		return sPrefs.getString(GTV_VER, "");
	}

	public static String getBaseUrl() {
		return "http://" + sPrefs.getString(IP_ADDR, "");
	}

	public static String getCommonSessionId() {
		return sPrefs.getString(COMMON_SESSION_ID, "");
	}

	public static String getGaraponAuth() {
		return sPrefs.getString(GARAPON_AUTH, "");
	}

	public static String getGtvSessionId() {
		return sPrefs.getString(GTV_SESSION_ID, "");
	}

	public static void setGaraponAuth(String id) {
		sPrefs.edit()
		.putString(GARAPON_AUTH, id)
		.putString(COMMON_SESSION_ID, id)
		.commit();
	}

	public static void setGtvSessionId(String id) {
		sPrefs.edit()
		.putString(GTV_SESSION_ID, id)
		.putString(COMMON_SESSION_ID, id)
		.commit();
	}

	public static void setSearch(ProgSearchList ps) {
		sPrefs.edit()
		.putString(PROG_SEARCH_LIST, ps.toJson())
		.commit();
	}

	public static String getProgSearchList() {
		return sPrefs.getString(PROG_SEARCH_LIST, "");
	}

	public static int getPlayerId() {
		try {
			String value = sPrefs.getString(PLAYER, null);
			int id = Integer.valueOf(value);
			return id;
		} catch (Exception e) {
			return App.PLAYER_WEBVIEW;
		}
	}

	public static int getStartPage(int defaultValue) {
		return sPrefs.getInt(START_PAGE, defaultValue);
	}

	public static void setStartPage(int page) {
		if (getStartPage(-1) != page) {
			sPrefs.edit().putInt(START_PAGE, page).commit();
		}
	}

	public static void setSyoboiToken(String token) {
		sPrefs.edit()
		.putBoolean(USE_SYOBOI_SERVER, true)
		.putString(SYOBOI_TOKEN, token)
		.commit();
	}

	public static String getSyoboiToken() {
		if (sPrefs.getBoolean(USE_SYOBOI_SERVER, false)) {
			return sPrefs.getString(SYOBOI_TOKEN, "");
		}
		return "";
	}

	public static boolean isFullScreen() {
		return sPrefs.getBoolean(FULL_SCREEN, true);
	}

	public static void logout() {
		setUser("", "");
	}
}
