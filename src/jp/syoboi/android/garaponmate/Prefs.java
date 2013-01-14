package jp.syoboi.android.garaponmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class Prefs {
	public static final String USER = "user";
	public static final String PASSWORD = "password";

	public static final String IP_ADDR = "ipaddr";
	public static final String P_IP_ADDR = "pipaddr";
	public static final String G_IP_ADDR = "gipaddr";
	public static final String PORT = "port";
	public static final String TS_PORT = "port2";

	private static final String GTV_SESSION_ID = "gtvSessionId";
	private static final String GARAPON_AUTH = "garaponAuth";
	private static final String COMMON_SESSION_ID = "commonSessionId";


	private static SharedPreferences sPrefs;

	private static String sUser;
	private static String sPass;

	public static SharedPreferences getInstance() {
		return sPrefs;
	}

	public static void init(Context context) {
		sPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		sUser = sPrefs.getString(USER, null);
		sPass = sPrefs.getString(PASSWORD, null);

		sPrefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sp,
					String key) {

				if (USER.equals(key) || PASSWORD.equals(key)) {
					String user = sPrefs.getString(USER, null);
					String pass = sPrefs.getString(PASSWORD, null);

					if (!TextUtils.equals(user, sUser) || !TextUtils.equals(pass, sPass)) {
						// user, pass が変更されていたら関連する情報を削除する
						clearSession();
						sUser = user;
						sPass = pass;
					}
				}
			}
		});
	}

	public static void clearSession() {
		Editor editor = sPrefs.edit();
		for (String key2: new String [] {
				IP_ADDR, P_IP_ADDR, G_IP_ADDR, PORT, TS_PORT,
				GARAPON_AUTH, GTV_SESSION_ID }) {
			editor.remove(key2);
		}
		editor.commit();
	}

	public static String getIpAdr() {
		return sPrefs.getString(IP_ADDR, "");
	}

	public static String getPort() {
		return sPrefs.getString(PORT, "80");
	}

	public static String getTsPort() {
		return sPrefs.getString(TS_PORT, "");
	}

	public static String getGaraponHost() {
		return getIpAdr() + ":" + getPort();
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

	public static String getFavTitle(int j) {
		return sPrefs.getString("fav" + j + "title", "");
	}

	public static String getFavUrl(int j) {
		String url = sPrefs.getString("fav" + j + "url", "");
		int pos = url.indexOf("//");
		if (pos != -1) {
			pos = url.indexOf("/", pos + 2);
			if (pos != -1) {
				url = getBaseUrl() + url.substring(pos);
			}
		}
		return url;
	}

	public static void setFav(int j, String title, String url) {
		sPrefs.edit()
		.putString("fav" + j + "title", title)
		.putString("fav" + j + "url", url)
		.commit();
	}
}
