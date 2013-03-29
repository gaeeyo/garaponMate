package jp.syoboi.android.garaponmate.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;

public class LoginActivity extends Activity {

	View		mLoginFormView;
	TextView	mUserView;
	TextView	mPassView;
	TextView	mMessage;
	View		mProgressView;
	LoginTask	mLoginTask;

	boolean		mIsLoginProgress;

	public static void startActivity(Activity a) {
		Intent i = new Intent(a, LoginActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		a.startActivity(i);
		a.finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		mLoginFormView = findViewById(R.id.loginForm);
		mProgressView = findViewById(R.id.progress);
		mUserView = (TextView) findViewById(R.id.user);
		mPassView = (TextView) findViewById(R.id.password);
		mMessage = (TextView) findViewById(R.id.message);

		findViewById(R.id.login).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startLogin();
			}
		});

		mMessage.setText(null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateView();
	}

	void updateView() {
		mMessage.setVisibility(mMessage.getText().length() > 0 ? View.VISIBLE : View.GONE);
		mProgressView.setVisibility(mIsLoginProgress ? View.VISIBLE : View.GONE);
		mLoginFormView.setVisibility(!mIsLoginProgress ? View.VISIBLE : View.GONE);
	}

	/**
	 * ログインを開始
	 */
	void startLogin() {
		if (mIsLoginProgress) {
			return;
		}

		final String user = mUserView.getText().toString().trim();
		final String pass = mPassView.getText().toString().trim();

		View errorView = null;
		mUserView.setError(null);
		mPassView.setError(null);
		mMessage.setText(null);

		// パスワードのチェック
		if (TextUtils.isEmpty(pass)) {
			mPassView.setError(getString(R.string.errFormEmpty));
			errorView = mPassView;
		}

		// ガラポンIDのチェック
		if (TextUtils.isEmpty(user)) {
			mUserView.setError(getString(R.string.errFormEmpty));
			errorView = mUserView;
		}

		// エラーがあればエラーのViewにフォースして終了
		if (errorView != null) {
			errorView.requestFocus();
			return;
		}

		// ログインを試行
		mIsLoginProgress = true;
		updateView();

		mLoginTask = new LoginTask() {
			@Override
			protected void onPostExecute(Object result) {
				mLoginTask = null;
				if (result instanceof Throwable) {
					// ログイン中にエラーが発生したら、エラーを表示
					Throwable e = (Throwable) result;
					mIsLoginProgress = false;
					mMessage.setText(String.valueOf(e.getMessage()));
					updateView();
				} else {
					// エラーがなければuser,passを保存して
					// MainActivityを起動
					Prefs.setUser(user, pass);
					MainActivity.startActivity(LoginActivity.this);
					finish();
				}
			}
			@Override
			protected void onCancelled() {
				super.onCancelled();
				mLoginTask = null;
			}
		};
		mLoginTask.execute(user, pass);
	}

	private static class LoginTask extends AsyncTask<Object,Object,Object> {

		@Override
		protected Object doInBackground(Object... params) {

			String user = (String) params[0];
			String pass = (String) params[1];
			try {
				GaraponClientUtils.auth(user, pass);
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return e;
			}
		}
	}

}
