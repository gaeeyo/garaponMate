package jp.syoboi.android.garaponmate.fragment;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.util.HashMap;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.fragment.base.MainBaseFragment;
import jp.syoboi.android.garaponmate.task.ProgressDialogTask;

public class GaraponWebFragment extends MainBaseFragment {

	static final String TAG = "GaraponWebFragment";

	static final int FLAG_AUTO_LOGIN_PROGRESS = 1;


	Handler				mHandler = new Handler();
	int					mFlags;
	boolean				mTitleExtracting;
	boolean				mReloadAfterLogin;
	ProgressBar			mProgress;
	WebView				mWebView;
	ProgressDialogTask	mLoginTask;
	boolean				mIsWebViewAvailable;
	boolean				mRestore;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_garapon_web, null);
		mWebView = (WebView) v.findViewById(R.id.webView);
		mWebView.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!mRestore && TextUtils.isEmpty(mWebView.getUrl())) {
					reload();
				}
			}
		});
		mProgress = (ProgressBar) v.findViewById(R.id.progress);
		mIsWebViewAvailable = true;
		return v;
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mProgress.setMax(100);

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSaveFormData(false);
		webSettings.setSavePassword(false);
		webSettings.setDomStorageEnabled(true);

		mWebView.setWebViewClient(new WebViewClient() {

//			@Override
//			public void onLoadResource(WebView view, String url) {
//				Log.v(TAG, "onLoadResource url:" + url);
//				super.onLoadResource(view, url);
//			}

//			@Override
//			public void onPageStarted(WebView view, String url, Bitmap favicon) {
//				Log.v(TAG, "onPageStarted url:" + url);
//				super.onPageStarted(view, url, favicon);
//			}

//			@Override
//			public void onPageFinished(WebView view, String url) {
//				Log.v(TAG, "onPageFinished url:" + url);
//				super.onPageFinished(view, url);
////				if (mResumed && !mTitleExtracting) {
////					extractTitle();
////				}
//			}

			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view,
					String url) {
				Log.v(TAG, "shouldInterceptRequest url:" + url);
				if (isResumed()) {
					if (overrideUrl(url)) {
						return super.shouldInterceptRequest(view, null);
					}
				}

				return super.shouldInterceptRequest(view, url);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.v(TAG, "shouldOverrideUrlLoading url:" + url);
				if (isResumed()) {
					if (overrideUrl(url)) {
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

		mRestore = false;
		if (savedInstanceState != null) {
			mRestore = true;
			mWebView.restoreState(savedInstanceState);
		}
	}

	@Override
	public void onDestroyView() {
		mIsWebViewAvailable = false;
		if (mLoginTask != null) {
			mLoginTask.cancel(true);
			mLoginTask = null;
		}
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if (mWebView != null) {
			mWebView.destroy();
			mWebView = null;
		}
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mWebView.saveState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();
		mWebView.onPause();
	}


	@Override
	public void onResume() {
		mWebView.onResume();
		super.onResume();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_web, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.reload:
			if (TextUtils.isEmpty(mWebView.getUrl())) {
				reload();
			} else {
				mWebView.reload();
			}
			break;
		case R.id.topPage:
			reload();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void reload() {
		navigate(Prefs.getBaseUrl());
	}

	void navigate(String url) {
		if (mWebView != null) {
			Log.v(TAG, "navigate url:" + url);
			mWebView.loadUrl(url);
		}
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
	void loadUrl(String url) {
		mWebView.loadUrl(url);
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
	 * ログイン
	 */
	void loginBackground() {
		if (mLoginTask != null) {
			return;
		}

		mLoginTask = new ProgressDialogTask(getActivity()) {
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


}
