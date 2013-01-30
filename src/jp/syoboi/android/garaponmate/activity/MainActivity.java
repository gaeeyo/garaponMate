package jp.syoboi.android.garaponmate.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.HashMap;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.client.SearchParam;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.fragment.ErrorDialogFragment;
import jp.syoboi.android.garaponmate.fragment.SearchResultFragment;
import jp.syoboi.android.garaponmate.fragment.SummaryFragment;
import jp.syoboi.android.garaponmate.service.PlayerService;
import jp.syoboi.android.garaponmate.task.ProgressDialogTask;
import jp.syoboi.android.garaponmate.view.PlayerView;

public class MainActivity extends Activity  {

	static final String TAG = "MainActivity";

	static final int FLAG_AUTO_LOGIN_PROGRESS = 1;
	static final long CHANGE_FULLSCREEN_DELAY = 3000;

	static final String SPECIAL_PAGE_PATH = "/garaponMate";

	private static final int PAGE_SUMMARY = 0;
	private static final int PAGE_WEB = 1;
	private static final int PAGE_SEARCH = 2;

	int				mFlags;

	Handler			mHandler = new Handler();
	LinearLayout	mMainContainer;
	PlayerView		mPlayer;
	View			mPlayerOverlay;
	View			mWebViewContainer;
	WebView			mWebView;
	ProgressBar		mProgress;
	View			mPlayerClose;
	View			mSummaryPage;
	View			mSearchPage;
	boolean			mPlayerExpanded;
	boolean			mTitleExtracting;
	boolean			mReloadAfterLogin;
	int				mPage;

	private boolean 	mSettingChanged;
	ProgressDialogTask	mLoginTask;

	OnSharedPreferenceChangeListener	mPrefsChangeListener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			if (Prefs.USER.equals(key) || Prefs.PASSWORD.equals(key)) {
				mSettingChanged = true;
			}
		}
	};

	View.OnClickListener mOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.special:
				switchPage(PAGE_SUMMARY);
				break;
			case R.id.settings:
				startSettingsActivity();
				break;
			case R.id.web:
				switchPage(PAGE_WEB);
				mWebView.loadUrl(Prefs.getBaseUrl());
				break;
			}
		}
	};

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			// 起動時はガラポン認証が実行されるようにする
			GaraponClientUtils.setRefreshAuth();
		}

		Prefs.getInstance().registerOnSharedPreferenceChangeListener(mPrefsChangeListener);

		mSummaryPage = findViewById(R.id.summaryFragment);
		mSearchPage = findViewById(R.id.searchResultFragment);

		findViewById(R.id.special).setOnClickListener(mOnClickListener);
		findViewById(R.id.web).setOnClickListener(mOnClickListener);
		findViewById(R.id.settings).setOnClickListener(mOnClickListener);

		mMainContainer = (LinearLayout) findViewById(R.id.mainContainer);
		mPlayer = (PlayerView) findViewById(R.id.player);
		mWebViewContainer = findViewById(R.id.webViewContainer);
		mWebView = (WebView) findViewById(R.id.webView);
		mPlayerClose = findViewById(R.id.playerClose);

		mProgress = (ProgressBar) findViewById(R.id.progress);
		mProgress.setMax(100);


		updateMainContainer();

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSaveFormData(false);
		webSettings.setSavePassword(false);
		webSettings.setDomStorageEnabled(true);

		mPlayerClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				closePlayer();
			}
		});

		mPlayerOverlay = findViewById(R.id.playerOverlay);
		mPlayerOverlay.setOnTouchListener(new View.OnTouchListener() {
			boolean touching;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (!mPlayerExpanded) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						touching = true;
						break;
					case MotionEvent.ACTION_UP:
						if (touching) {
							expandPlayer(true);
						}
						touching = false;
						break;
					case MotionEvent.ACTION_CANCEL:
						touching = false;
						break;
					}
					return true;
				} else {
					return false;
				}
			}
		});

		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onLoadResource(WebView view, String url) {
				Log.v(TAG, "onLoadResource url:" + url);
				super.onLoadResource(view, url);
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
				extractTitle();
			}

			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view,
					String url) {
				Log.v(TAG, "shouldInterceptRequest url:" + url);
				if (overrideUrl(url)) {
					return super.shouldInterceptRequest(view, null);
				}

				return super.shouldInterceptRequest(view, url);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.v(TAG, "shouldOverrideUrlLoading url:" + url);
				if (overrideUrl(url)) {
					return true;
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

				if (mTitleExtracting) {
					mTitleExtracting = false;
					if (title.equals("ログアウト")) {
						mReloadAfterLogin = true;
						loginBackground();
					}
				}
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
			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				return super.onConsoleMessage(consoleMessage);
			}
		});

//		navigateFav("fav0");
//		loginBackground();
		int page = (savedInstanceState == null
				? PAGE_SUMMARY : savedInstanceState.getInt("page"));
		switchPage(page);

