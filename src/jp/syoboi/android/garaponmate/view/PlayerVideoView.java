package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.view.View;
import android.widget.VideoView;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.view.PlayerView.PlayerInterface;

public class PlayerVideoView implements PlayerInterface {
	private static final String TAG = "PlayerVideoView";

	VideoView	mVideoView;
	boolean		mPause;
	boolean		mStarted;
	boolean		mStopped = true;
	int			mDuration;
	int			mCurPos;
	int			mSeekPos;
	boolean		mSeeking;
	String		mPendingId;

	public PlayerVideoView(Context context) {
		mVideoView = new VideoView(context) {
		};

		mVideoView.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
			}
		});

		mVideoView.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {

				if (mPendingId != null) {
					mPause = false;
					mStarted = false;
					String id = mPendingId;
					mPendingId = null;
					setVideo(id);
					return;
				}
				mp.setOnSeekCompleteListener(new OnSeekCompleteListener() {
					@Override
					public void onSeekComplete(MediaPlayer mp) {
						mSeeking = false;
						if (mSeekPos != 0) {
							int seekPos = mSeekPos;
							mSeekPos = 0;
							seek(seekPos);
						}
					}
				});
				mp.setScreenOnWhilePlaying(true);
				mDuration = mVideoView.getDuration();

				play();
			}
		});
	}

	@Override
	public void play() {
		mVideoView.start();
		mStarted = true;
		mPause = false;
	}

	@Override
	public void stop() {
		mPause = true;
		if (mVideoView.isPlaying()) {
			mVideoView.stopPlayback();
		}
	}

	@Override
	public void pause() {
		mPause = true;
		if (mVideoView.isPlaying()) {
			mVideoView.pause();
		}
	}

	@Override
	public void setVideo(final String id) {
		stop();

		mPause = false;
		mDuration = 0;
		mStarted = false;
		mStopped = false;
		Uri uri = Uri.parse("http://" + Prefs.getGaraponHost()
				+ "/cgi-bin/play/m3u8.cgi?"
				+ id + "-" + Prefs.getCommonSessionId());
		mVideoView.setVideoURI(uri);
	}

	@Override
	public void onPause() {
		mCurPos = mVideoView.getCurrentPosition();
		mVideoView.suspend();
	}

	@Override
	public void onResume() {
		mVideoView.resume();
		if (mCurPos != 0) {
			mVideoView.seekTo(mCurPos);
			mCurPos = 0;
		}
	}

	@Override
	public void destroy() {
		mVideoView.stopPlayback();
	}

	@Override
	public void jump(int sec) {
		if (!mSeeking) {
			mSeeking = true;
			int curPos = mVideoView.getCurrentPosition();
			if (sec > 0) {
				mCurPos = curPos + sec * 1000;
				if (mVideoView.canSeekForward()) {
					mVideoView.seekTo(mCurPos);
				}
			} else {
				mCurPos = curPos + sec * 1000;
				if (mVideoView.canSeekBackward()) {
					mVideoView.seekTo(mCurPos);
				}
			}
		} else {
			mSeekPos = mCurPos + sec * 1000;
		}
	}

	@Override
	public View getView() {
		return mVideoView;
	}

	@Override
	public void seek(int msec) {
		if (!mSeeking) {
			mSeeking = true;
			mCurPos = msec;
			mVideoView.seekTo(msec);
		} else {
			mSeekPos = msec;
		}
	}

	@Override
	public int getDuration() {
		return mVideoView.getDuration();
	}

	@Override
	public int getCurrentPos() {
		if (mSeeking) {
			return mCurPos;
		}
		return mVideoView.getCurrentPosition();
	}
}
