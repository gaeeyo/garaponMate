package jp.syoboi.android.garaponmate.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.view.PlayerView.PlayerInterface;

public class PlayerWebView implements PlayerInterface {
	private static final String TAG = "PlayerWebView";

	WebView			mWebView;
	int				mEmbedToolbarHeight;
	boolean			mPlaying;
	boolean			mPlayOnResume;

	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
	public PlayerWebView(Context context) {
		mWebView = new WebView(context);

		mWebView.setBackgroundColor(0xff000000);

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setPluginState(PluginState.ON);

		mWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				Log.v(TAG, "onConsoleMessage msg:" + consoleMessage.message());
				return super.onConsoleMessage(consoleMessage);
			}
		});

//		// オーバーレイの下の15dp分を開けておく(FlasyPlayerのツールバー部分)
//		LayoutParams overlayLp = new LayoutParams(
//				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
//		overlayLp.bottomMargin = mEmbedToolbarHeight;

//		mOverlay.setBackgroundColor(0x880000ff);
//		mOverlay.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//
//			}
//		});

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
		playerCtrl(true, "player:jsPlay", "");
	}

	@Override
	public void stop() {
		mPlaying = false;
		playerCtrl(true, "player:jsStop", "");
	}

	@Override
	public void pause() {
		mPlaying = false;
		playerCtrl(true, "player:jsPause", "");
	}

	@Override
	public void jump(int sec) {
		if (sec > 0) {
			playerCtrl(true, "player:jsSetFastForward", String.valueOf(sec));
		} else {
			playerCtrl(true, "player:jsSetRewind", String.valueOf(-sec));
		}
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
		Log.v(TAG, "script:" + script);

		mWebView.loadUrl(script);
	}

	/**
	 *
	 * @param vol 0～200 (default:100)
	 */
	public void setVolume(int vol) {
		playerCtrl(true, "player:jsVolume", String.valueOf(vol));
	}

	public void getVolume() {
		playerCtrl(false, "player:jsVolume");
	}

	public void setPos(int pos) {
		playerCtrl(true, "player:jsSetPosition", String.valueOf(pos));
	}

	private void setVideoInternal(String id) {
		String flvPath = id.substring(6,8) + "/" + id + ".ts-" + Prefs.getCommonSessionId();

		String rtmp = "rtmp://" + Prefs.getIpAdr() + ":" + Prefs.getTsPort() + "/";

		String html = "<html>"
				+ "<meta name='viewport' content='target-densitydpi=low-dpi' />"
				+ "<style type='text/css'>"
				+ "body { padding:0; margin:0; background:#000; color:#fff; text-align:center; width:100%; }"
				+ "#player { text-align:center; width:100%; height:100%; }"
				+ "</style>"
				+ "<body>"
				+ "<object id='player' type='application/x-shockwave-flash' data='/swf/fp/player_flv_maxi.swf?20120413'>"
				//+ "<param name='allowFullScreen' value='true'>"
				+ "<param name='allowScriptAccess' value='always'>"
				+ "<param name='flashvars' value='arg1=val"
					+ "&amp;flv=" + flvPath
					+ "&amp;netconnection=" + rtmp
					+ "&amp;showstop=0"
					//+ "&amp;showvolume=1"
					//+ "&amp;showtime=2"
					+ "&amp;showtime=2"
					//+ "&amp;showfullscreen=1"
					//+ "&amp;margin=1"
					+ "&amp;margin=0"
					+ "&amp;bgcolor=000000"
					+ "&amp;bgcolor1=000000"
					+ "&amp;bgcolor2=000000"
					+ "&amp;playercolor=000000"
					+ "&amp;loadingcolor=0aff17"
					+ "&amp;buffershowbg=0"
					+ "&amp;onclick=none"
					+ "&amp;ondoubleclick=none"
					//+ "&amp;showiconplay=1"
					+ "&amp;showiconplay=0"
					+ "&amp;buttoncolor=aaaaaa"
					+ "&amp;iconplaycolor=aaaaaa"
					+ "&amp;videobgcolor=000000"
					+ "&amp;sliderovercolor=ffffff"
					+ "&amp;slidercolor1=aaaaaa"
					+ "&amp;slidercolor2=aaaaaa"
					+ "&amp;buttonovercolor=aaaaaa"
					+ "&amp;showplayer=always"
					+ "&amp;showloading=always"
					+ "&amp;autoload=1"
					+ "&amp;autoplay=1"
					+ "'>"
				+ "</object>"
				//+ "<div style='background:#000; width:1px; height:100%; position:absolute; top:0px; right:0px; overflow:hidden;'>&nbsp;</div>"
				+ "<script type='text/javascript'>"
				+ "var player=document.getElementById('player');"
				+ "</script>"
				+ "</body>"
				+ "</html>"
				;
		mWebView.loadDataWithBaseURL(Prefs.getBaseUrl(), html, "text/html", "UTF-8", null);
	}

	@Override
	public void seek(int sec) {

	}

	@Override
	public int getDuration() {
		return 0;
	}

	@Override
	public int getCurrentPos() {
		return 0;
	}
}
