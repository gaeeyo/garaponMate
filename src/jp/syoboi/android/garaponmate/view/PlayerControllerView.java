package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.Locale;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.adapter.CaptionAdapter;
import jp.syoboi.android.garaponmate.data.Caption;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.utils.Utils;

public class PlayerControllerView extends FrameLayout {

	private static final String TAG = "PlayerControllerView";

	private static final int [] PLAYER_BUTTONS = { R.id.pause, R.id.previous, R.id.rew, R.id.ff, R.id.next };
	private static final int [] SOUND_ICONS = { R.drawable.ic_sound_s, R.drawable.ic_sound_l, R.drawable.ic_sound_r };
	private static final int INTERVAL = 500;

	ImageButton		mPauseButton;
	View			mCaptionContainer;
	CheckBox		mCaptionSwitch;
	ListView		mCaptionList;
	SeekBar			mSeekBar;
	TextView		mTime;
	TextView		mOsd;
	TextView		mSpeed;
	PlayerView		mPlayer;
	ImageView		mSound;
	Handler			mHandler = new Handler();
	int				mDuration;
	int				mCurPos;
	boolean			mVisible = true;
	Caption[]		mCaptions;
	GestureDetector	mGestureDetector;
	Animation		mOsdCloseAnimation;

	Program			mProgram;
	CaptionAdapter	mCaptionAdapter;
	AudioManager	mAudioManager;
	int				mSoundValue;
	float			mCurSpeed;

	public PlayerControllerView(Context context, AttributeSet attrs, PlayerView pv) {
		super(context, attrs);

		mPlayer = pv;
		inflate(context, R.layout.player_controller, this);

		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);


		mSeekBar = (SeekBar) findViewById(R.id.seekBar);
		mTime = (TextView) findViewById(R.id.time);
		mCaptionSwitch = (CheckBox) findViewById(R.id.captionSwitch);
		mCaptionContainer = findViewById(R.id.captionContainer);
		mCaptionList = (ListView) findViewById(R.id.captionList);
		mPauseButton = (ImageButton)findViewById(R.id.pause);
		mOsd = (TextView) findViewById(R.id.osd);
		mSound = (ImageView) findViewById(R.id.sound);
		mSpeed = (TextView) findViewById(R.id.speed);

		mOsd.setVisibility(View.GONE);

		mCaptionSwitch.setOnClickListener(mOnClickListener);
		mCaptionList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> lv, View view, int position,
					long id) {
				Object obj = lv.getItemAtPosition(position);
				if (obj instanceof Caption) {
					Caption caption = (Caption) obj;
					mPlayer.seek(Math.max(0, caption.time));
				}
			}
		});

		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
