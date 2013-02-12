package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

import jp.syoboi.android.garaponmate.R;

public class BorderingTextView extends TextView {

	private static Paint sPaint = new Paint();
	private static float [] sSinTable;
	private static float [] sCosTable;

	static {
		sPaint.setAntiAlias(true);

		float tableStep = (float) ((Math.PI * 2) / 11);
		int tableSize = (int)((Math.PI * 2) / tableStep);
		sSinTable = new float [tableSize];
		sCosTable = new float [tableSize];
		float x = 0;
		for (int j=0; j<tableSize; j++, x+=tableStep) {
			sCosTable[j] = (float) Math.cos(x);
			sSinTable[j] = (float) Math.sin(x);
		}

	}

	private Bitmap 	mBitmap;
	private Canvas	mCanvas;
	private int	 	mBorderColor;
	private int 	mBorderWidth;

	ColorMatrixColorFilter	mColorFilter;

	public BorderingTextView(Context context, AttributeSet attrs) {
		super(context, attrs);

		float density = context.getResources().getDisplayMetrics().density;

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BorderingTextView);

		mBorderColor = ta.getColor(R.styleable.BorderingTextView_borderColor, 0);
		mBorderWidth = ta.getDimensionPixelSize(R.styleable.BorderingTextView_borderWidth,
				Math.round(1.5f * density));

		ta.recycle();

		mColorFilter = new ColorMatrixColorFilter(new float [] {
				0, 0, 0, 0, Color.red(mBorderColor),
				0, 0, 0, 0, Color.green(mBorderColor),
				0, 0, 0, 0, Color.blue(mBorderColor),
				0, 0, 0, 1, 0,
		});
	}

	@Override
	public void draw(Canvas canvas) {
			if (mBorderColor == 0) {
				super.draw(canvas);
				return;
			}

			int width = getWidth();
			int height = getHeight();
			if (mBitmap == null || mBitmap.getWidth() != width || mBitmap.getHeight() != height) {
				if (mBitmap != null) {
					mBitmap.recycle();
				}
				mBitmap = Bitmap.createBitmap(width, height, Config.ARGB_4444);
				mCanvas = new Canvas(mBitmap);
			}

			Canvas tmpCanvas = mCanvas;
			Paint paint = sPaint;

			mBitmap.eraseColor(0);
			super.draw(tmpCanvas);

			paint.setColorFilter(mColorFilter);
			for (int j=0; j<sCosTable.length; j++) {
				float x = sCosTable[j] * mBorderWidth;
				float y = sSinTable[j] * mBorderWidth;
				canvas.drawBitmap(mBitmap, x, y, sPaint);
			}
			paint.setColorFilter(null);
			canvas.drawBitmap(mBitmap, 0, 0, sPaint);
	}
}
