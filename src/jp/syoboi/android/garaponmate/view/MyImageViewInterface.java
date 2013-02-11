package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface MyImageViewInterface {
	public Context getContext();
	public void setImageDrawable(Drawable d);
	public void setImageResource(int id);
	public Drawable getDrawable();
	public void onLoaded(Bitmap bmp);
}
