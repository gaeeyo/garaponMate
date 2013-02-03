package jp.syoboi.android.garaponmate.utils;

import android.widget.TextView;

public class WidgetUtils {
	public static int getInt(TextView edit, int fallback) {
		String str = edit.getText().toString();
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			return fallback;
		}
	}
}
