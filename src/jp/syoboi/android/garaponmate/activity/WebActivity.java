package jp.syoboi.android.garaponmate.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import jp.syoboi.android.garaponmate.App;

@SuppressLint("SetJavaScriptEnabled")
public class WebActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WebView web = new WebView(this);
		web.getSettings().setJavaScriptEnabled(true);
//		web.setWebViewClient(new WebViewClient() {
//
//		});
		web.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onReceivedTitle(WebView view, String title) {
				super.onReceivedTitle(view, title);
				setTitle(title);
				view.loadUrl("javascript:document.getElementById('version').textContent='Version " + App.VERSION +"';");
			}
		});
		web.loadUrl(getIntent().getDataString());
		setContentView(web);
	}
}
