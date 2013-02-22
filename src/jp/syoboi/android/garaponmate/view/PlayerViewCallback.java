package jp.syoboi.android.garaponmate.view;

public interface PlayerViewCallback {
	public void onMessage(String message);
	public void onBuffering(int pos, int max);
}