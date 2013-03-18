package jp.syoboi.android.garaponmate.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;

/**
 * カスタマイズしたFlashのMediaPlayerを使った PlayerView
 * @author naofumi
 *
 */
public class PlayerWebView2 implements PlayerViewInterface {
	private static final String TAG = "PlayerWebView2";

	private static String sTemplate;

	WebView			mWebView;
	int				mEmbedToolbarHeight;
	boolean			mPlaying;
	boolean			mPlayOnResume;
	String			mJsObjId;
	PlayerViewCallback	mCallback;
	int				mPos;
	int				mDuration;

	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
	public PlayerWebView2(Context context, PlayerViewCallback callback) {
		mWebView = new WebView(context) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				return true;
			}
		};
		mCallback = callback;

		mJsObjId = "obj" + System.currentTimeMillis();

		if (sTemplate == null) {
			StringWriter sw = new StringWriter();
			try {
				InputStreamReader is = new InputStreamReader(
						context.getAssets().open("player_web_view2.html"),
						"utf-8");
				try {
					char [] buf = new char [2048];
					int readSize;
					while ((readSize = is.read(buf)) > 0) {
						sw.write(buf, 0, readSize);
					}
					sTemplate = sw.toString();
				} finally {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		mWebView.setBackgroundColor(0xff000000);
		mWebView.setHorizontalScrollbarOverlay(true);
		mWebView.setVerticalScrollbarOverlay(true);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.setVerticalScrollBarEnabled(false);

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setPluginState(PluginState.ON);
		webSettings.setUseWideViewPort(true);

		mWebView.addJavascriptInterface(new JsObj(), mJsObjId);

		mWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				Log.v(TAG, "onConsoleMessage msg:" + consoleMessage.message());
				return super.onConsoleMessage(consoleMessage);
			}
		});
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view,
					String url) {
				if (url.contains("/player_flv_js.swf")) {
					InputStream is;
					try {
						is = view.getContext().getAssets().open("player_flv_js.swf");
						return new WebResourceResponse("application/x-shockwave-flash", null, is);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return super.shouldInterceptRequest(view, url);
			}
		});

		float density = context.getResources().getDisplayMetrics().density;
		mEmbedToolbarHeight = Math.round((20) * density);


	}

	@Override
	public View getView() {
		return mWebView;
	}

	@Override
	public void setVideo(String id) {
		mPlaying = true;
		setVideoInternal(id);
	}

	@Override
	public void play() {
		mPlaying = true;
		playerCtrl(true, "method:play", "");
	}

	@Override
	public void stop() {
		mPlaying = false;
		playerCtrl(true, "method:stop", "");
	}

	@Override
	public void pause() {
		mPlaying = false;
		playerCtrl(true, "method:pause", "");
	}

	@Override
	public void jump(int sec) {
		playerCtrl(true, "method:seek", String.valueOf(sec));
	}



	@Override
	public void onPause() {
		if (mPlaying) {
			mPlayOnResume = true;
			pause();
		} else {
			mPlayOnResume = false;
		}
		mWebView.onPause();
	}

	@Override
	public void onResume() {
		mWebView.onResume();
		if (mPlayOnResume) {
			play();
		}
	}

	@Override
	public void destroy() {
		mWebView.destroy();
		mWebView = null;
	}

	void playerCtrl(boolean set, String... params) {
		StringBuilder sb = new StringBuilder();
		sb.append("javascript:console.log(player.");
		if (set) {
			sb.append("SetVariable(");
		} else {
			sb.append("GetVariable(");
		}

		boolean paramAdded = false;
		for (String name: params) {
			if (paramAdded) {
				sb.append(',');
			}
			sb.append('"').append(name).append('"');
			paramAdded = true;
		}
		sb.append("))");

		String script = sb.toString();
		Log.v(TAG, "script: " + script);

		mWebView.loadUrl(script);
	}

	/**
	 *
	 * @param vol 0～200 (default:100)
	 */
	public void setVolume(int vol) {
		playerCtrl(true, "method:setVolume", String.valueOf(vol));
	}

	public void getVolume() {
		playerCtrl(false, "method:setVolume");
	}

	public void setPos(int pos) {
		playerCtrl(true, "method:setPosition", String.valueOf(pos));
	}

	private void setVideoInternal(String id) {
		String flvPath = id.substring(6,8) + "/" + id + ".ts-" + Prefs.getCommonSessionId();

		String rtmp = "rtmp://" + Prefs.getGaraponTsHost() + "/";

		String html = sTemplate.replace("\"{JSOBJ}\"", mJsObjId)
				.replace("{RTMP}", rtmp)
				.replace("{FLV}", flvPath);
		mWebView.loadDataWithBaseURL(Prefs.getBaseUrl(),
				html, "text/html", "UTF-8", null);
	}

	@Override
	public void seek(int msec) {
//		// 正確にシークできないので10秒で丸める
//		int sec = msec / 1000;
//		setPos((sec / 10) * 10);
		setPos(msec);
	}

	@Override
	public int getDuration() {
		return mDuration;
	}

	@Override
	public int getCurrentPos() {
		return mPos;
	}

	public class JsObj {
		int mBuffPos = -1;
		public void reportPos(String pos) {
			if (App.DEBUG) {
				Log.v(TAG, String.format("pos:%s", pos));
			}
			try {
				mPos = (int)Float.parseFloat(pos);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void reportFull(String pos, String duration,
				String bufLen, String bufTime, String isPlaying, String volume) {
			if (App.DEBUG) {
				Log.v(TAG, String.format("pos:%s dur:%s bufLen:%s bufTime:%s play:%s vol:%s",
						pos, duration,
						bufLen, bufTime, isPlaying, volume));
//				Log.v(TAG, String.format("pos:%s dur:%s total:%s load:%s perc:%s bufLen:%s bufTime:%s play:%s vol:%s",
//						pos, duration,
//						total, loaded, percent,
//						bufLen, bufTime, isPlaying, volume));
			}
			try {
				mPos = (int)Float.parseFloat(pos);
				mDuration = (int)Float.parseFloat(duration);

				int bufMax = (int)(Float.parseFloat(bufTime) * 100);
				int bufPos = (int)(Float.parseFloat(bufLen) * 100);
				if (bufPos > bufMax) {
					bufPos = bufMax;
				}
				if (bufPos != mBuffPos) {
					mCallback.onBuffering(bufPos, bufMax);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setSound(String lr) {
		playerCtrl(true, "method:setSound", lr);
	}

	@Override
	public boolean isSetSoundAvailable() {
		return false;
	}
}
