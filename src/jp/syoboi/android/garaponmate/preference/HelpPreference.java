package jp.syoboi.android.garaponmate.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;

public class HelpPreference extends Preference {

	public HelpPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.help_preference);
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		final WebView web = (WebView) view.findViewById(R.id.webView);
//		web.getSettings().setAllowFileAccess(true);
		web.loadUrl("file:///android_asset/help.html");
		web.getSettings().setJavaScriptEnabled(true);
		web.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onReceivedTitle(WebView view, String title) {
				super.onReceivedTitle(view, title);
				web.loadUrl("javascript:document.getElementById('version').textContent='Version " + App.VERSION +"';");
			}
		});
	}
}
