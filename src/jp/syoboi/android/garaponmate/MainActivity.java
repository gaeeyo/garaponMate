package jp.syoboi.android.garaponmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity  {

	static final String TAG = "MainActivity";

	static final int FLAG_AUTO_LOGIN_PROGRESS = 1;
	static final int [] FAV_BUTTONS = { R.id.fav0, R.id.fav1, R.id.fav2 };
	static final int [] PLAYER_BUTTONS = { R.id.back, R.id.forward };

	String 		mBaseUrl;
	String 		mUser;
	String 		mPass;
	int			mFlags;

	Handler			mHandler = new Handler();
	LinearLayout	mMainContainer;
	View			mPlayerContainer;
	WebView			mPlayerView;
	View			mPlayerOverlay;
	View			mWebViewContainer;
	WebView			mWebView;
	ProgressBar		mProgress;
	boolean			mPlayerExpanded;

	private boolean mSettingChanged;

	OnSharedPreferenceChangeListener	mPrefsChangeListener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			mSettingChanged = true;
		}
	};

	View.OnClickListener mOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.settings:
				startSettingsActivity();
				break;
			case R.id.forward:
				mPlayerView.loadUrl("javascript:document.getElementById(player_flg).SetVariable('player:jsSetFastForward', '30');");
				break;
			case R.id.back:
				mPlayerView.loadUrl("javascript:document.getElementById(player_flg).SetVariable('player:jsSetRewind', '15');");
				break;
			default:
				Object tag = v.getTag();
				if (tag instanceof String) {
					String tagName = tag.toString();
					if (tagName.startsWith("fav")) {
						navigateFav((String)v.getTag());

					}
				}
				break;
			}
		}
	};

	View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			extractTitle();

			final String tag = (String)v.getTag();

			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					String title = getTitle().toString();
					String url = mWebView.getUrl();
					saveFav(tag, title, url);
				}
			}, 500);
			return true;
		}
	};

	void extractTitle() {
		String script = "javascript:"
				+ "var page=document.getElementsByClassName('ui-page-active');"
				+ "var titles=page[0].getElementsByClassName('ui-title');"
				+ "var title=titles[0].textContent;"
				//+ "alert(title);"
				+ "document.title=title;";
		mWebView.loadUrl(script);
	}

	boolean navigateFav(String tag) {
		String url = getPrefs().getString(tag + "url", "");
		if (!TextUtils.isEmpty(url)) {
			mWebView.loadUrl(url);
			return true;
		}
		return false;
	}

	void saveFav(String tag, String title, String url) {
		getPrefs().edit()
		.putString(tag + "title", title)
		.putString(tag + "url", url)
		.commit();

		String msg = "★" + title + "\n"
				+ "URL:" + url;
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		reloadSettings();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		SharedPreferences prefs = getPrefs();
		prefs.registerOnSharedPreferenceChangeListener(mPrefsChangeListener);
		reloadSettings();

		for (int id: PLAYER_BUTTONS) {
			findViewById(id).setOnClickListener(mOnClickListener);
		}
		int favIndex = 0;
		for (int id: FAV_BUTTONS) {
			View v = findViewById(id);
			v.setTag("fav" + (favIndex++));
			v.setOnClickListener(mOnClickListener);
			v.setOnLongClickListener(mOnLongClickListener);
		}

		findViewById(R.id.settings).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				mWebView.loadUrl(mBaseUrl);
				return true;
			}
		});

		mMainContainer = (LinearLayout) findViewById(R.id.mainContainer);
		mPlayerContainer = findViewById(R.id.playerContainer);
		mWebViewContainer = findViewById(R.id.webViewContainer);
		mWebView = (WebView) findViewById(R.id.webView);
		mPlayerView = (WebView) findViewById(R.id.player);

		mProgress = (ProgressBar) findViewById(R.id.progress);
		mProgress.setMax(100);

		updateMainContainer();

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSaveFormData(false);
		webSettings.setSavePassword(false);
		//webSettings.setDatabaseEnabled(true);
		webSettings.setDomStorageEnabled(true);
//		webSettings.setPluginsEnabled(true);
//		webSettings.setPluginState(PluginState.ON);

		webSettings = mPlayerView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setPluginState(PluginState.ON);
