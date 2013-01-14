package jp.syoboi.android.garaponmate;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.util.JksnUtils;
import jp.syoboi.android.util.JksnUtils.JksnArray;
import jp.syoboi.android.util.JksnUtils.JksnArrayCallback;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.json.JSONException;

public class GaraponClient {
	private static final String TAG = "GaraponClient";

	public static final String ENCODING = "utf-8";

	public static final String AUTH_URL = "http://garagw.garapon.info/getgtvaddress";

	public static final String WEB_LOGIN_PATH = "/";
	public static final String LOGIN_PATH = "/gapi/v2/auth";
	public static final String SEARCH_PATH = "/gapi/v2/search";

	public static final int STATUS_SUCCESS = 1;
	public static final int LOGIN_SUCCESS = 1;

	private static Resources sResources;

	public static void init(Context context) {
		sResources = context.getApplicationContext().getResources();
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

		HttpURLConnection con = (HttpURLConnection)new URL(AUTH_URL).openConnection();
		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			String query = new Uri.Builder()
			.appendQueryParameter("user", id)
			.appendQueryParameter("md5passwd", md5(pass))
			.build().getQuery();

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

		HttpURLConnection con = (HttpURLConnection)new URL("http://" + host + LOGIN_PATH).openConnection();
		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			String query = new Uri.Builder()
				.appendQueryParameter("type", "login")
				.appendQueryParameter("loginid", id)
				.appendQueryParameter("md5pswd", md5(pass))
				.build().getQuery();

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

		HttpURLConnection con = (HttpURLConnection)new URL("http://" + host + WEB_LOGIN_PATH)
			.openConnection();
		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			String query = new Uri.Builder()
				.appendQueryParameter("LoginID", id)
				.appendQueryParameter("Passwd", pass)
				.build().getQuery();

			con.getOutputStream().write(query.getBytes());
			con.connect();

			String text = readStream(con.getInputStream(), ENCODING);

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
	 */
	public static SearchResult search(String ipaddr, String sessionId,
			int count, int page, SearchType searchType, String keyword, String gtvid,
			Integer genre0, Integer genre1, int ch, SearchTime searchTime, long sdate, long edate,
			Rank rank, boolean sortAscent, boolean videoAll) throws MalformedURLException, IOException {

		Uri.Builder builder = new Uri.Builder()
		.appendQueryParameter("gtvsession", sessionId)
		.appendQueryParameter("n", String.valueOf(count))
		.appendQueryParameter("p", String.valueOf(page));

		if (keyword != null) {
			builder.appendQueryParameter("key", keyword);
		}
		if (gtvid != null) {
			builder.appendQueryParameter("gtvid", gtvid);
		}
		if (genre0 != null) {
			builder.appendQueryParameter("genre0", String.valueOf(genre0));
			if (genre1 != null) {
				builder.appendQueryParameter("genre1", String.valueOf(genre1));
			}
		}
		if (ch != 0) {
			builder.appendQueryParameter("ch", String.valueOf(ch));
		}
		if (searchType != null) {
			switch (searchType) {
			case EPG:
				builder.appendQueryParameter("s", "e");
				break;
			case SUBTITLE:
				builder.appendQueryParameter("s", "c");
				break;
			}
		}
		if (searchTime != null) {
			switch (searchTime) {
			case START:
				builder.appendQueryParameter("dt", "s");
				break;
			case END:
				builder.appendQueryParameter("dt", "e");
				break;
			}
		}
		if (sdate != 0) {
			builder.appendQueryParameter("sdate", longToDateTimeStr(sdate));
		}
		if (edate != 0) {
			builder.appendQueryParameter("edate", longToDateTimeStr(edate));
		}
		if (rank != null) {
			switch (rank) {
			case FAVORITE:
				builder.appendQueryParameter("rank", "all");
			}
		}
		if (sortAscent) {
			builder.appendQueryParameter("sort", sortAscent ? "sta" : "std");
		}
		if (videoAll) {
			builder.appendQueryParameter("video", "all");
		}

//		builder.clearQuery();
//		builder//.appendQueryParameter("gtvsession", sessionId)
//		.appendQueryParameter("sdate", "2013-01-12 22:13:00")
//		.appendQueryParameter("edate", "2013-01-12 23:11:00")
//		.appendQueryParameter("dt", "e")
//		.appendQueryParameter("key", "")
//		.appendQueryParameter("video", "all");


		String query = builder.build().getQuery();
		if (App.DEBUG) {
			Log.i(TAG, "検索 " + query);
		}

		HttpURLConnection con = (HttpURLConnection)new URL("http://" + ipaddr + SEARCH_PATH
				+ "?gtvsession=" + sessionId)
			.openConnection();

		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			con.getOutputStream().write(query.getBytes());
			con.connect();

			SearchResult result = parseSearchResult(con.getInputStream());

			return result;
		} finally {
			con.disconnect();
		}

	}

