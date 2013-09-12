package jp.syoboi.android.garaponmate.client;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.ProgramList;
import jp.syoboi.android.garaponmate.utils.Utils;
import jp.syoboi.android.util.JksnUtils;
import jp.syoboi.android.util.JksnUtils.JksnArrayCallback;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.json.JSONException;

public class GaraponClient {
	private static final String TAG = "GaraponClient";

	public static final String ENCODING = "utf-8";

	public static final String AUTH_URL = "http://garagw.garapon.info/getgtvaddress";

	public static final String WEB_LOGIN_PATH = "/";

	public static final String LOGIN_PATH = "auth";
	public static final String SEARCH_PATH = "search";
	public static final String FAVORITE_PATH = "favorite";

	private static final String API_BASE_V2 = "/gapi/v2/";
	private static final String API_BASE_V3 = "/gapi/v3/";
	public static String API_BASE = API_BASE_V2;

	public static final int LOGIN_SUCCESS = 1;


	public static final int STATUS_INVALID_SESSION = 0;	//
	public static final int STATUS_SUCCESS = 1;
	public static final int STATUS_INVALID_PARAMETER = 100;
	public static final int STATUS_MP4_NOT_FOUND = 150;
	public static final int STATUS_DB_CONNECTION_FAILED = 200;

	private static Resources sResources;
	private static String DEV_ID = null;

	public static void init(Context context) {
		sResources = context.getApplicationContext().getResources();

		PackageInfo packageInfo;
		try {
			packageInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(),
					PackageManager.GET_META_DATA);
			DEV_ID = packageInfo.applicationInfo.metaData.getString("garapon_dev_id");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static void setVersion(String gtvver) {
		if (TextUtils.equals(gtvver, "GTV3.0")) {
			API_BASE = API_BASE_V3;
		} else {
			API_BASE = API_BASE_V2;
		}
	}

	public static String getRTMPPath(String gtvid) {
		String path;
		if (API_BASE == API_BASE_V3) {
			path = gtvid + "-" + Prefs.getCommonSessionId();
		}
		else {
			path = gtvid.substring(6,8) + "/" + gtvid + ".ts-" + Prefs.getCommonSessionId();
		}
		return path;
	}

	/**
	 * ガラポン認証
	 * ガラポンのipアドレスを取得する
	 * @param id
	 * @param pass
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws NoSuchAlgorithmException
	 * @throws GaraponClientException
	 * @throws NotFoundException
	 */
	public static HashMap<String,String> auth(String id, String pass) throws MalformedURLException, IOException, NoSuchAlgorithmException, NotFoundException, GaraponClientException {
		if (App.DEBUG) {
			Log.i(TAG, "ガラポンTV認証");
		}

		if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pass)) {
			throw new GaraponClientException(sResources.getString(R.string.settingsWarning));
		}

		HttpURLConnection con = openConnection(AUTH_URL);
		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			String query = newUriBuilder()
			.appendQueryParameter("user", id)
			.appendQueryParameter("md5passwd", md5(pass))
			.build().getEncodedQuery();

			con.getOutputStream().write(query.getBytes());
			con.connect();

			HashMap<String, String> map = parseAuthResult(con.getInputStream());

			if (map.containsKey("1")) {
				throw new GaraponAuthException(map.get("1"));
			}