//		webSettings.setDefaultZoom(ZoomDensity.CLOSE);
//		webSettings.setUseWideViewPort(true);
		mPlayerView.setHorizontalScrollBarEnabled(false);
		mPlayerView.setVerticalScrollBarEnabled(false);

		mPlayerOverlay = findViewById(R.id.playerOverlay);
		mPlayerOverlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				expandPlayer(true);
			}
		});

		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onLoadResource(WebView view, String url) {
				Log.v(TAG, "onLoadResource url:" + url);
				super.onLoadResource(view, url);
//				if (url.contains("/swf/fp/player_flv_maxi.swf")) {
//					resizePlayer();
//				}
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.v(TAG, "onPageStarted url:" + url);
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				Log.v(TAG, "onPageFinished url:" + url);
				super.onPageFinished(view, url);

				if (url.endsWith("/auth/login.garapon")) {
					autoLogin();
				}
			}

			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view,
					String url) {
				Log.v(TAG, "shouldInterceptRequest url:" + url);
				if (url.contains("/viewer/player.garapon?gtvid=")) {
					final String id = getGtvIdFromUrl(url, "gtvid");
					if (id != null) {
						mHandler.post(new Runnable() {
							@Override
							public void run() {
//								mWebView.stopLoading();
								setPlayerPage(id);
							}
						});
						return super.shouldInterceptRequest(view, null);
					}
				}
				// site.garapon.tv の画像をローカルで開く
				if (url.contains("http://site.garapon.tv/g?g=")) {
					final String id = getGtvIdFromUrl(url, "g");
					if (id != null) {
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								mWebView.stopLoading();
								setPlayerPage(id);
							}
						});
						return super.shouldInterceptRequest(view, null);
					}
				}
				return super.shouldInterceptRequest(view, url);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.v(TAG, "shouldOverrideUrlLoading url:" + url);

				if (url.contains("/main.garapon")) {
					if ((mFlags & FLAG_AUTO_LOGIN_PROGRESS) != 0) {
						// ログインが完了したら fav0 を開く
						mFlags &= ~FLAG_AUTO_LOGIN_PROGRESS;
						if (navigateFav("fav0")) {
							return true;
						}
					}
				}
				return super.shouldOverrideUrlLoading(view, url);
			}
		});

		mWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onReceivedTitle(WebView view, String title) {
				Log.v(TAG, "onReceivedTitle title:" + title);
				super.onReceivedTitle(view, title);
				setTitle(title);
			}
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				super.onProgressChanged(view, newProgress);

				if (newProgress == 100) {
					mProgress.setVisibility(View.GONE);
				} else {
					mProgress.setVisibility(View.VISIBLE);
					mProgress.setProgress(newProgress);
				}
			}
		});

		login();
	}

	String getGtvIdFromUrl(String url, String key) {
		int start = url.indexOf(key + "=");
		if (start != -1) {
			int end = url.indexOf("&", start);
			if (end == -1) {
				end = url.length();
			}
			String id = url.substring(start + key.length() + 1, end);
			return id;
		}
		return null;
	}

	@Override
	protected void onDestroy() {
		SharedPreferences prefs = getPrefs();
		prefs.unregisterOnSharedPreferenceChangeListener(mPrefsChangeListener);
		if (mWebView != null) {
			mWebView.destroy();
			mWebView = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mWebView.onPause();
		mPlayerView.onPause();
	}

	@Override
	protected void onResume() {
		mWebView.onResume();
		mPlayerView.onResume();
		super.onResume();

		if (mSettingChanged) {
			reloadSettings();
			login();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
//		resizePlayer();
		updateMainContainer();
	}

	@Override
	public void onBackPressed() {
		if (mPlayerExpanded) {
			expandPlayer(false);
			return;
		}

		if (mWebView.canGoBack()) {
			mWebView.goBack();
			return;
		}
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startSettingsActivity();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	void startSettingsActivity() {
		Intent i = new Intent(MainActivity.this, SettingActivity.class);
		startActivity(i);
	}

	void reloadSettings() {
		mSettingChanged = false;

		SharedPreferences prefs = getPrefs();
		mUser = prefs.getString("user", null);
		mPass = prefs.getString("password", null);
		mBaseUrl = prefs.getString("baseUrl", null);
		if (mBaseUrl != null && mBaseUrl.endsWith("/")) {
			mBaseUrl = mBaseUrl.substring(0, mBaseUrl.length()-1);
		}

		int idx = 0;
		for (int id: FAV_BUTTONS) {
			TextView tv = (TextView) findViewById(id);
			String title = prefs.getString("fav" + idx + "title", null);
			if (TextUtils.isEmpty(title)) {
				title = "fav" + idx;
			}
			tv.setText(title);
			idx++;
		}
	}


	SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	void autoLogin() {
	//		Log.v(TAG, "自動ログイン");
	//		String script = "javascript:"
	//				+ "var f=document.forms[0];"
	//				+ "f.LoginID.value=\"" + mUser +"\";"
	//				+ "f.Passwd.value=\"" + mPass +"\";"
	//				+ "f.submit();"
	//				;
	//		mWebView.loadUrl(script);
		}

	void login() {

		if (TextUtils.isEmpty(mUser)
				|| TextUtils.isEmpty(mPass)
				|| TextUtils.isEmpty(mBaseUrl)) {

			new AlertDialog.Builder(this).setTitle(R.string.settingsWarning)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startSettingsActivity();
				}
			})
			.setNeutralButton(android.R.string.no, null)
			.show();
			return;
		}


		String data = "LoginID=" + mUser
				+ "&" + "Passwd=" + mPass;

		mFlags |= FLAG_AUTO_LOGIN_PROGRESS;
		mWebView.postUrl(mBaseUrl + "/", data.getBytes());
	}

	Runnable mResizePlayerRunnable = new Runnable() {
		@Override
		public void run() {
			Log.v(TAG, "resizePlayer");
			float density = getResources().getDisplayMetrics().density;
			DisplayMetrics dm = getResources().getDisplayMetrics();

			int toolbarHeight = 20;
			int pagePadding = (int)(15 * density);
			int viewWidth = (int)(dm.widthPixels / density);
			int viewHeight = (int)(dm.heightPixels / density);

			float aspect = 16 / 9f;
			int width = viewWidth - pagePadding * 2;
			int height = (int)(width / aspect + toolbarHeight);

			if (height > viewHeight) {
				height = viewHeight - toolbarHeight;
				width = (int)(height * aspect);
			}

			String script = "javascript:"
					+ "var objs=document.getElementsByTagName('object');"
					+ "for (var j=0; j<objs.length; j++) {"
					+ " objs[j].width=" + width + ";"
					+ " objs[j].height=" + height + ";"
					+ "}"
					;
			mWebView.loadUrl(script);
		}
	};

	void resizePlayer() {
		mHandler.removeCallbacks(mResizePlayerRunnable);
		mHandler.postDelayed(mResizePlayerRunnable, 500);
	}

	String getSession() {
		String cookies = CookieManager.getInstance().getCookie(mBaseUrl);

		Matcher m = Pattern.compile("GaraponAuthKey=([0-9a-z]+)").matcher(cookies);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	void setPlayerPage(String id) {
		Log.v(TAG, "setPlayerPage id:" + id);
		String sessionId = getSession();

		String flv = id.substring(6,8) + "/" + id + ".ts-" + sessionId;
		int pos = mBaseUrl.indexOf("//");
		String rtmp = "rtmp://" + mBaseUrl.substring(pos + 2) + "/";

		String html = "<html>"
				+ "<style type='text/css'>"
				+ "body { padding:0; margin:0; background:#000; color:#fff; text-align:center; width:100%; }"
				+ "#player_flg { text-align:center; width:100%; height:100%; }"
				+ "</style>"
				+ "<body>"
				+ "<object id='player_flg' type='application/x-shockwave-flash' data='/swf/fp/player_flv_maxi.swf?20120413'>"
				//+ "<param name='allowFullScreen' value='true'>"
				+ "<param name='allowScriptAccess' value='always'>"
				+ "<param name='flashvars' value='arg1=val"
					+ "&amp;flv=" + flv
					+ "&amp;netconnection=" + rtmp
					+ "&amp;showstop=0"
					+ "&amp;showvolume=1"
					//+ "&amp;showtime=2"
					+ "&amp;showtime=2"
					//+ "&amp;showfullscreen=1"
					//+ "&amp;margin=1"
					+ "&amp;margin=0"
					+ "&amp;bgcolor1=000000"
					+ "&amp;bgcolor2=000000"
					+ "&amp;playercolor=000000"
					+ "&amp;loadingcolor=0aff17"
					+ "&amp;buffershowbg=0"
					//+ "&amp;ondoubleclick=fullscreen"
					+ "&amp;showiconplay=1"
					+ "&amp;sliderovercolor=0aff17"
					+ "&amp;buttonovercolor=0aff17"
					+ "&amp;showplayer=always"
					+ "&amp;showloading=always"
					+ "&amp;autoload=1"
					+ "&amp;autoplay=1"
					+ "'>"
				+ "</object>"
				+ "<div style='background:#000; width:1px; height:100%; position:absolute; top:0px; right:0px; overflow:hidden;'>&nbsp;</div>"
				+ "<script type='text/javascript'>"
				+ "var player_flg='player_flg';"
				+ "</script>"
				+ "</body>"
				+ "</html>"
				;
		mPlayerView.loadDataWithBaseURL(mBaseUrl, html, "text/html", "UTF-8", null);
		expandPlayer(false);
	}

	void expandPlayer(boolean expand) {

		mPlayerExpanded = expand;
		mPlayerOverlay.setVisibility(expand ? View.GONE : View.VISIBLE);

		mPlayerContainer.setVisibility(View.VISIBLE);
		mWebViewContainer.setVisibility(expand ? View.GONE : View.VISIBLE);
		for (int id: PLAYER_BUTTONS) {
			findViewById(id).setVisibility(expand ? View.VISIBLE : View.GONE);
		}
		for (int id: FAV_BUTTONS) {
			findViewById(id).setVisibility(expand ? View.GONE : View.VISIBLE);
		}

		updatePlayerContainerSize();
//		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mPlayerContainer.getLayoutParams();
//		if (expand) {
//			lp.height = 0;
//			lp.weight = 1;
//
//		} else {
//			Point size = calcPlayerSize();
//			lp.weight = 0;
//			lp.height = size.y;
//		}
//		mPlayerContainer.requestLayout();
	}

	void updatePlayerContainerSize() {
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mPlayerContainer.getLayoutParams();
		switch (getResources().getConfiguration().orientation) {
		case Configuration.ORIENTATION_LANDSCAPE:
			lp.width = mPlayerExpanded
					? LayoutParams.FILL_PARENT
					: getResources().getDisplayMetrics().widthPixels / 3;
			lp.height = LayoutParams.FILL_PARENT;
			break;
		default:
			lp.width = LayoutParams.FILL_PARENT;
			lp.height = mPlayerExpanded
				? LayoutParams.FILL_PARENT
				: getResources().getDisplayMetrics().heightPixels / 3;
			break;
		}
	}

	Point calcPlayerSize() {
		float density = getResources().getDisplayMetrics().density;
		DisplayMetrics dm = getResources().getDisplayMetrics();

		int toolbarHeight = 20;
		int pagePadding = (int)(15 * density);
		int viewWidth = (int)(dm.widthPixels / density);
		int viewHeight = (int)(dm.heightPixels / density);

		float aspect = 16 / 9f;
		int width = viewWidth - pagePadding * 2;
		int height = (int)(width / aspect + toolbarHeight);

		if (height > viewHeight) {
			height = viewHeight - toolbarHeight;
			width = (int)(height * aspect);
		}
		Point pt = new Point(width, height);
		return pt;
	}

	void updateMainContainer() {

//		LinearLayout.LayoutParams webLp = (LinearLayout.LayoutParams)mWebViewContainer.getLayoutParams();
//		LinearLayout.LayoutParams playerLp = (LinearLayout.LayoutParams)mPlayerContainer.getLayoutParams();

		updatePlayerContainerSize();

		switch (getResources().getConfiguration().orientation) {
		case Configuration.ORIENTATION_LANDSCAPE:
			mMainContainer.setOrientation(LinearLayout.HORIZONTAL);
//			webLp.height = LayoutParams.FILL_PARENT;
//			webLp.weight = 0;
//			playerLp.width = getResources().getDisplayMetrics().widthPixels / 3;
			break;
		default:
			mMainContainer.setOrientation(LinearLayout.VERTICAL);
//			playerLp.width = LayoutParams.FILL_PARENT;
//			webLp.height = 0;
//			webLp.weight = 1;
			break;
		}
	}
}
