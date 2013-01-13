package jp.syoboi.android.garaponmate;

import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import jp.syoboi.android.garaponmate.GaraponClient.GaraponClientException;

import org.json.JSONException;

public class GaraponClientUtils {
	public static HashMap<String,String> login(SharedPreferences prefs)
			throws MalformedURLException, NoSuchAlgorithmException, GaraponClientException, IOException, JSONException, URISyntaxException {

		// user, pass 設定を取得
		String user = prefs.getString(Prefs.USER, null);
		String pass = prefs.getString(Prefs.PASSWORD, null);

		// 前回使用した ipaddr
		String ipaddr = prefs.getString(Prefs.IP_ADDR, null);

		// ipaddr が保存されていなければガラポンに問い合わせる
//		if (TextUtils.isEmpty(ipaddr)) {
			ipaddr = auth(prefs, user, pass);
//		}

		// 自分のガラポンにログインしてcookieを取得

		HashMap<String,String> cookies = GaraponClient.loginWeb(ipaddr, user, pass);

		String session = cookies.get("GaraponAuth");
		prefs.edit().putString(Prefs.GTV_SESSION_ID, session).commit();

		return cookies;
	}

	private static String auth(SharedPreferences prefs, String user, String pass)
			throws MalformedURLException, NoSuchAlgorithmException, IOException, NotFoundException, GaraponClientException {

		HashMap<String, String> result = GaraponClient.auth(user, pass);

		String ipaddr = result.get("ipaddr");

		// 取得した情報を保存
		prefs.edit()
		.putString(Prefs.IP_ADDR, ipaddr)
		.putString(Prefs.P_IP_ADDR, result.get("pipaddr"))
		.putString(Prefs.G_IP_ADDR, result.get("gipaddr"))
		.putString(Prefs.PORT, result.get("port"))
		.putString(Prefs.TS_PORT, result.get("port2"))
		.commit();

		return ipaddr;
	}
}
