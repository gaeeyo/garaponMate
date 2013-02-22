package jp.syoboi.android.garaponmate.client;

import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.util.JksnUtils;
import jp.syoboi.android.util.JksnUtils.JksnArrayCallback;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonParser;

public class SyoboiClient {

	private static final String TAG = "SyoboiClient";

	public static final String AUTH_URL = "http://garapon.syoboi.jp:50006/auth.php";

	public static final String SALT = "garapon";
	public static final String API_BASE = "http://garapon.syoboi.jp:50006/";
	public static final String API_PLAY = API_BASE + "play.php";
	public static final String API_HISTORIES = API_BASE + "histories.php";

	public static void sendPlay(Program p, int msec) throws MalformedURLException, IOException {
		int sec = msec / 1000;
		if (App.DEBUG) {
			Log.v(TAG, "sendPlay p:" + p + " time:" + sec);
		}
		Uri.Builder builder = new Uri.Builder();
		builder.appendQueryParameter("gtvid", p.gtvid);
		if (!TextUtils.isEmpty(p.title)) {
			builder.appendQueryParameter("title", p.title);
			builder.appendQueryParameter("start", String.valueOf(p.startdate / 1000));
			builder.appendQueryParameter("end", String.valueOf((p.startdate + p.duration)/1000));
			builder.appendQueryParameter("description", p.description);
			builder.appendQueryParameter("time", String.valueOf(sec));
		}
		builder.appendQueryParameter("token", Prefs.getSyoboiToken());

		HttpURLConnection con = openConnection(API_PLAY);
		try {
			String query = builder.build().getEncodedQuery();

			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.getOutputStream().write(query.getBytes());
			con.connect();

			String result = getContent(con.getInputStream());
//			JksnObject jo = (JksnObject) JksnUtils.parseJson(con.getInputStream(), null);
			Log.v(TAG, "result: " +  result);

		} finally {
			con.disconnect();
		}
	}

	public static String getContent(InputStream is) throws IOException {
		try {
			StringBuilder sb = new StringBuilder();
			InputStreamReader isr = new InputStreamReader(is);
			char [] buf = new char [1024];
			int size;
			while ((size = isr.read(buf)) != -1) {
				sb.append(buf, 0, size);
			}
			return sb.toString();
		} finally {
			is.close();
		}
	}

	public static ArrayList<History> fetchHistories(long watched_at) throws MalformedURLException, IOException {
		String token = Prefs.getSyoboiToken();
		if (TextUtils.isEmpty(token)) {
			return null;
		}

		Uri.Builder builder = new Uri.Builder()
			.appendQueryParameter("token", token)
			.appendQueryParameter("watched_at", String.valueOf(watched_at));

		String query = builder.build().getEncodedQuery();

		if (App.DEBUG) {
			Log.d(TAG, "fetchHistories url:" + query);
		}
		HttpURLConnection con = openConnection(API_HISTORIES);
		try {
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.getOutputStream().write(query.getBytes());
			con.connect();


			final ArrayList<History> histories = new ArrayList<History>();
			JksnObject jo = (JksnObject) JksnUtils.parseJson(con.getInputStream(), new JksnArrayCallback() {

				@Override
				public boolean onTargetObject(JsonParser jp, JksnObject j) {
					histories.add(new History(j));
					return true;
				}

				@Override
				public boolean isTargetArray(JsonParser jp, String name) {
					return "histories".equals(name);
				}
			});
			Log.d(TAG, "result: " +  jo);
			return histories;
		} finally {
			con.disconnect();
		}
	}

	static HttpURLConnection openConnection(String url) throws MalformedURLException, IOException {
		HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
		con.setConnectTimeout(10*1000);
		con.setReadTimeout(10*1000);
		return con;
	}

	public static class History {
		public String gtvid;
		public int time;
		public long watched_at;
		public History(JksnObject jo) {
			gtvid = jo.getString("gtvid");
			time = jo.getInt("time");
			watched_at = parseDateTime(jo.getString("watched_at"));
		}

		private static final Time sTime = new Time();
		public static long parseDateTime(String time) {
			Time t = sTime;
			synchronized (t) {
				t.year = Integer.valueOf(time.substring(0, 4));
				t.month = Integer.valueOf(time.substring(5,7)) - 1;
				t.monthDay = Integer.valueOf(time.substring(8, 10));
				t.hour = Integer.valueOf(time.substring(11, 13));
				t.minute = Integer.valueOf(time.substring(14, 16));
				t.second = Integer.valueOf(time.substring(17, 19));
				return t.toMillis(true);
			}
		}
	}

	public static class Histories {
		public HashMap<String,History> mMap = new HashMap<String,History>();
		public ArrayList<History> mList = new ArrayList<History>();

		public Histories() {

		}

		public synchronized long getWatchedAtMax() {
			if (mList.size() > 0) {
				return mList.get(0).watched_at;
			}
			return 0;
		}

		public synchronized void merge(ArrayList<History> newList) {
			synchronized (this) {
				ArrayList<History> list = mList;
				HashMap<String,History> map = mMap;
				int j = 0;
				for (History h: newList) {
					remove(h.gtvid);
					list.add(j, h);
					map.put(h.gtvid, h);
					j++;
				}
			}
		}

		synchronized void remove(String gtvid) {
			if (mMap.remove(gtvid) != null) {
				ArrayList<History> list = mList;
				for (int j=list.size()-1; j>=0; j--) {
					if (gtvid.equals(list.get(j).gtvid)) {
						list.remove(j);
						break;
					}
				}
			}
		}

		public synchronized History get(String gtvid) {
			return mMap.get(gtvid);
		}
	}
}
