package jp.syoboi.android.garaponmate.utils;

import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

	static Matcher UNCOOL_MATCHER;
	static StringBuilder UNCOOL_TMP = new StringBuilder();

	static HashMap<Character,Character> COOL_MAP = new HashMap<Character, Character>();
	static {
		COOL_MAP.put('　', ' ');
		COOL_MAP.put('：', ':');
		COOL_MAP.put('）', ')');
		COOL_MAP.put('（', '(');

		StringBuilder sb = new StringBuilder();

		sb.append("[０-９ａ-ｚＡ-Ｚ");
		for (Character key: COOL_MAP.keySet()) {
			sb.append(key);
		}
		sb.append("]");

		UNCOOL_MATCHER = Pattern.compile(sb.toString()).matcher("");
	}


	/**
	 * 番組表によくでてくる全角のダサい文字を変換
	 * @param text
	 * @return
	 */
	public synchronized static CharSequence convertCoolTitle(CharSequence text) {
		final Matcher m = UNCOOL_MATCHER;

		if (!m.reset(text).find()) {
			return text;
		}

		final StringBuilder sb = UNCOOL_TMP;
		sb.delete(0, sb.length());
		sb.append(text);

		int start;
		do {
			start = m.start();
			char c = sb.charAt(start);
			if ('０' <= c && c <= '９') {
				sb.setCharAt(start, (char) ('0' + c - '０'));
			}
			else if ('ａ' <= c && c <= 'ｚ') {
				sb.setCharAt(start, (char) ('a' + c - 'ａ'));
			}
			else if ('Ａ' <= c && c <= 'Ｚ') {
				sb.setCharAt(start, (char) ('A' + c - 'Ａ'));
			}
			else {
				Character newChar = COOL_MAP.get(c);
				if (newChar != null) {
					sb.setCharAt(start, newChar);
				}
			}
			start++;
		} while (m.find(start));

		return sb.toString();
	}

	public static String h(String text) {
		return Html.fromHtml(text).toString();
	}

	public static String readStream(InputStream is, String encoding) throws IOException {
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

	public static void spinnerSetSelectionById(Spinner spinner, Integer id) {
		SpinnerAdapter adapter = spinner.getAdapter();

		if (id == null) {
			if (spinner.getCount() > 0) {
				spinner.setSelection(0);
			}
			return;
		}

		for (int j=0; j<adapter.getCount(); j++) {
			if (adapter.getItemId(j) == id) {
				spinner.setSelection(j);
				break;
			}
		}
	}

	public static CharSequence highlightText(CharSequence text, Matcher m,
			int textColor, int bgColor) {

		m.reset(text);
		if (!m.find()) {
			return text;
		}

		SpannableString ss = new SpannableString(text);
		int start;
		do {
			int spanStart = m.start();
			int spanEnd = m.end();
			if (textColor != 0) {
				ss.setSpan(new ForegroundColorSpan(textColor),
						spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (bgColor != 0) {
				ss.setSpan(new BackgroundColorSpan(bgColor),
						spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			start = m.end();
		} while (m.find(start));

		return ss;
	}
}
