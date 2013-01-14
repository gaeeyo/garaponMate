package jp.syoboi.android.garaponmate;

import android.text.Html;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

	static Matcher UNCOOL_MATCHER = Pattern.compile("[０-９ａ-ｚＡ-Ｚ　]").matcher("");
	static StringBuilder UNCOOL_TMP = new StringBuilder();

	public synchronized static String convertCoolTitle(String text) {
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
			else if (c == 0x3000) {
				sb.setCharAt(start, ' ');
			}
			start++;
		} while (m.find(start));

		return sb.toString();
	}

	public static String h(String text) {
		return Html.fromHtml(text).toString();
	}

}
