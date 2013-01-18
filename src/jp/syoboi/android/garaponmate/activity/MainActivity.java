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
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.garaponmate.GaraponClient.Program;
import jp.syoboi.android.garaponmate.GaraponClientUtils;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.fragment.ErrorDialogFragment;
import jp.syoboi.android.garaponmate.fragment.SummaryFragment;
import jp.syoboi.android.garaponmate.task.ProgressDialogTask;
import jp.syoboi.android.garaponmate.view.PlayerView;

public class MainActivity extends Activity  {

	static final String TAG = "MainActivity";

	static final int FLAG_AUTO_LOGIN_PROGRESS = 1;
	static final int [] FAV_BUTTONS = { R.id.fav0, R.id.fav1, R.id.fav2 };
	static final long CHANGE_FULLSCREEN_DELAY = 3000;

	static final String SPECIAL_PAGE_PATH = "/garaponMate";

	static final int PAGE_SUMMARY = 0;
	static final int PAGE_WEB = 1;

	int				mFlags;

	Handler			mHandler = new Handler();
	LinearLayout	mMainContainer;
	PlayerView		mPlayer;
	View			mPlayerOverlay;
	View			mWebViewContainer;
	WebView			mWebView;
	ProgressBar		mProgress;
	View			mSummaryPage;
	boolean			mPlayerExpanded;
	boolean			mFullScreen;
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
			default:
				Object tag = v.getTag();
				if (tag instanceof String) {
					String tagName = tag.toString();
					if (tagName.startsWith("fav")) {
						if (!navigateFav((String)v.getTag())) {
							loadUrl(Prefs.getBaseUrl());
						}
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
		reloadSettings();

		mSummaryPage = findViewById(R.id.summary);

		int favIndex = 0;
		for (int id: FAV_BUTTONS) {
			View v = findViewById(id);
			v.setTag("fav" + (favIndex++));
			v.setOnClickListener(mOnClickListener);
			v.setOnLongClickListener(mOnLongClickListener);
		}
		findViewById(R.id.special).setOnClickListener(mOnClickListener);
		findViewById(R.id.settings).setOnClickListener(mOnClickListener);
		findViewById(R.id.settings).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				loadUrl(Prefs.getBaseUrl());
				return true;
			}
		});

		mMainContainer = (LinearLayout) findViewById(R.id.mainContainer);
		mPlayer = (PlayerView) findViewById(R.id.player);
		mWebViewContainer = findViewById(R.id.webViewContainer);
		mWebView = (WebView) findViewById(R.id.webView);

		mProgress = (ProgressBar) findViewById(R.id.progress);
		mProgress.setMax(100);

		updateMainContainer();

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSaveFormData(false);
		webSettings.setSavePassword(false);
		webSettings.setDomStorageEnabled(true);

		mPlayer.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
			@Override
			public void onSystemUiVisibilityChange(int visibility) {
				final int FS_FLAGS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
				if (mFullScreen) {
					if ((visibility & FS_FLAGS) == 0) {
						// フルスクリーンが解除された
						cancelFullScreen();
						startFullScreenDelay();
					}
				}

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
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mFullScreen) {
							touching = true;
							cancelFullScreen();
						} else {
							startFullScreenDelay();
						}
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						startFullScreenDelay();
						if (touching) {
							touching = false;
							return true;
						}
						break;
					}
					return touching;
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
		switchPage(PAGE_SUMMARY);
	}

	boolean overrideUrl(String url) {
		if (url.contains("/viewer/player.garapon?gtvid=")) {
			final String id = getGtvIdFromUrl(url, "gtvid");
			if (id != null) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
//						mWebView.stopLoading();
						setPlayerPage(id);
					}
				});
				return true;
			}
		}
		if (url.contains("/main.garapon")) {
			if ((mFlags & FLAG_AUTO_LOGIN_PROGRESS) != 0) {
				// ログインが完了したら fav0 を開く
				mFlags &= ~FLAG_AUTO_LOGIN_PROGRESS;
				if (navigateFav("fav0")) {
					return true;
				}
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
						setPlayerPage(id);
					}
				});
				return true;
			}
		}
		// site.garapon.tv の画像をローカルで開く
		if (url.contains("/play?")) {
			final String id = getGtvIdFromUrl(url, "gtvid");
			setPlayerPage(id);
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
			reloadSettings();
			loginBackground();
		}
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
		if (mFullScreen) {
			cancelFullScreen();
			startFullScreenDelay();
			return;
		}
		// プレイヤーが拡大されていたら縮小
		if (mPlayerExpanded) {
			expandPlayer(false);
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

	/**
	 * 設定をリロード
	 */
	void reloadSettings() {
		mSettingChanged = false;

		// お気に入りボタンのラベルに反映
		int idx = 0;
		for (int id: FAV_BUTTONS) {
			TextView tv = (TextView) findViewById(id);
			String title = Prefs.getFavTitle(idx);
			if (TextUtils.isEmpty(title)) {
				title = "fav" + idx;
			}
			tv.setText(title);
			idx++;
		}
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
	public void setPlayerPage(String id) {
		Log.v(TAG, "setPlayerPage id:" + id);
		mPlayer.setVideo(id);

		expandPlayer(false);

		Fragment f = getFragmentManager().findFragmentById(R.id.summary);
		if (f instanceof SummaryFragment) {
			((SummaryFragment) f).setSelected(id);
		}
	}

	public void setPlayerPage(Program p) {
		setPlayerPage(p.gtvid);
	}

	/**
	 * Player部分のサイズを変更
	 * @param expand
	 */
	void expandPlayer(boolean expand) {

		mPlayerExpanded = expand;

		mPlayer.setVisibility(View.VISIBLE);
		mPlayer.showToolbar(expand);
		mWebViewContainer.setVisibility(expand ? View.GONE : View.VISIBLE);

		updatePlayerContainerSize();

		if (expand) {
			startFullScreenDelay();
		} else {
			cancelFullScreen();
		}
	}

	Runnable mChangeFullScreenRunnable = new Runnable() {
		@Override
		public void run() {
			setFullScreen(true);
		};
	};

	/**
	 * フルスクリーンへの自動的な移行を開始
	 */
	void startFullScreenDelay() {
		mHandler.removeCallbacks(mChangeFullScreenRunnable);
		mHandler.postDelayed(mChangeFullScreenRunnable, CHANGE_FULLSCREEN_DELAY);
	}

	/**
	 * フルスクリーンへの移行をキャンセル
	 */
	void cancelFullScreen() {
		mHandler.removeCallbacks(mChangeFullScreenRunnable);
		if (mFullScreen) {
			setFullScreen(false);
		}
	}

	/**
	 * フルスクリーン設定
	 * @param fullscreen
	 */
	void setFullScreen(boolean fullscreen) {
		int FS_FLAGS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

		int systemUiVisibility = mPlayer.getSystemUiVisibility();
		if (fullscreen) {
			systemUiVisibility |= FS_FLAGS;
			mFullScreen = true;
			mPlayer.showToolbar(false);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			systemUiVisibility &= FS_FLAGS;
			mFullScreen = false;
			mPlayer.showToolbar(true);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		mPlayer.setSystemUiVisibility(systemUiVisibility);
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

	/**
	 * お気に入りを開く
	 * @param tag
	 * @return
	 */
	boolean navigateFav(String tag) {
		int idx = favTagToIndex(tag);
		if (idx != -1) {
			String url = Prefs.getFavUrl(idx);
			if (!TextUtils.isEmpty(url)) {
				loadUrl(url);
				return true;
			}
		}
		return false;
	}

	void loadUrl(String url) {
		switchPage(PAGE_WEB);
		mWebView.loadUrl(url);
	}

	/**
	 * お気に入りに保存(登録)
	 * @param tag
	 * @param title
	 * @param url
	 */
	void saveFav(String tag, String title, String url) {
		int idx = favTagToIndex(tag);
		if (idx != -1) {
			Prefs.setFav(idx, title, url);

			String msg = "★" + title + "\n"
					+ "URL:" + url;
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
			reloadSettings();
		}
	}

	int favTagToIndex(String tag) {
		Matcher m = Pattern.compile("\\d+").matcher(tag);

		if (m.find()) {
			return Integer.valueOf(m.group());
		}
		return -1;
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
					ErrorDialogFragment.newInstance(
							getString(R.string.error),
							(Throwable)result)
					.show(getFragmentManager(), "dialog");
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
						if (!navigateFav("fav0")) {
							loadUrl(Prefs.getBaseUrl());
						}
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
		mWebView.setVisibility(mSummaryPage.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
	}
}
