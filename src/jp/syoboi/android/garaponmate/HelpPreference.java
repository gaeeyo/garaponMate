package jp.syoboi.android.garaponmate;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

public class HelpPreference extends Preference {

	public HelpPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.help_preference);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		WebView web = (WebView) view.findViewById(R.id.webView);
//		web.getSettings().setAllowFileAccess(true);
		web.loadUrl("file:///android_asset/help.html");
//		web.loadUrl("http://www.yahoo.co.jp/");
	}
}
