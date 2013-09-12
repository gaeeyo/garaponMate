package jp.syoboi.android.garaponmate.data;

import android.text.TextUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.utils.Utils;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

public class SearchParam extends GaraponClient.Search implements Serializable, Cloneable {
	private static final long serialVersionUID = -1535056156459423850L;


	public static final int COUNT_MAX = 100;
	public long id;
	public String comment;

	public int durationMin;
	public int durationMax;

	private transient Pattern mPattern;

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

		durationMin = j.getInt("durationMin", 0);
		durationMax = j.getInt("durationMax", 0);
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
		j.writeNumberField("durationMin", durationMin);
		j.writeNumberField("durationMax", durationMax);
		j.writeEndObject();
	}

	@Override
	public SearchParam clone() {
		try {
			return (SearchParam) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Matcher createMatcher() {
		Pattern ptn = createPattern();
		if (ptn == null) {
			return null;
		}
		return ptn.matcher("");
	}

	public Pattern createPattern() {
		if (mPattern != null) {
			return mPattern;
		}

		if (TextUtils.isEmpty(keyword)) {
			return null;
		}
		String words [] = keyword.split("[ 　]+");
		StringBuilder sb = new StringBuilder();
		for (String word: words) {
			if (word.length() < 1) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append('|');
			}
			sb.append(Pattern.quote(word));
		}

		if (sb.length() == 0) {
			return null;
		}

		mPattern = Pattern.compile(Utils.convertCoolTitle(sb.toString()).toString(),
				Pattern.UNICODE_CASE | Pattern.DOTALL);
		return mPattern;
	}

	public PostMatcher createPostMatcher() {
		return new PostMatcher(this);
	}

	public static class PostMatcher {
		SearchParam mSearchParam;
		int durationMin;
		int durationMax;

		public PostMatcher(SearchParam p) {
			mSearchParam = p;
			durationMin = p.durationMin;
			durationMax = (p.durationMax != 0 ? p.durationMax : Integer.MAX_VALUE);
		}
		public boolean match(Program p) {
			if (p.duration < durationMin || p.duration > durationMax) {
				return false;
			}
			return true;
		}
	}
}