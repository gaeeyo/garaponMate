package jp.syoboi.android.garaponmate.client;

import android.content.Context;
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
import jp.syoboi.android.garaponmate.client.GaraponClient.ApiResult;
import jp.syoboi.android.garaponmate.client.GaraponClient.Ch;
import jp.syoboi.android.garaponmate.client.GaraponClient.GaraponClientException;
import jp.syoboi.android.garaponmate.client.GaraponClient.Search;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.data.Genre;
import jp.syoboi.android.garaponmate.data.GenreGroup;
import jp.syoboi.android.garaponmate.data.GenreGroupList;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.ProgramList;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.data.SearchParam.PostMatcher;
import jp.syoboi.android.garaponmate.utils.Utils;

import org.json.JSONException;

public class GaraponClientUtils {

	private static final String TAG = "GaraponClientUtils";

	private static boolean REFRESH_AUTH;
	private static String sGtvSession;

	public static void setRefreshAuth() {
		REFRESH_AUTH = true;
	}

	public static HashMap<String,String> loginWeb()
			throws MalformedURLException, NoSuchAlgorithmException, GaraponClientException, IOException, JSONException, URISyntaxException {

		ensureAuth();

		// 自分のガラポンWebにログインしてcookieを取得
		HashMap<String,String> cookies = GaraponClient.loginWeb(
				Prefs.getGaraponHost(), Prefs.getUserId(), Prefs.getPassword());

		Prefs.setGaraponAuth(cookies.get("GaraponAuth"));

		return cookies;
	}

	public static void login() throws NoSuchAlgorithmException, IOException, JSONException, NotFoundException, GaraponClientException {

		ensureAuth();

		// login が同時に実行された場合に1度しか認証が実行されないようにする
		synchronized (TAG) {
			if (sGtvSession == null) {
				String gtvsessionid = GaraponClient.login(
						Prefs.getGaraponHost(), Prefs.getUserId(), Prefs.getPassword());
				sGtvSession = gtvsessionid;
				Prefs.setGtvSessionId(gtvsessionid);
			}
		}
	}

	static synchronized void ensureAuth() throws MalformedURLException, NoSuchAlgorithmException, NotFoundException, IOException, GaraponClientException {
		String user = Prefs.getUserId();
		String pass = Prefs.getPassword();

		if (REFRESH_AUTH || Prefs.isEmptyIpAdr()) {
			auth(user, pass);
		}
	}

	public static void auth(String user, String pass)
			throws MalformedURLException, NoSuchAlgorithmException, IOException, NotFoundException, GaraponClientException {

		HashMap<String, String> result = GaraponClient.auth(user, pass);

		// 取得した情報を保存
		Prefs.setAuthResult(result);
		GaraponClient.setVersion(Prefs.getGtvVer());

		REFRESH_AUTH = false;
		sGtvSession = null;
	}

	/**
	 * 放送中
	 * @throws Exception
	 */
	public static SearchResult searchNowBroadcasting() throws Exception {

		SearchParam param = new SearchParam();
		long now = Utils.currentTimeMillisJp();
		param.count = 50;
		param.searchTime = SearchParam.STIME_END;
		param.sdate = now;
		param.edate = now + 6 * DateUtils.HOUR_IN_MILLIS;
		param.sort = SearchParam.SORT_STA;

		SearchResult sr = search(param);

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


	/**
	 * 検索API呼び出し
	 * @param param
	 * @return
	 * @throws Exception
	 */
	public static SearchResult search(final SearchParam param) throws Exception {

		ensureAuth();

		SearchResult result = new ApiCallWrapper<SearchResult>() {
			@Override
			public SearchResult doApiCall() throws Exception {
				return GaraponClient.search(Prefs.getGaraponHost(),
						Prefs.getGtvSessionId(), param);
			}
		}.execute();

		if (result.program != null) {
			ProgramList items = new ProgramList();
			PostMatcher m = param.createPostMatcher();
			for (Program p: result.program) {
				if (m.match(p)) {
					items.add(p);
				}
			}
			result.program = items;
		}

		return result;
	}

	/**
	 * お気に入りにAPI呼び出し
	 * @param gtvid
	 * @param favorite
	 * @return
	 * @throws Exception
	 */
	public static ApiResult favorite(final String gtvid, final boolean favorite) throws Exception {
		ensureAuth();
		ApiResult result = new ApiCallWrapper<ApiResult>() {
			@Override
			public ApiResult doApiCall() throws MalformedURLException, IOException  {
				return GaraponClient.favorite(Prefs.getGaraponHost(),
						Prefs.getGtvSessionId(), gtvid, favorite);
			}
		}.execute();
		return result;
	}

	/**
	 * API呼び出しのラッパー
	 * status をチェックして invalid な場合は再ログインしてAPIを再度呼び出す
	 * @param <T>
	 */
	public static abstract class ApiCallWrapper<T extends ApiResult> {
		public ApiCallWrapper() {
		}

		public T execute() throws Exception {
			T result = doApiCall();
			if (result.status == GaraponClient.STATUS_INVALID_SESSION) {
				sGtvSession = null;
				login();
				result = doApiCall();
			}
			return result;
		}

		public abstract T doApiCall() throws Exception;
	}

	public static String formatSearchParam(Context context, SearchParam p) {

		final String separator = " ";
		StringBuilder sb = new StringBuilder();

		if (!TextUtils.isEmpty(p.keyword)) {
			sb.append(separator)
			.append('"');
			if (p.searchType == Search.STYPE_CAPTION) {
				sb.append(context.getString(R.string.caption))
				.append(':');
			}
			sb.append(p.keyword)
			.append('"');
		}
		if (p.ch != 0) {
			Ch chInfo = App.getChList().getCh(p.ch);

			sb.append(separator)
			.append('[')
			.append(chInfo != null ? chInfo.bc : String.valueOf(p.ch))
			.append(']');
		}
		if (p.genre0 != SearchParam.GENRE_EMPTY) {
			GenreGroupList ggl = GenreGroupList.getInstance(context);
			GenreGroup gg = ggl.findByValue(p.genre0);
			if (gg != null) {
				sb.append(separator)
				.append('(')
				.append(gg.name);
				if (p.genre1 != SearchParam.GENRE_EMPTY) {
					Genre g = gg.findByValue(p.genre1);
					if (g != null) {
						sb.append("/")
						.append(g.name);
					}
				}
				sb.append(')');
			}
		}
		if (p.rank == SearchParam.RANK_FAVORITE) {
			sb.append(separator)
			.append(context.getString(R.string.favoriteOnly));
		}
		if (p.durationMin != 0 && p.durationMax != 0) {
			sb.append(separator)
			.append(context.getString(R.string.durationMinMaxFmt,
					p.durationMin / DateUtils.MINUTE_IN_MILLIS,
					p.durationMax / DateUtils.MINUTE_IN_MILLIS));
		}
		else if (p.durationMin != 0) {
			sb.append(separator)
			.append(context.getString(R.string.durationMinFmt,
					p.durationMin / DateUtils.MINUTE_IN_MILLIS));
		}
		else if (p.durationMax != 0) {
			sb.append(separator)
			.append(context.getString(R.string.durationMaxFmt,
					p.durationMax / DateUtils.MINUTE_IN_MILLIS));
		}


		if (sb.length() > 0) {
			return sb.substring(separator.length());
		}
		return "Empty";
	}

	public static String getM3uUrl(String gtvid) {
		return GaraponClient.getM3uUrl(Prefs.getGaraponHost(),
				gtvid, Prefs.getCommonSessionId());
	}
}
