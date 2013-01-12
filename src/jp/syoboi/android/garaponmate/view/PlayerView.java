package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import jp.syoboi.android.garaponmate.R;

public class PlayerView extends RelativeLayout {

	private static final String TAG = "PlayerView";

	private static final int GET_TIME_INTERVAL = 500;

	private static final int [] PLAYER_BUTTONS = { R.id.pause, R.id.previous, R.id.rew, R.id.ff, R.id.next };

	Handler			mHandler = new Handler();
	WebView			mWebView;
	View			mOverlay;
	View			mToolbar;
	ImageButton		mPauseButton;
	int				mEmbedToolbarHeight;
	boolean			mPause;

	public PlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (isInEditMode()) {
			return;
		}

		inflate(context, R.layout.player_controls, this);

		float density = getResources().getDisplayMetrics().density;
		mEmbedToolbarHeight = Math.round((20) * density);

		// ボタン
		mToolbar = findViewById(R.id.playerToolbar);
		for (int id: PLAYER_BUTTONS) {
			findViewById(id).setOnClickListener(mOnClickListener);
		}

		mPauseButton = (ImageButton)findViewById(R.id.pause);

		// PlayerのWebView
		mWebView = (WebView) findViewById(R.id.playerWebView);

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

		// オーバーレイ
		mOverlay = findViewById(R.id.playerViewOverlay);
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

		mHandler.postDelayed(mGetTimeRunnable, GET_TIME_INTERVAL);
	}

	Runnable mGetTimeRunnable = new Runnable() {
		@Override
		public void run() {
			playerCtrl(false, "player:jsSetPosition");
			mHandler.postDelayed(mGetTimeRunnable, GET_TIME_INTERVAL);
		};
	};

	public void showToolbar(boolean show) {
		if (show) {
			mToolbar.setVisibility(View.VISIBLE);
		} else {
			mToolbar.setVisibility(View.GONE);
		}
	}

	View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.pause:
				mPause = !mPause;
				if (mPause) {
					pause();
				} else {
					play();
				}
				updatePauseButton();
				break;
			case R.id.previous:
				jump(-15);
				break;
//			case R.id.rew:
//				jump(-15);
//				break;
//			case R.id.ff:
//				jump(15);
//				break;
			case R.id.next:
				jump(30);
				break;
			};
		}
	};

	void updatePauseButton() {
		mPauseButton.setImageResource(
				mPause
				? R.drawable.ic_media_play
				: R.drawable.ic_media_pause);
	}

	public View getPlayerView() {
		return mWebView;
	}

	public void onPause() {
		mHandler.removeCallbacks(mGetTimeRunnable);
		mWebView.onPause();
	}

	public void onResume() {
		mWebView.onResume();
	}

	public void destroy() {
		if (mWebView != null) {
			mWebView.destroy();
			mWebView = null;
		}
	}

	public void setVideo(String baseUrl, String flv) {
		int pos = baseUrl.indexOf("//");

		String rtmp = "rtmp://" + baseUrl.substring(pos + 2) + "/";
		Log.d(TAG, "flv:" + flv + " " + rtmp);

		String html = "<html>"
				+ "<style type='text/css'>"
				+ "body { padding:0; margin:0; background:#000; color:#fff; text-align:center; width:100%; }"
				+ "#player { text-align:center; width:100%; height:100%; }"
				+ "</style>"
				+ "<body>"
				+ "<object id='player' type='application/x-shockwave-flash' data='/swf/fp/player_flv_maxi.swf?20120413'>"
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
					//+ "&amp;showiconplay=1"
					+ "&amp;showiconplay=0"
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
				+ "var player=document.getElementById('player');"
				+ "</script>"
				+ "</body>"
				+ "</html>"
				;
		mWebView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
	}

	public void jump(int sec) {
		if (sec > 0) {
			playerCtrl(true, "player:jsSetFastForward", String.valueOf(sec));
		} else {
			playerCtrl(true, "player:jsSetRewind", String.valueOf(sec));
		}
	}

	public void play() {
		playerCtrl(true, "player:jsPlay", "");
	}

	public void pause() {
		playerCtrl(true, "player:jsPause", "");
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
}
