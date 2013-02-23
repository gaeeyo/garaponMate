package jp.syoboi.android.garaponmate.data;

import android.text.format.Time;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.client.GaraponClient.Ch;
import jp.syoboi.android.garaponmate.utils.Utils;
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
	private static final Caption [] CAPTION_EMPTY = new Caption [0];

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
	public int flag;
	public final Caption [] caption;

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

		JksnArray captionArray = jo.getArray("caption");
		if (captionArray != null && captionArray.size() > 0) {
			int captionCount = captionArray.size();
			caption = new Caption [captionCount];
			for (int j=0; j<captionCount; j++) {
				caption[j] = new Caption(captionArray.getObject(j));
			}
		} else {
			caption = CAPTION_EMPTY;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Program) {
			return gtvid.equals(((Program)o).gtvid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return gtvid.hashCode();
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
		caption = CAPTION_EMPTY;
	}

	public void write(JsonGenerator jg) throws JsonGenerationException, IOException {
		jg.writeStartObject();
		jg.writeStringField("gtvid", gtvid);
		jg.writeStringField("startdate", Utils.formatDateTime(startdate));
		jg.writeStringField("duration", Utils.formatDuration(duration));
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

	public static int parseGenreStr(String text) {
		int pos = text.indexOf('/');
		if (pos == -1) {
			return 0;
		}
		int genre0 = Integer.valueOf(text.substring(0,  pos), 10);
		int genre1 = Integer.valueOf(text.substring(pos+1), 10);
		return Genre.makeGenre(genre0, genre1);
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

		return String.format(Locale.ENGLISH,
				"%s [%02d:%02d] %s %s %s",
				t.format("%Y-%m-%d %H:%M:%S"),
				min / 60, min % 60,
				(ch != null ? ch.bc : null),
				title,
				description);
	}

	public boolean hasFlag(int flagMask) {
		return (flag & flagMask) != 0;
	}

	public void addFlag(int value) {
		flag |= value;
	}

	public void clearFlag(int value) {
		flag &= ~value;
	}
}