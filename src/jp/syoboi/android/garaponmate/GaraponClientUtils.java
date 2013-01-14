package jp.syoboi.android.garaponmate;

import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import jp.syoboi.android.garaponmate.GaraponClient.GaraponClientException;
import jp.syoboi.android.garaponmate.GaraponClient.Program;
import jp.syoboi.android.garaponmate.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.GaraponClient.SearchTime;
import jp.syoboi.android.garaponmate.data.ProgSearchList.ProgSearchListMatcher;

import org.json.JSONException;

public class GaraponClientUtils {

	private static final String TAG = "GaraponClientUtils";

	private static boolean REFRESH_AUTH;

	public static void setRefreshAuth() {
		REFRESH_AUTH = true;
	}

	public static HashMap<String,String> loginWeb()
			throws MalformedURLException, NoSuchAlgorithmException, GaraponClientException, IOException, JSONException, URISyntaxException {

		SharedPreferences prefs = Prefs.getInstance();

		// user, pass 設定を取得
		String user = prefs.getString(Prefs.USER, null);
		String pass = prefs.getString(Prefs.PASSWORD, null);

		String host = ensureAuth();

		// 自分のガラポンWebにログインしてcookieを取得
		HashMap<String,String> cookies = GaraponClient.loginWeb(host, user, pass);

		Prefs.setGaraponAuth(cookies.get("GaraponAuth"));

		return cookies;
	}

	public static void login() throws NoSuchAlgorithmException, IOException, JSONException, NotFoundException, GaraponClientException {
		SharedPreferences prefs = Prefs.getInstance();

		// user, pass 設定を取得
		String user = prefs.getString(Prefs.USER, null);
		String pass = prefs.getString(Prefs.PASSWORD, null);

		String host = ensureAuth();

		String gtvsessionid = GaraponClient.login(host, user, pass);

		Prefs.setGtvSessionId(gtvsessionid);
	}

	static String ensureAuth() throws MalformedURLException, NoSuchAlgorithmException, NotFoundException, IOException, GaraponClientException {
		SharedPreferences prefs = Prefs.getInstance();
		String user = prefs.getString(Prefs.USER, null);
		String pass = prefs.getString(Prefs.PASSWORD, null);

		if (REFRESH_AUTH || TextUtils.isEmpty(Prefs.getIpAdr()) || TextUtils.isEmpty(Prefs.getPort())) {
			return auth(user, pass);
		}
		else {
			return Prefs.getGaraponHost();
		}
	}

	static String auth(String user, String pass)
			throws MalformedURLException, NoSuchAlgorithmException, IOException, NotFoundException, GaraponClientException {

		SharedPreferences prefs = Prefs.getInstance();

		HashMap<String, String> result = GaraponClient.auth(user, pass);

		String ipaddr = result.get("ipaddr");
		String port = result.get("port");

		// 取得した情報を保存
		prefs.edit()
		.putString(Prefs.IP_ADDR, ipaddr)
		.putString(Prefs.P_IP_ADDR, result.get("pipaddr"))
		.putString(Prefs.G_IP_ADDR, result.get("gipaddr"))
		.putString(Prefs.PORT, port)
		.putString(Prefs.TS_PORT, result.get("port2"))
		.commit();

		return ipaddr + ":" + port;
	}

	/**
	 * 放送中
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws GaraponClientException
	 * @throws JSONException
	 * @throws NotFoundException
	 * @throws NoSuchAlgorithmException
	 */
	public static SearchResult searchNowBroadcasting() throws MalformedURLException, IOException, NoSuchAlgorithmException, NotFoundException, JSONException, GaraponClientException {

		ensureAuth();

		SearchResult sr = searchNowBroadcastingInternal();
		if (sr.status == 0) {
			login();
			sr = searchNowBroadcastingInternal();
		}

		long now = System.currentTimeMillis();

		for (int j=sr.program.size()-1; j>=0; j--) {
			Program p = sr.program.get(j);
			if (p.startdate > now || p.startdate + p.duration <= now) {
				sr.program.remove(p);
			}
		}
		return sr;
	}

	public static SearchResult searchNowBroadcastingInternal() throws MalformedURLException, IOException {
		long now = System.currentTimeMillis();
		SearchResult sr = GaraponClient.search(Prefs.getGaraponHost(),
				Prefs.getGtvSessionId(),
				50, 1,
				null, null,
				null, null, null, 0,
				SearchTime.END, now, now + 6 * DateUtils.HOUR_IN_MILLIS,
				null, true, true);
		return sr;
	}

	public static SearchResult searchFavorite(ProgSearchListMatcher m, int count) throws MalformedURLException, IOException {

		final SearchResult result = new SearchResult();
		result.program = new ArrayList<Program>();

		final ArrayList<Program> progs = result.program;

		int total = 0;
		int hit = -1;

		long now = System.currentTimeMillis();
		long limit = now - 1 * DateUtils.DAY_IN_MILLIS;
		boolean stop = false;

		for (int j=0; j<10; j++) {
			Log.v(TAG, "searchFavorite page:" + j);
			SearchResult sr = GaraponClient.search(Prefs.getGaraponHost(),
					Prefs.getGtvSessionId(),
					100, (j + 1),
					null, null,
					null, null, null, 0,
					SearchTime.START, limit, 0,
					null, false, true);

			if (hit == -1) {
				hit = sr.hit;
				result.hit = sr.hit;
				result.status = sr.status;
			}
			total += sr.program.size();

			for (Program p: sr.program) {
				if (m.match(p)) {
					Log.v(TAG, p.toString());
					progs.add(p);
				} else {
					Log.d(TAG, p.toString());
				}
				if (p.startdate < limit) {
					stop = true;
				}
			}
			if (stop) {
				break;
			}

			if (progs.size() > count) {
				break;
			}

			if (total >= hit || sr.program.size() == 0) {
				break;
			}
		}

		return result;
	}

}