//				Log.d(TAG, "onStopTrackingTouch");
				mPlayer.seek(seekBar.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
//				Log.d(TAG, "onStartTrackingTouch");
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
//				Log.d(TAG, "onProgressChanged fromUser:" + fromUser);
				updateTime(progress);
			}
		});

		mSound.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSoundValue++;
				if (mSoundValue >= SOUND_ICONS.length) {
					mSoundValue = 0;
				}
				mSound.setImageResource(SOUND_ICONS[mSoundValue]);
				mPlayer.setSound("SLR".substring(mSoundValue, mSoundValue+1));
			}
		});
		
		mSpeed.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCurSpeed == 1f) {
					mCurSpeed = 1.4f;
				}
				else if (mCurSpeed == 1.4f) {
					mCurSpeed = 1.2f;
				}
				else {
					mCurSpeed = 1f;
				}
				updateSpeed();
				mPlayer.mPlayer.setSpeed(mCurSpeed);
			}
		});

		// ジェスチャーで音量調整
		mGestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {
			final int CONTROL_LIGHT = 1;
			final int CONTROL_VOL = 2;
			int		mControlTarget;
			float	mTotalDistance;
			int		mDistanceUnit = (int)Math.max(1, 16 * getContext().getResources().getDisplayMetrics().density);

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
//				Log.d("", "onSingleTapUp " + mPlayer.isFullScreen());
				if (!mPlayer.isFullScreen()) {
					mPlayer.setFullScreen(true);
				}
				return false;
			}

			@Override
			public void onShowPress(MotionEvent e) {
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {
				//Log.v(TAG, "onScroll distanceY:" + distanceY);

				mTotalDistance += distanceY;

				if (Math.abs(mTotalDistance) < mDistanceUnit) {
					return true;
				}
				int direction = (int)(mTotalDistance / mDistanceUnit);
				mTotalDistance -= direction * mDistanceUnit;

				switch (mControlTarget) {
				case CONTROL_VOL:
					while (direction != 0) {
						mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
								direction > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
								0);
						if (direction > 0) direction--;
						else direction++;
					}

					int vol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
					int volMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
					int volPercent = vol * 100 / volMax;
					showOsd(getContext().getString(R.string.volumeOsd, volPercent));
					break;
				case CONTROL_LIGHT:
					float screenBrightness = mPlayer.getScreenBrightness();
					float newScreenBrightness = screenBrightness + (direction / 20f);
					newScreenBrightness = Math.max(0.02f, Math.min(1, newScreenBrightness));
					if (screenBrightness != newScreenBrightness) {
						int percent = (int)(newScreenBrightness * 100);
						showOsd(getContext().getString(R.string.screenBrightnessOsd, percent));
						mPlayer.setScreenBrightness(newScreenBrightness);
					}
					break;
				}
				return true;
			}

			@Override
			public void onLongPress(MotionEvent e) {
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
				return false;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				int x = (int) e.getX();
				int width = getWidth();
				mControlTarget = 0;
				mTotalDistance = 0;
				if (x < width / 3) {
					mControlTarget = CONTROL_LIGHT;
				} else if (x > width * 2 / 3) {
					mControlTarget = CONTROL_VOL;
				}
//				Log.d("", "onDown " + mPlayer.isFullScreen());
				return true; // mControlTarget != 0;
			}
		});

		for (int id: PLAYER_BUTTONS) {
			findViewById(id).setOnClickListener(mOnClickListener);
		}
		updateIntervalTimer();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!mPlayer.isFullScreen()) {
			return mGestureDetector.onTouchEvent(event);
		}
		return false;
	}

	public void setCaptions(Caption [] captions) {
		mCaptions = captions;
		updateCaptionAdapter();
	}

	public void setProgram(Program p) {
		mProgram = p;
		if (p == null) {
			return ;
		}
		mDuration = (int) p.duration;
		if (p.duration != 0) {
			mTime.setText(getTimeStr(mDuration));
		} else {
			mTime.setText(null);
		}
		mSeekBar.setVisibility(View.GONE);

		mSound.setVisibility(
				mPlayer.mPlayer.isSetSoundAvailable() ? View.VISIBLE : View.GONE);
		mSpeed.setVisibility(
				mPlayer.mPlayer.isSpeedAvailable() ? View.VISIBLE : View.GONE);
		mCurSpeed = 1f;
		updateSpeed();

		updateCaptionAdapter();
		updateIntervalTimer();
	}


	public void onPause() {
		mHandler.removeCallbacks(mIntervalRunnable);
	}

	public void onResume() {
		if (!mPlayer.mPause) {
			mHandler.postDelayed(mIntervalRunnable, INTERVAL);
		}
	}

	public void onPlayStateChanged() {
		updateIntervalTimer();
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		mVisible = visibility == View.VISIBLE;
		mSeekBar.setVisibility(mDuration > 0
				? View.VISIBLE : View.GONE);
		updateIntervalTimer();
	}
	
	void updateSpeed() {
		mSpeed.setText(String.format(Locale.ENGLISH, "x%.1f", mCurSpeed));
	}

	void updateIntervalTimer() {
		Log.d(TAG, "updateIntervalTimer pause:" + mPlayer.mPause
				+ " visible:" + mVisible);
		if (mPlayer.mPause || !mVisible) {
			mHandler.removeCallbacks(mIntervalRunnable, INTERVAL);
		} else {
			mHandler.postDelayed(mIntervalRunnable, INTERVAL);
		}
	}

	void updatePauseButton() {
		mPauseButton.setImageResource(
				mPlayer.mPause
				? R.drawable.ic_media_play
				: R.drawable.ic_media_pause);
	}


	View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.pause:
				mPlayer.togglePause();
				updatePauseButton();
				break;
			case R.id.previous:
				mPlayer.jump(-15);
				break;
			case R.id.rew:
				mPlayer.jump(-15);
				break;
			case R.id.ff:
				mPlayer.jump(30);
				break;
			case R.id.next:
				mPlayer.jump(30);
				break;
			case R.id.captionSwitch:
				Utils.showAnimation(mCaptionList,
						0.1f, 0,
						mCaptionSwitch.isChecked());
				break;
			};
		}
	};

	Runnable	mIntervalRunnable = new Runnable() {
		@Override
		public void run() {
			if (mPlayer != null && mPlayer.mPlayer != null) {
				if (App.DEBUG) {
					Log.d(TAG, "updateControls");
				}
				if (!mVisible) {
					return;
				}
				int duration = mPlayer.mPlayer.getDuration();
				if (mDuration != duration) {
					mDuration = duration;
					if (duration == 0) {
						mSeekBar.setVisibility(View.GONE);
					} else {
						mSeekBar.setVisibility(View.VISIBLE);
						mSeekBar.setMax(duration);
					}
				}
				int curPos = mPlayer.mPlayer.getCurrentPos();
				if (mCurPos != curPos) {
					mCurPos = curPos;
					if (mDuration > 0) {
						if (!mSeekBar.isPressed()) {
							mSeekBar.setProgress(curPos);
							updateTime(mCurPos);
						}
					}
				}
				if (!mPlayer.mPause) {

					mHandler.postDelayed(mIntervalRunnable, INTERVAL);
				}
			}
		}
	};
	
	StringBuilder	mTimeStr = new StringBuilder();

	void updateTime(int pos) {
		mTimeStr.delete(0, mTimeStr.length());
		
		if (mProgram != null) {
			// 放送時間も表示
			mTimeStr.append(DateUtils.formatDateTime(getContext(), 
					mProgram.startdate + pos, 
					DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
					| DateUtils.FORMAT_SHOW_WEEKDAY 
					| DateUtils.FORMAT_ABBREV_ALL));
			mTimeStr.append("\n");
		}
		mTimeStr.append(getTimeStr(pos));
		
		mTimeStr.append(" / ");
		mTimeStr.append(getTimeStr(mDuration));
		mTime.setText(mTimeStr);
	}

	String getTimeStr(int millis) {
		int sec = millis / 1000;
		if (sec > 60*60) {
			int min = sec / 60;
			return String.format(Locale.ENGLISH, "%d:%02d:%02d",
					min / 60, min % 60, sec % 60);
		}
		return String.format(Locale.ENGLISH, "%d:%02d", sec / 60, sec % 60);
	}

	void updateCaptionAdapter() {
		if (mCaptions != null && mCaptions.length > 0) {
			if (mCaptionAdapter == null) {
				mCaptionAdapter = new CaptionAdapter(getContext());
				mCaptionList.setAdapter(mCaptionAdapter);
			}
			mCaptionAdapter.clear();
			for (Caption c: mCaptions) {
				mCaptionAdapter.add(c);
			}
			if (mCaptionAdapter.getCount() > 0) {
				if (!mCaptionSwitch.isChecked()) {
					mCaptionSwitch.toggle();
				}
			}
			mCaptionContainer.setVisibility(View.VISIBLE);
		} else {
			mCaptionContainer.setVisibility(View.GONE);
		}
	}

	private void showOsd(CharSequence text) {
		mOsd.setText(text);
		mOsd.clearAnimation();
		mOsd.setVisibility(View.VISIBLE);

		if (mOsdCloseAnimation == null) {
			mOsdCloseAnimation = new AlphaAnimation(1, 0);
			mOsdCloseAnimation.setDuration(400);
		}

		mHandler.removeCallbacks(mHideOsdRunnable);
		mHandler.postDelayed(mHideOsdRunnable, 1 * DateUtils.SECOND_IN_MILLIS);
	}

	Runnable mHideOsdRunnable = new Runnable() {
		@Override
		public void run() {
			mOsd.startAnimation(mOsdCloseAnimation);
			mOsd.setVisibility(View.GONE);
		}
	};

}
