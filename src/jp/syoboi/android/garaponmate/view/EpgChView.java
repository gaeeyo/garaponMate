package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.data.ChList;

public class EpgChView extends View {

	ChList	mChList;

	public EpgChView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mChList = App.from(context).getChList();
	}


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int count = mChList.size();

	}
}
