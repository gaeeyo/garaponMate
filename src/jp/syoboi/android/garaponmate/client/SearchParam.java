package jp.syoboi.android.garaponmate.client;

import java.io.IOException;
import java.io.Serializable;

import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

public class SearchParam implements Serializable {
	private static final long serialVersionUID = -1535056156459423850L;

	public static final int GENRE_EMPTY = -1;

	public static final int RANK_FAVORITE = 1;

	public static final int STYPE_EPG = 0;
	public static final int STYPE_SUBTITLE = 1;

	public static final int STIME_START = 0;
	public static final int STIME_END = 1;

	public static final int SORT_STD = 0;
	public static final int SORT_STA = 1;

	public static final int VIDEO_ALL = 1;

	public long id;
	public String comment;

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
	public int video;

	public SearchParam() {
		page = 1;
		count = 50;
	}

	public SearchParam(JksnObject j) {
		this();
		id = j.getLong("id", 0);
		comment = j.getString("comment");

		searchType = j.getInt("searchType", 0);
		keyword = j.getString("keyword");
		gtvid = j.getString("gtvid");
		genre0 = j.getInt("genre0", GENRE_EMPTY);
		genre1 = j.getInt("genre1", GENRE_EMPTY);
		ch = j.getInt("ch", 0);
		searchTime = j.getInt("searchTime", 0);
		sdate = j.getLong("sdate", 0);
		edate = j.getLong("edate", 0);
		rank = j.getInt("rank", 0);
		sort = j.getInt("sort", SORT_STD);
		video = j.getInt("video", 0);
	}

	public void write(JsonGenerator j) throws JsonGenerationException, IOException {
		j.writeStartObject();
		j.writeNumberField("id", id);
		j.writeStringField("comment", comment);

		j.writeNumberField("searchType", searchType);
		j.writeStringField("keyword", keyword);
		j.writeStringField("gtvid", gtvid);
		j.writeNumberField("genre0", genre0);
		j.writeNumberField("genre1", genre1);
		j.writeNumberField("ch", ch);
		j.writeNumberField("searchTime", searchTime);
		j.writeNumberField("sdate", sdate);
		j.writeNumberField("edate", edate);
		j.writeNumberField("rank", rank);
		j.writeNumberField("sortAscent", sort);
		j.writeNumberField("videoAll", video);
		j.writeEndObject();
	}
}