			return map;
		} finally {
			con.disconnect();
		}
	}

	/**
	 * ガラポン認証の結果をparse
	 * @param is
	 * @return
	 * @throws IOException
	 */
	static HashMap<String,String> parseAuthResult(InputStream is) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(is, ENCODING));

			HashMap<String,String> map = new HashMap<String, String>();

			Matcher m = Pattern.compile("(.*?);(.*)").matcher("");

			String line;
			while ((line = br.readLine()) != null) {
				if (m.reset(line).find()) {
					map.put(m.group(1), m.group(2));
				}
			}
			return map;
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}


	public static String login(String host, String id, String pass)
			throws IOException, NoSuchAlgorithmException, JSONException, GaraponClientException {
		if (App.DEBUG) {
			Log.i(TAG, "ガラポンTVログイン");
		}

		HttpURLConnection con = openConnection("http://" + host + API_BASE + LOGIN_PATH);
		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			String query = newUriBuilder()
				.appendQueryParameter("type", "login")
				.appendQueryParameter("loginid", id)
				.appendQueryParameter("md5pswd", md5(pass))
				.build().getEncodedQuery();

			con.getOutputStream().write(query.getBytes());
			con.connect();

			JksnObject jo = (JksnObject) JksnUtils.parseJson(con.getInputStream(), null);

			int status = jo.getInt("status", 0);
			if (status != STATUS_SUCCESS) {
				throw new GaraponClientException(getApiStatusMessage(status));
			}

			int login = jo.getInt("login");
			if (login != LOGIN_SUCCESS) {
				throw new GaraponClientException(getLoginErrorMessage(login));
			}

			String gtvsession = jo.getString("gtvsession");

			return gtvsession;
		} finally {
			con.disconnect();
		}
	}


	public static HashMap<String,String> loginWeb(String host, String id, String pass)
			throws IOException, NoSuchAlgorithmException, JSONException, URISyntaxException {
		if (App.DEBUG) {
			Log.i(TAG, "ガラポンTVログイン(Web)");
		}

		HttpURLConnection con = openConnection("http://" + host + WEB_LOGIN_PATH);
		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			String query = newUriBuilder()
				.appendQueryParameter("LoginID", id)
				.appendQueryParameter("Passwd", pass)
				.build().getEncodedQuery();

			con.getOutputStream().write(query.getBytes());
			con.connect();

			String text = Utils.readStream(con.getInputStream(), ENCODING);

			Map<String,List<String>> fields = con.getHeaderFields();

			HashMap<String, String> cookies = new HashMap<String,String>();
			for (String key: fields.keySet()) {
				if (key != null && key.equalsIgnoreCase("Set-Cookie")) {
					for (String value: fields.get(key)) {
						Matcher m = COOKIE_VALUE_PTN.matcher(value);
						if (m.find()) {
							cookies.put(m.group(1), m.group(2));
						}
//						Log.v(TAG, key +":"+ value);
					}
				}
			}
			return cookies;
		} finally {
			con.disconnect();
		}
	}

	public static SearchResult search(String ipaddr, String sessionId, Search param) throws MalformedURLException, IOException {
		return search(ipaddr, sessionId,
				param.count, param.page, param.searchType, param.keyword, param.gtvid,
				param.genre0, param.genre1, param.ch, param.searchTime, param.sdate, param.edate,
				param.rank,
				param.sort,
				param.video);
	}

	private static Uri.Builder newUriBuilder() {
		Uri.Builder builder = new Uri.Builder();
		builder.appendQueryParameter("dev_id", DEV_ID);
		return builder;
	}

	/**
	 *
	 * @param ipaddr  IPアドレス
	 * @param sessionId	セッションID
	 * @param count 1ページの表示数
	 * @param page ページ数
	 * @param searchType 検索対象
	 * @param keyword キーワード
	 * @param gtvid 1番組の詳細を取得するときのgtvid
	 * @param genre0 ジャンル大分類
	 * @param genre1 ジャンル小分類
	 * @param ch チャンネル番号
	 * @param searchTime 指定した時間を開始時間と終了時間のどちらを対象にするか
	 * @param sdate 時間の始点
	 * @param edate 時間の終点
	 * @param rank allにするとお気に入り
	 * @param sortAscent
	 * @param videoAll
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws GaraponApiException
	 */
	public static SearchResult search(String ipaddr, String sessionId,
			int count, int page, int searchType, String keyword, String gtvid,
			int genre0, int genre1, int ch,
			int searchTime, long sdate, long edate,
			int rank, int sort, int video) throws MalformedURLException, IOException {

		Uri.Builder builder = newUriBuilder();

		if (count != 0) {
			builder.appendQueryParameter("n", String.valueOf(count));
		}
		if (page != 0) {
			builder.appendQueryParameter("p", String.valueOf(page));
		}

		if (keyword != null) {
			builder.appendQueryParameter("key", keyword);
		}
		if (gtvid != null) {
			builder.appendQueryParameter("gtvid", gtvid);

			// v2 の場合は、STYPE_CAPTION を指定しなければ字幕が取得できないが
			// v3 ではSTYPE_EPGでも字幕が取得でき、STYPE_CAPTIONではタイムアウトしてしまう
			if (API_BASE == API_BASE_V3) {
				searchType = Search.STYPE_EPG;
			}
		}
		if (genre0 != Search.GENRE_EMPTY) {
			builder.appendQueryParameter("genre0", String.valueOf(genre0));
			if (genre1 != Search.GENRE_EMPTY) {
				builder.appendQueryParameter("genre1", String.valueOf(genre1));
			}
		}
		if (ch != 0) {
			builder.appendQueryParameter("ch", String.valueOf(ch));
		}
		switch (searchType) {
		case Search.STYPE_CAPTION:
			builder.appendQueryParameter("s", "c");
			break;
		case Search.STYPE_EPG:
		default:
			builder.appendQueryParameter("s", "e");
			break;
		}

		switch (searchTime) {
		case Search.STIME_END:
			builder.appendQueryParameter("dt", "e");
			break;
		case Search.STIME_START:
		default:
			builder.appendQueryParameter("dt", "s");
			break;
		}

		if (sdate != 0) {
			builder.appendQueryParameter("sdate", longToDateTimeStr(sdate));
		}
		if (edate != 0) {
			builder.appendQueryParameter("edate", longToDateTimeStr(edate));
		}

		switch (rank) {
		case Search.RANK_FAVORITE:
			builder.appendQueryParameter("rank", "all");
		}

		switch (sort) {
		case Search.SORT_STA:
			builder.appendQueryParameter("sort", "sta");
			break;
		case Search.SORT_STD:
			builder.appendQueryParameter("sort", "std");
			break;
		}

		if (video == Search.VIDEO_ALL) {
			builder.appendQueryParameter("video", "all");
		}

		String query = builder.build().getEncodedQuery();
		if (App.DEBUG) {
			Log.d(TAG, "検索 " + builder.build().getQuery());
		}

		HttpURLConnection con = openConnection(
				"http://" + ipaddr + API_BASE + SEARCH_PATH
				+ "?gtvsession=" + sessionId);

		CountInputStream fis = null;
		try {
			long start = System.currentTimeMillis();
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			con.getOutputStream().write(query.getBytes());
			con.connect();

			fis = new CountInputStream(con.getInputStream());

			SearchResult result = parseSearchResult(fis);
			if (App.DEBUG) {
				long end = System.currentTimeMillis();
				Log.d(TAG, "検索結果 status:" + result.status + " hit:" + result.program.size()
						+ " time:" + (end - start) + " size:" + fis.mCount);
			}

			return result;
		} finally {
			if (fis != null) {
				fis.close();
			}
			con.disconnect();
		}

	}

	public static class CountInputStream extends FilterInputStream {

		int mCount;
		protected CountInputStream(InputStream in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			int ret = super.read();
			if (ret >= 0) {
				mCount++;
			}
			return ret;
		}

		@Override
		public int read(byte[] buffer) throws IOException {
			int size = super.read(buffer);
			if (size > 0) {
				mCount += size;
			}
			return size;
		}

		@Override
		public int read(byte[] buffer, int offset, int count)
				throws IOException {
			int size = super.read(buffer, offset, count);
			if (size > 0) {
				mCount += size;
			}
			return size;
		}

	}

	public static JksnObject parseResult(InputStream is) throws IOException {
		try {
			return (JksnObject) JksnUtils.parseJson(is, null);
		} finally {
			is.close();
		}
	}

	public static SearchResult parseSearchResult(InputStream is) throws JsonParseException, IOException {
		try {
			final HashMap<Integer,Ch>	chMap = new HashMap<Integer,Ch>();
			final ProgramList programs = new ProgramList();

			JksnObject jo = (JksnObject) JksnUtils.parseJson(is, new JksnArrayCallback() {

				@Override
				public boolean onTargetObject(JsonParser jp, JksnObject j) {
					try {
						programs.add(new Program(j, chMap));
					} catch (Exception e) {
						e.printStackTrace();
					}
					return true;
				}

				@Override
				public boolean isTargetArray(JsonParser jp, String name) {
					return name.equals("program");
				}
			});

			SearchResult sr = new SearchResult(jo);

			sr.hit = Integer.valueOf(jo.getString("hit", "-1"), 10);
			sr.program = programs;
			sr.ch = chMap;

			return sr;
		} finally {
			is.close();
		}
	}

	public static ApiResult favorite(String ipaddr, String sessionId, String gtvid, boolean favorite) throws MalformedURLException, IOException {
		Uri.Builder builder = newUriBuilder();

		builder.appendQueryParameter("gtvid", gtvid);
		builder.appendQueryParameter("rank", (favorite ? "1" : "0"));

		String query = builder.build().getEncodedQuery();
		if (App.DEBUG) {
			Log.i(TAG, "favorite favorite:" + favorite);
		}

		HttpURLConnection con = openConnection(
				"http://" + ipaddr + API_BASE + FAVORITE_PATH
				+ "?gtvsession=" + sessionId);

		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			con.getOutputStream().write(query.getBytes());
			con.connect();

			JksnObject jo = parseResult(con.getInputStream());
			ApiResult result = new ApiResult(jo);

			return result;
		} finally {
			con.disconnect();
		}
	}

	static final Time TMP_TIME = new Time();

	public static synchronized String longToDateTimeStr(long d) {
		synchronized (TMP_TIME) {
			Time t = TMP_TIME;
			t.set(d);
			return t.format("%Y-%m-%d %H:%M:%S");
	//		return String.format(
	//				"%04d-%02d-%02d %02D:%02d:%02d",
	//				t.year, t.month, t.monthDay,
	//				t.hour, t.minute, t.second);
		}
	}

	public static long parseDateTimeStr(String str) {
		synchronized (TMP_TIME) {
			Time t = TMP_TIME;
			t.year = Integer.valueOf(str.substring(0, 4), 10);
			t.month = Integer.valueOf(str.substring(5, 7), 10) - 1;
			t.monthDay = Integer.valueOf(str.substring(8, 10), 10);
			t.hour = Integer.valueOf(str.substring(11, 13), 10);
			t.minute = Integer.valueOf(str.substring(14, 16), 10);
			t.second = Integer.valueOf(str.substring(17, 19), 10);
			return t.toMillis(true);
		}
	}

	public static long parseTimeStr(String str) {
		int hourEnd = str.indexOf(':');
		int hour = Integer.valueOf(str.substring(0, hourEnd), 10);
		int minute = Integer.valueOf(str.substring(3, hourEnd + 3), 10);
		int second = Integer.valueOf(str.substring(6, hourEnd + 6), 10);
		return hour * DateUtils.HOUR_IN_MILLIS
				+ minute * DateUtils.MINUTE_IN_MILLIS
				+ second * DateUtils.SECOND_IN_MILLIS;
	}


	static Pattern COOKIE_VALUE_PTN = Pattern.compile("([^=]+)=([^;]+)");


	static String md5(String text) throws NoSuchAlgorithmException {

		MessageDigest md = MessageDigest.getInstance("md5");
		md.update(text.getBytes());

		StringBuilder sb = new StringBuilder();
		for (byte b: md.digest()) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static enum Rank {
		FAVORITE,
	}

	public static enum SearchType {
		EPG, SUBTITLE
	}

	public static enum SearchTime {
		START, END
	}

	public static class ApiResult implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -9216262125964499824L;
		public int status;
		public String version;

		public ApiResult(JksnObject j) {
			status = j.getInt("status", 0);
			version = j.getString("version", null);
		}
	}

	public static class SearchResult extends ApiResult implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -2537372780467862014L;
		public int hit;
		public ProgramList program;
		public HashMap<Integer,Ch> ch;
		public long timestamp;
		public SearchResult(JksnObject j) {
			super(j);
		}
	}

	/**
	 * チャンネル
	 */
	public static class Ch implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -5970947698044309849L;

		public int ch;
		public String bc;
		public String bc_tags;
		public Ch(int ch, String bc, String bc_tags) {
			this.ch = ch;
			this.bc = bc;
			this.bc_tags = bc_tags;
		}

		public Ch(JksnObject jo) {
			ch = jo.getInt("ch");
			bc = jo.getString("bc");
			bc_tags = jo.getString("bc_tags");
		}

		public void write(JsonGenerator jg) throws JsonGenerationException, IOException {
			jg.writeStartObject();
			jg.writeNumberField("ch", ch);
			jg.writeStringField("bc", bc);
			jg.writeStringField("bc_tags", bc_tags);
			jg.writeEndObject();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Ch) {
				Ch x = (Ch)o;
				return ch == x.ch
						&& TextUtils.equals(bc, x.bc)
						&& TextUtils.equals(bc_tags, x.bc_tags);
			}
			return super.equals(o);
		}

		@Override
		public String toString() {
			if (ch == 0) {
				return bc;
			}
			return bc + " (" + ch + ")";
		}
	}

	public static String getM3uUrl(String host, String gtvid, String sessionId) {
		return "http://" + host
				+ "/cgi-bin/play/m3u8.cgi?"
				+ gtvid
				+ "-" + sessionId;
	}

	static HttpURLConnection openConnection(String url) throws MalformedURLException, IOException {
		HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
		con.setConnectTimeout(10*1000);
		con.setReadTimeout(10*1000);
		return con;
	}

	public static class GaraponClientException extends Exception {
		private static final long serialVersionUID = 1L;
		public GaraponClientException() {

		}
		public GaraponClientException(String msg) {
			super(msg);
		}
	}

	/**
	 * 認証エラー例外
	 */
	public static class GaraponAuthException extends GaraponClientException {
		private static final HashMap<String,Integer> sErrorMap;

		static {
			sErrorMap = new HashMap<String,Integer>();
			sErrorMap.put("wrong password", R.string.wrongPassword);
			sErrorMap.put("unknown user", R.string.unknownUser);
			sErrorMap.put("unknown registkey", R.string.unknownRegistkey);
			sErrorMap.put("unknown ip address", R.string.unknownIpAddress);
		}

		private static final long serialVersionUID = 1L;

		private String mErrorMessage;

		public GaraponAuthException(String serverMsg) {
			mErrorMessage = serverMsg;
			if (sErrorMap.containsKey(serverMsg)) {
				mErrorMessage = sResources.getString(sErrorMap.get(serverMsg));
			}
		}

		@Override
		public String getMessage() {
			return mErrorMessage;
		}
	}

	public static String getApiStatusMessage(int status) {
		switch (status) {
		case 100:
			return sResources.getString(R.string.invalidParameter);
		case 200:
			return sResources.getString(R.string.authSyncError);
		default:
			return "ERROR status: " + status;
		}
	}

	public static String getLoginErrorMessage(int login) {
		switch (login) {
		case 100:
			return sResources.getString(R.string.unknownUser);
		case 200:
			return sResources.getString(R.string.wrongPassword);
		default:
			return "ERROR login: " + login;
		}
	}

	public static class Search implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -4075022043665770912L;

		public static final int GENRE_EMPTY = -1;

		public static final int RANK_FAVORITE = 1;

		public static final int STYPE_EPG = 0;
		public static final int STYPE_CAPTION = 1;

		public static final int STIME_START = 0;
		public static final int STIME_END = 1;

		public static final int SORT_STD = 0;
		public static final int SORT_STA = 1;

		public static final int VIDEO_ALL = 1;

		public int count;
		public int page;

		public int searchType;
		public String keyword;
		public String gtvid;
		public int genre0 = GENRE_EMPTY;
		public int genre1 = GENRE_EMPTY;
		public int ch;
		public int searchTime;
		public long sdate;
		public long edate;
		public int rank;
		public int sort = SORT_STD;
		public int video = VIDEO_ALL;
	}
}
