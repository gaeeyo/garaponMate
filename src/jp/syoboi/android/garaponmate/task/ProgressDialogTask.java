package jp.syoboi.android.garaponmate.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;

public abstract class ProgressDialogTask {

	private static final String TAG = "ProgressDialogTask";

	ProgressDialog			mDialog;
	Context					mContext;
	AsyncTask<Object,Object,Object>	mTask;

	public ProgressDialogTask(Context context) {
		mContext = context;


		mTask = new AsyncTask<Object, Object, Object>() {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				getDialog().show();
			}
			@Override
			protected Object doInBackground(Object... params) {
				return ProgressDialogTask.this.doInBackground(params);
			}
			@Override
			protected void onPostExecute(Object result) {
				dismissDialog();
				if (isCancelled()) {
					ProgressDialogTask.this.onCancelled();
				} else {
					super.onPostExecute(result);
					ProgressDialogTask.this.onPostExecute(result);
				}
			}
			@Override
			protected void onCancelled() {
				dismissDialog();
				super.onCancelled();
				ProgressDialogTask.this.onCancelled();
			}
		};
	}

	public void setMessage(int id) {
		getDialog().setMessage(mContext.getString(id));
	}

	public ProgressDialog getDialog() {
		if (mDialog == null) {
			mDialog = new ProgressDialog(mContext);
		}
		return mDialog;
	}

	public void execute(Object... params) {
		mTask.execute(params);
	}

	public void cancel(boolean mayInterruptIfRunning) {
		if (mTask != null) {
			mTask.cancel(mayInterruptIfRunning);
		} else {
			Log.w(TAG, "すでにタスクが終了しています");
		}
	}

	public Status getStatus() {
		return mTask.getStatus();
	}

	void dismissDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}

	protected abstract Object doInBackground(Object... params);
	protected abstract void onPostExecute(Object result);
	protected abstract void onCancelled();
}
