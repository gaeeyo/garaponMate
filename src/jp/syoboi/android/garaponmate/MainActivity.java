package jp.syoboi.android.garaponmate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.garaponmate.view.PlayerView;

public class MainActivity extends Activity  {

	static final String TAG = "MainActivity";

	static final int FLAG_AUTO_LOGIN_PROGRESS = 1;
	static final int [] FAV_BUTTONS = { R.id.fav0, R.id.fav1, R.id.fav2 };
	static final long CHANGE_FULLSCREEN_DELAY = 3000;

	String 		mBaseUrl;
	String 		mUser;
	String 		mPass;
	int			mFlags;

	Handler			mHandler = new Handler();
	LinearLayout	mMainContainer;
	PlayerView		mPlayer;
	View			mPlayerOverlay;
	View			mWebViewContainer;
	WebView			mWebView;
	ProgressBar		mProgress;
	boolean			mPlayerExpanded;
	boolean			mFullScreen;

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

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		SharedPreferences prefs = getPrefs();
		prefs.registerOnSharedPreferenceChangeListener(mPrefsChangeListener);
		reloadSettings();

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
				final int FS_FLAGS = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
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
				if (url.endsWith("/auth/login.garapon")) {
					// ログイン画面に遷移するとき、自動ログインする
					if ((mFlags & FLAG_AUTO_LOGIN_PROGRESS) == 0) {
						login();
						return true;
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

	@Override
	protected void onDestroy() {
		SharedPreferences prefs = getPrefs();
		prefs.unregisterOnSharedPreferenceChangeListener(mPrefsChangeListener);

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
			login();
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
		// ページが戻れる状態だったら戻る
		if (mWebView.canGoBack()) {
			mWebView.goBack();
			return;
		}
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

	/**
	 * ログイン
	 */
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
	 * ガラポンTVのセッションを取得
	 * @return
	 */
	String getSession() {
		String cookies = CookieManager.getInstance().getCookie(mBaseUrl);

		Matcher m = Pattern.compile("GaraponAuthKey=([0-9a-z]+)").matcher(cookies);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	/**
	 * Playerで再生
	 * @param id
	 */
	void setPlayerPage(String id) {
		Log.v(TAG, "setPlayerPage id:" + id);
		String sessionId = getSession();
		String flv = id.substring(6,8) + "/" + id + ".ts-" + sessionId;
		mPlayer.setVideo(mBaseUrl, flv);

		expandPlayer(false);
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
		final int FS_FLAGS = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

		int systemUiVisibility = mPlayer.getSystemUiVisibility();
		if (fullscreen) {
			systemUiVisibility |= FS_FLAGS;
			mFullScreen = true;
			mPlayer.showToolbar(false);
		} else {
			systemUiVisibility &= FS_FLAGS;
			mFullScreen = false;
			mPlayer.showToolbar(true);
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
		String script = "javascript:"
				+ "var page=document.getElementsByClassName('ui-page-active');"
				+ "var titles=page[0].getElementsByClassName('ui-title');"
				+ "var title=titles[0].textContent;"
				//+ "alert(title);"
				+ "document.title=title;";
		mWebView.loadUrl(script);
	}

	/**
	 * お気に入りを開く
	 * @param tag
	 * @return
	 */
	boolean navigateFav(String tag) {
		String url = getPrefs().getString(tag + "url", "");
		if (!TextUtils.isEmpty(url)) {
			mWebView.loadUrl(url);
			return true;
		}
		return false;
	}

	/**
	 * お気に入りに保存(登録)
	 * @param tag
	 * @param title
	 * @param url
	 */
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
}
