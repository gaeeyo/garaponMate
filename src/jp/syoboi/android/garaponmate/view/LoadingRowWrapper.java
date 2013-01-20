package jp.syoboi.android.garaponmate.view;

import android.view.View;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import jp.syoboi.android.garaponmate.R;

public class LoadingRowWrapper  {

	ViewSwitcher	mView;
	View			mProgress;
	TextView		mMessage;

	public LoadingRowWrapper(ViewSwitcher v) {
		mView = v;
		mProgress = v.findViewById(android.R.id.progress);
		mMessage = (TextView) v.findViewById(android.R.id.message);
	}

	public void setLoading() {
		mView.setDisplayedChild(mView.indexOfChild(mProgress));
	}

	public void setMessage(String message) {
		mView.setDisplayedChild(mView.indexOfChild(mMessage));
		mMessage.setText(message);
	}

	public void setMessage(Throwable t) {
		String errorPrefix = mView.getResources().getString(R.string.error);
		setMessage(errorPrefix +": " + t.getMessage());
	}

	public View getView() {
		return mView;
	}

	public TextView getMesageView() {
		return mMessage;
	}
}
