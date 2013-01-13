package jp.syoboi.android.garaponmate;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class GaraponClient {
	private static final String TAG = "GaraponClient";

	public static final String ENCODING = "utf-8";

	public static final String AUTH_URL = "http://garagw.garapon.info/getgtvaddress";

	public static final String LOGIN_PATH = "/gapi/v2/auth";
	public static final String SEARCH_PATH = "/gapi/v2/search";

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


	public static JSONObject login(String host, String id, String pass)
			throws IOException, NoSuchAlgorithmException, JSONException {

		HttpURLConnection con = (HttpURLConnection)getURL(host, LOGIN_PATH).openConnection();
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

			Log.d(TAG, "query:" + query);
			String text = readStream(con.getInputStream(), ENCODING);
			Log.v(TAG, "result: " + text);

			JSONObject jo = new JSONObject(text);

			return jo;
		} finally {
			con.disconnect();
		}
//		JsonReader jr = new JsonReader(new InputStreamReader(is, ENCODING));
//		jr.ha
	}

	public static HashMap<String,String> loginWeb(String host, String id, String pass)
			throws IOException, NoSuchAlgorithmException, JSONException, URISyntaxException {

		HttpURLConnection con = (HttpURLConnection)getURL(host, "/").openConnection();
		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");

			String query = new Uri.Builder()
				.appendQueryParameter("LoginID", id)
				.appendQueryParameter("Passwd", pass)
				.build().getQuery();

			con.getOutputStream().write(query.getBytes());
			con.connect();

			Log.d(TAG, "query:" + query);
			String text = readStream(con.getInputStream(), ENCODING);
			Log.v(TAG, "result: " + text);

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
//		JsonReader jr = new JsonReader(new InputStreamReader(is, ENCODING));
//		jr.ha
	}

	static Pattern COOKIE_VALUE_PTN = Pattern.compile("([^=]+)=([^;]+)");

	private static URL getURL(String host, String path) throws MalformedURLException {
		return new URL("http", host, path);
	}


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

	public static class GaraponClientException extends Exception {
		private static final long serialVersionUID = 1L;
		public GaraponClientException() {

		}
		public GaraponClientException(String msg) {
			super(msg);
		}
	}

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

}
