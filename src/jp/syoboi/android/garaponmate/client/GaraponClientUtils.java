package jp.syoboi.android.garaponmate.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClient.Ch;
import jp.syoboi.android.garaponmate.client.GaraponClient.GaraponClientException;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.data.Genre;
import jp.syoboi.android.garaponmate.data.GenreGroup;
import jp.syoboi.android.garaponmate.data.GenreGroupList;
import jp.syoboi.android.garaponmate.data.Program;

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

		SearchParam param = new SearchParam();
		long now = System.currentTimeMillis();
		param.count = 50;
		param.searchTime = SearchParam.STIME_END;
		param.sdate = now;
		param.edate = now + 6 * DateUtils.HOUR_IN_MILLIS;
		param.sort = SearchParam.SORT_STA;
		param.video = SearchParam.VIDEO_ALL;

		String host = Prefs.getGaraponHost();
		String sessionId = Prefs.getGtvSessionId();

		SearchResult sr = GaraponClient.search(host, sessionId, param);
		if (sr.status == 0) {
			login();
			sr = GaraponClient.search(host, sessionId, param);
		}

		for (int j=sr.program.size()-1; j>=0; j--) {
			Program p = sr.program.get(j);
			if (p.startdate > now || p.startdate + p.duration <= now) {
				sr.program.remove(p);
			}
		}
		return sr;
	}

//	public static SearchResult searchNowBroadcastingInternal(SearchParam p) throws MalformedURLException, IOException {
//		long now = System.currentTimeMillis();
//		SearchResult sr = GaraponClient.search(Prefs.getGaraponHost(),
//				Prefs.getGtvSessionId(),
//				50, 1,
//				null, null,
//				null, null, null, 0,
//				SearchTime.END, now, now + 6 * DateUtils.HOUR_IN_MILLIS,
//				null, true, true);
//		return sr;
//	}


	public static SearchResult search(SearchParam param) throws MalformedURLException, IOException, NoSuchAlgorithmException, NotFoundException, JSONException, GaraponClientException {

		ensureAuth();

		SearchResult sr = GaraponClient.search(Prefs.getGaraponHost(),
				Prefs.getGtvSessionId(), param);
		if (sr.status == 0) {
			login();
			sr = GaraponClient.search(Prefs.getGaraponHost(),
					Prefs.getGtvSessionId(), param);
		}

		return sr;
	}

	public static String formatSearchParam(Context context, SearchParam p) {

		final String separator = ", ";
		StringBuilder sb = new StringBuilder();
		if (!TextUtils.isEmpty(p.keyword)) {
			sb.append(separator)
			.append(context.getString(R.string.keyword))
			.append(":").append(p.keyword);
		}
		if (p.ch != 0) {
			Ch chInfo = App.getChList().getCh(p.ch);

			sb.append(separator)
			.append(context.getString(R.string.ch))
			.append(":").append(chInfo != null ? chInfo.bc : String.valueOf(p.ch));
		}
		if (p.genre0 != SearchParam.GENRE_EMPTY) {
			GenreGroupList ggl = GenreGroupList.getInstance(context);
			GenreGroup gg = ggl.findByValue(p.genre0);
			if (gg != null) {
				sb.append(separator)
				.append(context.getString(R.string.genre))
				.append(":").append(gg.name);
				if (p.genre1 != SearchParam.GENRE_EMPTY) {
					Genre g = gg.findByValue(p.genre1);
					if (g != null) {
						sb.append("/")
						.append(g.name);
					}
				}
			}
		}
		if (p.rank == SearchParam.RANK_FAVORITE) {
			sb.append(separator)
			.append(context.getString(R.string.favoriteOnly));
		}
		if (sb.length() > 0) {
			return sb.substring(separator.length());
		}
		return "Empty";
	}
}