//		ProgManager.getInstance().refresh();
	}

	boolean overrideUrl(String url) {
		if (url.contains("/viewer/player.garapon?gtvid=")) {
			final String id = getGtvIdFromUrl(url, "gtvid");
			if (id != null) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
//						mWebView.stopLoading();
						playVideo(new Program(id));
					}
				});
				return true;
			}
		}
		if (url.contains("/main.garapon")) {
			if ((mFlags & FLAG_AUTO_LOGIN_PROGRESS) != 0) {
				// ログインが完了したら fav0 を開く
				mFlags &= ~FLAG_AUTO_LOGIN_PROGRESS;
			}
		}
		if (url.endsWith("/auth/login.garapon")) {
			// ログイン画面に遷移するとき、自動ログインする
			if ((mFlags & FLAG_AUTO_LOGIN_PROGRESS) == 0) {
				loginBackground();
				return true;
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
						playVideo(new Program(id));
					}
				});
				return true;
			}
		}
		// site.garapon.tv の画像をローカルで開く
		if (url.contains("/play?")) {
			final String id = getGtvIdFromUrl(url, "gtvid");
			playVideo(new Program(id));
			return true;
		}

		return false;
	}

	@Override
	protected void onDestroy() {
		Prefs.getInstance().unregisterOnSharedPreferenceChangeListener(mPrefsChangeListener);

		if (mLoginTask != null) {
			mLoginTask.cancel(true);
			mLoginTask = null;
		}
		if (mWebView != null) {
			mWebView.destroy();
			mWebView = null;
		}
		if (mPlayer != null) {
			mPlayer.destroy();
			mPlayer = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mWebView.onPause();
		mPlayer.onPause();
	}

	@Override
	protected void onResume() {
		mWebView.onResume();
		mPlayer.onResume();
		super.onResume();

		// 設定が変更されていたら読み直してログインしなおす
		if (mSettingChanged) {
			loginBackground();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("page", mPage);
	}

	/**
	 * 画面の向きが変わったらレイアウトを再設定
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		updateMainContainer();
	}

	@Override
	public void onBackPressed() {
		// フルスクリーンだったら解除
		mPlayer.setFullScreen(false);

		// プレイヤーが拡大されていたら縮小
		if (mPlayerExpanded) {
			expandPlayer(false);
			return;
		}

		if (mPage == PAGE_SEARCH) {
			if (getFragmentManager().getBackStackEntryCount() > 0) {
				if (getFragmentManager().getBackStackEntryCount() == 1) {
					switchPage(PAGE_SUMMARY);
				}
				getFragmentManager().popBackStack();
				return;
			}
		}

		if (mPage != PAGE_SUMMARY) {
			switchPage(PAGE_SUMMARY);
			return;
		}
//		//ページが戻れる状態だったら戻る
//		if (mWebView.canGoBack()) {
//			mWebView.goBack();
//			return;
//		}
		super.onBackPressed();
	}

	/**
	 * 設定を開く
	 */
	void startSettingsActivity() {
		Intent i = new Intent(MainActivity.this, SettingActivity.class);
		startActivity(i);
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

	/**
	 * Playerで再生
	 * @param id
	 */
	public void playVideo(Program p, int playerId) {
		switch (playerId) {
		case App.PLAYER_POPUP:
			{
				Intent i = new Intent(this, PlayerService.class);
				i.setAction(PlayerService.ACTION_SET_VIDEO);
				i.putExtra(App.EXTRA_PROGRAM, p);
				startService(i);
			}
			return;
		case App.PLAYER_EXTERNAL:
			playVideoExternal(p);
			return;
		}

		mPlayer.setVideo(p, playerId);

		expandPlayer(false);
		onVideoChanged(p.gtvid);
	}

	public void playVideo(Program p) {
		playVideo(p, Prefs.getPlayerId());
	}

	public void closePlayer() {
		mPlayer.destroy();
		expandPlayer(false);
		mPlayer.setVisibility(View.GONE);
		onVideoChanged(null);
	}

	void onVideoChanged(String id) {

		Fragment f = getFragmentManager().findFragmentById(R.id.summaryFragment);
		if (f instanceof SummaryFragment) {
			((SummaryFragment) f).setVideo(id);
		}
		f = getFragmentManager().findFragmentById(R.id.searchResultFragment);
		if (f instanceof SearchResultFragment) {
			((SearchResultFragment) f).setVideo(id);
		}
	}


	public void playVideoExternal(Program p) {
		Uri uri = Uri.parse("http://" + Prefs.getIpAdr()
				+ "/cgi-bin/play/m3u8.cgi?"
				+ p.gtvid
				+ "-" + Prefs.getCommonSessionId());

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(uri, "application/vnd.apple.mpegurl");
		startActivity(i);
	}

	/**
	 * Player部分のサイズを変更
	 * @param expand
	 */
	public void expandPlayer(boolean expand) {

		mPlayerExpanded = expand;

		mPlayer.setAutoFullScreen(expand);

		mPlayerClose.setVisibility(expand ? View.GONE : View.VISIBLE);

		mPlayer.setVisibility(View.VISIBLE);
		mPlayer.showToolbar(expand);
		mWebViewContainer.setVisibility(expand ? View.GONE : View.VISIBLE);

		updatePlayerContainerSize();
	}

	/**
	 * 画面の向きに合わせてレイアウトを変更する
	 */
	void updateMainContainer() {

		updatePlayerContainerSize();

		switch (getResources().getConfiguration().orientation) {
		case Configuration.ORIENTATION_LANDSCAPE:
			mMainContainer.setOrientation(LinearLayout.HORIZONTAL);
			break;
		default:
			mMainContainer.setOrientation(LinearLayout.VERTICAL);
			break;
		}
	}

	/**
	 * 分割表示痔のPlayerの幅と高さを設定
	 */
	void updatePlayerContainerSize() {
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mPlayer.getLayoutParams();
		switch (getResources().getConfiguration().orientation) {
		case Configuration.ORIENTATION_LANDSCAPE:
			lp.width = mPlayerExpanded
					? LayoutParams.MATCH_PARENT
					: getResources().getDisplayMetrics().widthPixels / 3;
			lp.height = LayoutParams.MATCH_PARENT;
			break;
		default:
			lp.width = LayoutParams.MATCH_PARENT;
			lp.height = mPlayerExpanded
				? LayoutParams.MATCH_PARENT
				: getResources().getDisplayMetrics().heightPixels / 3;
			break;
		}
	}

	/**
	 * ガラポンTVのページ内のタイトルっぽい部分を取得
	 */
	void extractTitle() {
		mTitleExtracting = true;
		String script = "javascript:"
				+ "var page=document.getElementsByClassName('ui-page-active');"
				+ "var titles=page[0].getElementsByClassName('ui-title');"
				+ "var title=titles[0].textContent;"
				//+ "alert(title);"
				+ "document.title=title;";
		loadUrl(script);
	}

	void loadUrl(String url) {
		switchPage(PAGE_WEB);
		mWebView.loadUrl(url);
	}


	/**
	 * ログイン
	 */
	void loginBackground() {
		if (mLoginTask != null) {
			return;
		}

		mLoginTask = new ProgressDialogTask(this) {
			@Override
			protected Object doInBackground(Object... params) {
				try {
					HashMap<String,String> cookies = GaraponClientUtils.loginWeb();

					return cookies;
				} catch (Throwable e) {
					e.printStackTrace();
					return e;
				}
			}

			@Override
			protected void onPostExecute(Object result) {
				mLoginTask = null;

				if (result instanceof Throwable) {
					// ログインエラー時はダイアログ表示
					ErrorDialogFragment.show(getFragmentManager(), (Throwable)result);
				}
				else if (result instanceof HashMap<?,?>) {

					@SuppressWarnings("unchecked")
					HashMap<String,String> setCookies = (HashMap<String,String>)result;

					CookieManager cm = CookieManager.getInstance();

					Uri.Builder builder = new Uri.Builder();

					cm.removeAllCookie();
					for (String key: setCookies.keySet()) {
						builder.appendQueryParameter(key, setCookies.get(key));
						cm.setCookie(Prefs.getBaseUrl(), key + "=;");
						cm.setCookie(Prefs.getBaseUrl(), key + "=" + setCookies.get(key) +";");
					}

					CookieSyncManager.getInstance().sync();

//					Log.v(TAG, "cookie\n old:" + cookies + "\n new:" + newCookies);
//					mWebView.loadDataWithBaseURL(mBaseUrl,
//							"<html><body><script>alert(document.cookie);</script></body></html>",
//							"text/html", "utf-8", null);

					if (mReloadAfterLogin && mWebView.getUrl().contains("/auth")) {
						mReloadAfterLogin = false;
					}

					if (mReloadAfterLogin) {
						mReloadAfterLogin = false;
						mWebView.reload();
					} else {
						loadUrl(Prefs.getBaseUrl());
					}
				}
			}

			@Override
			protected void onCancelled() {
				mLoginTask = null;
			}

		};
		mLoginTask.setMessage(R.string.loginProgress);
		mLoginTask.execute();
	}

	void switchPage(int page) {
		mPage = page;

		mSummaryPage.setVisibility(page == PAGE_SUMMARY ? View.VISIBLE : View.GONE);
		mWebView.setVisibility(page == PAGE_WEB ? View.VISIBLE : View.GONE);
		mSearchPage.setVisibility(page == PAGE_SEARCH ? View.VISIBLE : View.GONE);
	}

	public void search(SearchParam searchParam) {
		switchPage(PAGE_SEARCH);

		SearchResultFragment f = SearchResultFragment.newInstance(
				searchParam);

		getFragmentManager().beginTransaction()
		.addToBackStack("search")
		.replace(R.id.searchResultFragment, f)
		.commit();
	}
}
