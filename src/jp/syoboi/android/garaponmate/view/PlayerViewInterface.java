package jp.syoboi.android.garaponmate.view;

import android.view.View;

public interface PlayerViewInterface {
	public void setVideo(String id);
	public void play();
	public void stop();
	public void pause();
	public void onPause();
	public void onResume();
	public void destroy();
	public void seek(int msec);
	public int getDuration();
	public int getCurrentPos();
	public void jump(int msec);
	public View getView();
	public void setSound(String lr);
	public void setSpeed(float speed);
	public boolean isSetSoundAvailable();
	public boolean isSpeedAvailable();
}