	public static SearchResult parseSearchResult(InputStream is) throws JsonParseException, IOException {
		try {
			final SparseArray<Ch>	chMap = new SparseArray<Ch>();
			final ArrayList<Program> programs = new ArrayList<Program>();

			JksnObject jo = (JksnObject) JksnUtils.parseJson(is, new JksnArrayCallback() {

				@Override
				public boolean onTargetObject(JsonParser jp, JksnObject j) {
					programs.add(new Program(j, chMap));
					return true;
				}

				@Override
				public boolean isTargetArray(JsonParser jp, String name) {
					return name.equals("program");
				}
			});

			SearchResult sr = new SearchResult();
			sr.status = jo.getInt("status", 0);
			sr.hit = Integer.valueOf(jo.getString("hit", "-1"), 10);
			sr.program = programs;

			return sr;
		} finally {
			is.close();
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
		int hour = Integer.valueOf(str.substring(0, 2), 10);
		int minute = Integer.valueOf(str.substring(3, 5), 10);
		int second = Integer.valueOf(str.substring(6, 8), 10);
		return hour * DateUtils.HOUR_IN_MILLIS
				+ minute * DateUtils.MINUTE_IN_MILLIS
				+ second * DateUtils.SECOND_IN_MILLIS;
	}


	static Pattern COOKIE_VALUE_PTN = Pattern.compile("([^=]+)=([^;]+)");


	static String readStream(InputStream is, String encoding) throws IOException {
		try {
			InputStreamReader isr = new InputStreamReader(is, encoding);
			StringBuilder sb = new StringBuilder();
			char [] buf = new char [8*1024];
			int size;
			while ((size = isr.read(buf)) != -1) {
				sb.append(buf, 0, size);
			}
			return sb.toString();
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

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

	public static class SearchResult {
		public int status;
		public int hit;
		ArrayList<Program> program;
	}

	/**
	 * 番組
	 */
	public static class Program {
		public static final int FLAG_TS = 1;
		public static final int FLAG_TS_ONLY = 2;
		public static final int FLAG_MP4 = 4;
		public static final int FLAG_FAVORITE = 8;

		public final String gtvid;
		public final long startdate;
		public final long duration;
		public final Ch ch;
		public final String title;
		public final String description;
		public final int [] genre;
		public final int flag;

		public Program(JksnObject jo, SparseArray<Ch> chMap) {
			gtvid = jo.getString("gtvid");
			startdate = parseDateTimeStr(jo.getString("startdate"));
			duration = parseTimeStr(jo.getString("duration"));
			title = jo.getString("title");
			description = jo.getString("description");

			JksnArray genreArray = jo.getArray("genre");
			genre = new int [genreArray.size()];
			for (int j=0; j<genre.length; j++) {
				genre[j] = parseGenreStr(genreArray.getString(j));
			}

			int chNum = Integer.parseInt(jo.getString("ch","0"), 10);
			Ch chCache = chMap.get(chNum);
			if (chCache == null) {
				this.ch = new Ch(chNum, jo.getString("bc"), jo.getString("bc_tags"));
				chMap.put(chNum, this.ch);
			} else {
				this.ch = chCache;
			}

			flag = ("1".equals(jo.getString("ts", "0")) ? FLAG_TS : 0)
					| ("1".equals(jo.getString("tsonly", "0")) ? FLAG_TS_ONLY : 0)
					| ("1".equals(jo.getString("mp4", "0")) ? FLAG_MP4 : 0)
					| ("1".equals(jo.getString("favorite", "0")) ? FLAG_FAVORITE : 0);
		}

		public static int parseGenreStr(String text) {
			int pos = text.indexOf('/');
			if (pos == -1) {
				return 0;
			}
			int genre0 = Integer.valueOf(text.substring(0,  pos), 10);
			int genre1 = Integer.valueOf(text.substring(pos+1), 10);
			return genre0 << 16 | genre1;
		}
	}

	/**
	 * チャンネル
	 */
	public static class Ch {
		public int ch;
		public String bc;
		public String bc_tags;
		public Ch(int ch, String bc, String bc_tags) {
			this.ch = ch;
			this.bc = bc;
			this.bc_tags = bc_tags;
		}
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
}
