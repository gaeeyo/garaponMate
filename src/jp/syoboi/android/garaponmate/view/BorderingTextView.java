package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint.Style;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

public class BorderingTextView extends TextView {

	private int mTextColor;
	private int mShadowColor;
	private float mStrokeWidth;

	public BorderingTextView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray ta = context.obtainStyledAttributes(attrs, new int [] {
				android.R.attr.textColor,
				android.R.attr.shadowColor });
		mTextColor = ta.getColor(0, 0);
		mShadowColor = ta.getColor(1, 0);
		ta.recycle();

		mStrokeWidth = 1.5f * (context.getResources().getDisplayMetrics().density);
	}

	@Override
	public void draw(Canvas canvas) {

		if (mShadowColor == 0) {
			super.draw(canvas);
			return;
		}
		TextPaint paint = getPaint();

		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(mStrokeWidth);
		//paint.setColor(mShadowColor);
		setTextColor(mShadowColor);
		super.draw(canvas);

		paint.setStyle(Style.FILL);
		//paint.setColor(mTextColor);
		setTextColor(mTextColor);

		super.draw(canvas);
	}
}
