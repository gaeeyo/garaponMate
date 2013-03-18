package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;

public class PlayerVideoView implements PlayerViewInterface {
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
	PlayerViewCallback	mCallback;
	Resources	mResources;
	MediaPlayer	mMediaPlayer;

	public PlayerVideoView(Context context, PlayerViewCallback callback) {
		mResources = context.getResources();
		mVideoView = new VideoView(context) {
			@Override
			protected void onSizeChanged(int w, int h, int oldw, int oldh) {
				Log.v(TAG, "onSizeChanged w:" + w + " h:" + h);
				super.onSizeChanged(w, h, oldw, oldh);
			}
		};
		mCallback = callback;

		mVideoView.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				mPause = true;
			}
		});

		mVideoView.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				mMediaPlayer = mp;

				if (mPendingId != null) {
					mPause = false;
					mStarted = false;
					String id = mPendingId;
					mPendingId = null;
					setVideo(id);
					return;
				}

				if (mCallback != null) {
					mCallback.onMessage(null);
				}

				mp.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
					@Override
					public void onBufferingUpdate(MediaPlayer mp, int percent) {
						if (mCallback != null) {
							if (percent == 100) {
								mCallback.onMessage(null);
							} else {
								mCallback.onMessage(mResources.getString(R.string.bufferingFmt, percent));
							}
						}
					}
				});

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

		mVideoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				String whatMsg;
				switch (what) {
				case MediaPlayer.MEDIA_ERROR_UNKNOWN:
					whatMsg = "MEDIA_ERROR_UNKNOWN";
					break;
				case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
					whatMsg = "MEDIA_ERROR_SERVER_DIED";
					break;
				default:
					whatMsg = "what:" + what;
					break;
				}
				String extraMsg;
				switch (extra) {
				case MediaPlayer.MEDIA_ERROR_IO:
					extraMsg = "MEDIA_ERROR_IO";
					break;
				case MediaPlayer.MEDIA_ERROR_MALFORMED:
					extraMsg = "MEDIA_ERROR_MALFORMED";
					break;
				case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
					extraMsg = "MEDIA_ERROR_UNSUPPORTED";
					break;
				case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
					extraMsg = "MEDIA_ERROR_TIMED_OUT";
					break;
				default:
					extraMsg = "extra:" + extra;
					break;
				}

				mCallback.onMessage("ERROR:\nwhat:" + whatMsg + "\n" + extraMsg);
				return true;
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

		mCallback.onMessage(mResources.getString(R.string.loading));
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
		if (mStarted) {
			mVideoView.suspend();
		}
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
		mCallback = null;
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

	@Override
	public void setSound(String lr) {
		if (mMediaPlayer != null) {
			if ("L".equals(lr)) {
				mMediaPlayer.setVolume(1, 0);
			}
			else if ("R".equals(lr)) {
				mMediaPlayer.setVolume(0, 1);
			}
			else {
				mMediaPlayer.setVolume(1, 1);
			}
		}
	}

	@Override
	public boolean isSetSoundAvailable() {
		return false;
	}
}
