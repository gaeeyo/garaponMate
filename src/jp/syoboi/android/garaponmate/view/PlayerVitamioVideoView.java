package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnSeekCompleteListener;
import io.vov.vitamio.widget.VideoView;

import java.io.File;
import java.util.HashMap;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClient;

public class PlayerVitamioVideoView implements PlayerViewInterface {
	private static final String TAG = "PlayerVideoView";

	VideoView	mVideoView;
	boolean		mPause;
	boolean		mStarted;
	boolean		mStopped = true;
	long		mDuration;
	long		mCurPos;
	long		mSeekPos = -1;
	boolean		mSeeking;
	String		mPendingId;
	PlayerViewCallback	mCallback;
	Resources	mResources;
	MediaPlayer	mMediaPlayer;
	float		mSpeed = 1f;

	public PlayerVitamioVideoView(Context context, PlayerViewCallback callback) {
		mResources = context.getResources();
		mVideoView = new MyVideoView(context);
		mVideoView.setBufferSize(128*1024);
		
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
				mp.setPlaybackSpeed(mSpeed);

				if (mCallback != null) {
					mCallback.onMessage(null);
				}

				mp.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
					@Override
					public void onBufferingUpdate(MediaPlayer mp, int percent) {
						if (mCallback != null) {
							if (percent >= 95 || !mp.isBuffering() || mp.isPlaying()) {
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
						if (mSeekPos != -1) {
							long seekPos = mSeekPos;
							mSeekPos = -1;
							mCallback.onMessage(null);
							seek((int)seekPos);
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
//				case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
//					whatMsg = "MEDIA_ERROR_SERVER_DIED";
//					break;
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
		
		Uri uri = Uri.parse("rtmp://" + Prefs.getGaraponTsHost() + "/");
		HashMap<String,String> options = new HashMap<String,String>();
		options.put("rtmp_playpath", GaraponClient.getRTMPPath(id));
//		options.put("rtmp_live","record");
//		options.put("rtmp_buffer","10000");
		
		mVideoView.setVideoURI(uri, options);
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
			long curPos = mVideoView.getCurrentPosition();
			if (sec > 0) {
				mCurPos = curPos + sec * 1000;
//				if (mVideoView.canSeekForward()) {
//				if (mVideoViewcanSeekForward()) {
					mVideoView.seekTo(mCurPos);
//				}
			} else {
				mCurPos = Math.max(0, curPos + sec * 1000);
//				if (mVideoView.canSeekBackward()) {
					mVideoView.seekTo(mCurPos);
//				}
			}
		} else {
			if (mSeekPos == -1) {
//				mVideoView.pause();
				mSeekPos = mCurPos + sec * 1000;
			} else {
				mSeekPos = mSeekPos + sec * 1000;
			}
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
		return (int) mVideoView.getDuration();
	}

	@Override
	public int getCurrentPos() {
		if (mSeeking) {
			if (mSeekPos != -1) {
				return (int) mSeekPos;
			}
			return (int) mCurPos;
		}
		return (int) mVideoView.getCurrentPosition();
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
		return true;
	}
	
	
	static class MyVideoView extends VideoView {
		public MyVideoView(Context context) {
			super(context);
		}
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			final int videoWidth = getVideoWidth();
			final int videoHeight = getVideoHeight();
			
	        int width = getDefaultSize(videoWidth, widthMeasureSpec);
	        int height = getDefaultSize(videoHeight, heightMeasureSpec);
	        if (videoWidth > 0 && videoHeight > 0) {

	            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
	            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
	            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
	            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

	            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
	                // the size is fixed
	                width = widthSpecSize;
	                height = heightSpecSize;

	                // for compatibility, we adjust size based on aspect ratio
	                if ( videoWidth * height  < width * videoHeight ) {
	                    //Log.i("@@@", "image too wide, correcting");
	                    width = height * videoWidth / videoHeight;
	                } else if ( videoWidth * height  > width * videoHeight ) {
	                    //Log.i("@@@", "image too tall, correcting");
	                    height = width * videoHeight / videoWidth;
	                }
	            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
	                // only the width is fixed, adjust the height to match aspect ratio if possible
	                width = widthSpecSize;
	                height = width * videoHeight / videoWidth;
	                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
	                    // couldn't match aspect ratio within the constraints
	                    height = heightSpecSize;
	                }
	            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
	                // only the height is fixed, adjust the width to match aspect ratio if possible
	                height = heightSpecSize;
	                width = height * videoWidth / videoHeight;
	                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
	                    // couldn't match aspect ratio within the constraints
	                    width = widthSpecSize;
	                }
	            } else {
	                // neither the width nor the height are fixed, try to use actual video size
	                width = videoWidth;
	                height = videoHeight;
	                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
	                    // too tall, decrease both width and height
	                    height = heightSpecSize;
	                    width = height * videoWidth / videoHeight;
	                }
	                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
	                    // too wide, decrease both width and height
	                    width = widthSpecSize;
	                    height = width * videoHeight / videoWidth;
	                }
	            }
	        } else {
	            // no size yet, just adopt the given spec sizes
	        }
	        Log.v(TAG, "onMeasure width:" + width + " height:" + height);
	        setMeasuredDimension(width, height);
		}

		@Override
		public void setVideoLayout(int layout, float aspectRatio) {
			getHolder().setFixedSize(getVideoWidth(), getVideoHeight());
		}
	}


	@Override
	public boolean isSpeedAvailable() {
		return false;
	}

	@Override
	public void setSpeed(float speed) {
		mSpeed = speed;
		if (mMediaPlayer != null) {
			mMediaPlayer.setPlaybackSpeed(speed);
		}
	}
}
