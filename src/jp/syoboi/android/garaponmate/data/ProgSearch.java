package jp.syoboi.android.garaponmate.data;

import android.text.TextUtils;
import android.text.format.DateUtils;

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

	public String chOr;

	public int durationMin;
	public int durationMax;

	public ProgSearch() {
	}

	public ProgSearch(JksnObject jo) {
		kwAnd = jo.getString("kwAnd");
		kwOr = jo.getString("kwOr");
		kwNot = jo.getString("kwNot");
		titleAnd = jo.getString("titleAnd");
		titleOr = jo.getString("titleOr");
		titleNot = jo.getString("titleNot");
		chOr = jo.getString("chOr");
		durationMin = jo.getInt("durationMin", 0);
		durationMax = jo.getInt("durationMax", 0);
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
		.append(titleNot).append(", ")
		.append(chOr).append(", ")
		.append(durationMin).append(", ")
		.append(durationMax);

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
		jg.writeStringField("chOr", chOr);
		jg.writeNumberField("durationMin", durationMin);
		jg.writeNumberField("durationMax", durationMax);
		jg.writeEndObject();
	}

	public ProgSearchMatcher getMatcher() {
		return new ProgSearchMatcher(this);
	}

	public static class ProgSearchMatcher {

		private static final String KEYWORD_SPLIT = "[ 　]";
		private static final String [] EMPTY_ARRAY = new String [0];

		final Matcher [] mKw;
		final Matcher mKwNot;
		final Matcher [] mTitle;
		final Matcher mTitleNot;
		final Matcher [] mCh;
		final long mDurationMin;
		final long mDurationMax;

		public ProgSearchMatcher(ProgSearch f) {
			int patternFlag = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;

			mKw = createMatchers(f.kwAnd, f.kwOr, patternFlag);
			mKwNot = createNotMatcher(f.kwNot, patternFlag);

			mTitle = createMatchers(f.titleAnd, f.titleOr, patternFlag);
			mTitleNot = createNotMatcher(f.titleNot, patternFlag);

			mCh = createMatchers(null, f.chOr, patternFlag);
			mDurationMin = (f.durationMin == 0 ? Long.MIN_VALUE : f.durationMin * DateUtils.MINUTE_IN_MILLIS);
			mDurationMax = (f.durationMax == 0 ? Long.MAX_VALUE : f.durationMax * DateUtils.MINUTE_IN_MILLIS);
		}

		public boolean match(Program p) {
			if (!(mDurationMin <= p.duration && p.duration < mDurationMax)) {
				return false;
			}
			if (mKwNot != null && (mKwNot.reset(p.description).find() || mKwNot.reset(p.title).find())) {
				return false;
			}
			if (mTitleNot != null && mTitleNot.reset(p.title).find()) {
				return false;
			}
			if (mKw != null) {
				for (Matcher m: mKw) {
					if ((!m.reset(p.description).find() && !m.reset(p.title).find())) {
						return false;
					}
				}
			}
			if (mTitle != null) {
				for (Matcher m: mTitle) {
					if ((!m.reset(p.title).find())) {
						return false;
					}
				}
			}
			if (mCh != null) {
				for (Matcher m: mCh) {
					if ((!m.reset(p.ch.bc).find())) {
						return false;
					}
				}
			}
			return true;
		}

		static Matcher [] createMatchers(String inKwAnd, String inKwOr, int patternFlag) {
			String [] kwAnd = splitWord(inKwAnd);
			String [] kwOr = splitWord(inKwOr);

			int count = kwAnd.length + (kwOr.length > 0 ? 1 : 0);
			if (count == 0) {
				return null;
			}
			Matcher [] matchers = new Matcher [count];

			// or は 1つの正規表現にまとめる
			int idx = 0;
			if (kwOr.length > 0) {
				matchers[idx++] = Pattern.compile(wordsToRegex(kwOr), patternFlag).matcher("");
			}
			for (int j=0; j<kwAnd.length; j++) {
				matchers[idx++] = Pattern.compile(Pattern.quote(kwAnd[j]), patternFlag).matcher("");
			}
			return matchers;
		}

		static Matcher createNotMatcher(String inKwNot, int patternFlag) {
			String [] kwNot = splitWord(inKwNot);
			String kwNotPtn = wordsToRegex(kwNot);

			if (kwNotPtn.length() > 0) {
				return Pattern.compile(kwNotPtn, patternFlag).matcher("");
			} else {
				return null;
			}
		}

		static String [] splitWord(String keyword) {
			if (TextUtils.isEmpty(keyword)) {
				return EMPTY_ARRAY;
			}
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
