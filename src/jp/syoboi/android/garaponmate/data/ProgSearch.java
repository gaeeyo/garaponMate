package jp.syoboi.android.garaponmate.data;

import android.text.TextUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.garaponmate.GaraponClient.Program;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

public class ProgSearch implements Serializable {

	private static final long serialVersionUID = 1618893702757957265L;


	public String kwAnd;
	public String kwOr;
	public String kwNot;

	public String titleAnd;
	public String titleOr;
	public String titleNot;

	public ProgSearch() {
	}

	public ProgSearch(JksnObject jo) {
		kwAnd = jo.getString("kwAnd");
		kwOr = jo.getString("kwOr");
		kwNot = jo.getString("kwNot");
		titleAnd = jo.getString("titleAnd");
		titleOr = jo.getString("titleOr");
		titleNot = jo.getString("titleNot");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb
		.append(kwAnd).append(", ")
		.append(kwOr).append(", ")
		.append(kwNot).append(", ")
		.append(titleAnd).append(", " )
		.append(titleOr).append(", ")
		.append(titleNot);

		return sb.toString();
	}

	public void toJson(JsonGenerator jg) throws JsonGenerationException, IOException {
		jg.writeStartObject();
		jg.writeStringField("kwAnd", kwAnd);
		jg.writeStringField("kwOr", kwOr);
		jg.writeStringField("kwNot", kwNot);
		jg.writeStringField("titleAnd", titleAnd);
		jg.writeStringField("titleOr", titleOr);
		jg.writeStringField("titleNot", titleNot);
		jg.writeEndObject();
	}

	public ProgSearchMatcher getMatcher() {
		return new ProgSearchMatcher(this);
	}

	public static class ProgSearchMatcher {

		private static final String KEYWORD_SPLIT = "[ 　]";
		private static final String [] EMPTY_ARRAY = new String [0];

		final Matcher	[] mKw;
		final Matcher mKwNot;

		public ProgSearchMatcher(ProgSearch f) {
			int patternFlag = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;

			String [] kwOr = (f.kwOr == null ? EMPTY_ARRAY : splitWord(f.kwOr));
			String [] kwAnd = (f.kwAnd == null ? EMPTY_ARRAY : splitWord(f.kwAnd));
			String [] kwNot = (f.kwNot == null ? EMPTY_ARRAY : splitWord(f.kwNot));

			mKw = new Matcher [kwAnd.length + (kwOr.length > 0 ? 1 : 0)];

			// or は 1つの正規表現にまとめる
			int idx = 0;
			if (kwOr.length > 0) {
				mKw[idx++] = Pattern.compile(wordsToRegex(kwOr), patternFlag).matcher("");
			}
			for (int j=0; j<kwAnd.length; j++) {
				mKw[idx++] = Pattern.compile(Pattern.quote(kwAnd[j]), patternFlag).matcher("");
			}

			String kwNotPtn = wordsToRegex(kwNot);
			if (kwNotPtn.length() > 0) {
				mKwNot = Pattern.compile(kwNotPtn, patternFlag).matcher("");
			} else {
				mKwNot = null;
			}
		}

		public boolean match(Program p) {
			if (mKwNot != null && (mKwNot.reset(p.description).find() || mKwNot.reset(p.title).find())) {
				return false;
			}
			for (Matcher m: mKw) {
				if ((!m.reset(p.description).find() && !m.reset(p.title).find())) {
					return false;
				}
			}
			return true;
		}

		static String [] splitWord(String keyword) {
			String [] words = keyword.split(KEYWORD_SPLIT);
			if (words.length == 1 && words[0].length() == 0) {
				return EMPTY_ARRAY;
			}
			return words;
		}

		static String wordsToRegex(String [] words) {
			StringBuilder sb = new StringBuilder();
			for (int j=0; j<words.length; j++) {
				if (sb.length() > 0) {
					sb.append('|');
				}
				sb.append(Pattern.quote(words[j]));
			}
			return sb.toString();
		}
	}

	public boolean isEmpty() {
		if (TextUtils.isEmpty(kwAnd)
				&& TextUtils.isEmpty(kwOr)
				&& TextUtils.isEmpty(kwNot)
				&& TextUtils.isEmpty(titleAnd)
				&& TextUtils.isEmpty(titleOr)
				&& TextUtils.isEmpty(titleNot)) {
			return true;
		}
		return false;
	}
}
