package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * fitXY にすると縦横比を維持したまま拡大される(幅が基準)
 */
public class MyImageView extends ImageView implements MyImageViewInterface {

	public MyImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (getScaleType() == ScaleType.FIT_XY) {
			Drawable d = getDrawable();
			if (d != null) {
				int width = getMeasuredWidth();
				int height = getMeasuredHeight();

				int imageWidth = d.getIntrinsicWidth();
				int imageHeight = d.getIntrinsicHeight();

				if (imageWidth > 0 && imageHeight > 0) {
					int newHeight = imageHeight * width / imageWidth;
					if (newHeight != height) {
						setMeasuredDimension(width, newHeight);
					}
				}
			}
		}
	}

	@Override
	public void invalidateDrawable(Drawable dr) {
		super.invalidateDrawable(dr);
		if (dr == getDrawable()) {
			setImageDrawable(null);
			setImageDrawable(dr);
		}
	}

	@Override
	public void onLoaded(Bitmap bmp) {
		// TODO Auto-generated method stub

	}
}
