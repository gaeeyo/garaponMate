package jp.syoboi.android.garaponmate.data;

import android.text.format.Time;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.client.GaraponClient.Ch;
import jp.syoboi.android.util.JksnUtils.JksnArray;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

/**
 * 番組
 */
public class Program implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -8125082259488349596L;


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

	public Program(JksnObject jo, Map<Integer,Ch> chMap) {
		gtvid = jo.getString("gtvid");
		startdate = GaraponClient.parseDateTimeStr(jo.getString("startdate"));
		duration = GaraponClient.parseTimeStr(jo.getString("duration"));
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

	public Program(String gtvid) {
		this.gtvid = gtvid;
		startdate = 0;
		duration = 0;
		title = gtvid;
		description = null;
		genre = new int [0];
		ch = null;
		flag = 0;
	}

	public void write(JsonGenerator jg) throws JsonGenerationException, IOException {
		jg.writeStartObject();
		jg.writeStringField("gtvid", gtvid);
		jg.writeStringField("startdate", formatDateTime(startdate));
		jg.writeStringField("duration", formatDuration(duration));
		jg.writeStringField("ch", String.valueOf(ch.ch));
		jg.writeStringField("title", title);
		jg.writeFieldName("genre");
		jg.writeStartArray();
		for (int g: this.genre) {
			jg.writeString(formatGenreStr(g));
		}
		jg.writeEndArray();
		jg.writeStringField("favorite", ((flag & FLAG_FAVORITE) != 0) ? "1" : "0");
		jg.writeStringField("bc", ch.bc);
		jg.writeStringField("bc_tags", ch.bc_tags);
		jg.writeStringField("ts", ((flag & FLAG_TS) != 0) ? "1" : "0");
		jg.writeStringField("tsonly", ((flag & FLAG_TS_ONLY) != 0) ? "1" : "0");
		jg.writeEndObject();
	}

	static Time sTime = new Time();
	public String formatDateTime(long time) {
		synchronized (sTime) {
			sTime.set(time);
			return String.format("%04d-%02d-%02d %02d:%02d:%02d",
					sTime.year, sTime.month + 1, sTime.monthDay,
					sTime.hour, sTime.minute, sTime.second);

		}
	}

	public String formatDuration(long time) {
		time /= 1000;
		long sec = (time) % 60;
		long minute = (time / 60) % 60;
		long hour = (time / (60*60));
		return String.format("%02d:%02d:%02d",
				hour, minute, sec);
	}

	public static int parseGenreStr(String text) {
		int pos = text.indexOf('/');
		if (pos == -1) {
			return 0;
		}
		int genre0 = Integer.valueOf(text.substring(0,  pos), 10);
		int genre1 = Integer.valueOf(text.substring(pos+1), 10);
		return genre0 << 8 | genre1;
	}

	public static String formatGenreStr(int genre) {
		int genre0 = (genre >> 8) & 0xff;
		int genre1 = (genre & 0xff);
		return genre0 + "/" + genre1;
	}

	@Override
	public String toString() {
		Time t = new Time();
		t.set(startdate);
		long min = duration / 1000 / 60;

		return String.format("%s [%02d:%02d] %s %s %s",
				t.format("%Y-%m-%d %H:%M:%S"),
				min / 60, min % 60,
				ch.bc,
				title, description);
	}
}