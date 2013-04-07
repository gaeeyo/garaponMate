package jp.syoboi.android.garaponmate.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.GaraponAccount;

public class LoginActivity extends Activity {

	View			mLoginFormView;
	TextView		mUserView;
	TextView		mPassView;
	TextView		mMessage;
	View			mProgressView;
	View			mLoginHistory;
	LinearLayout	mLoginHistoryList;

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
		mLoginHistory = findViewById(R.id.loginHistory);
		mLoginHistoryList = (LinearLayout) findViewById(R.id.loginHistoryList);

		findViewById(R.id.login).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startLogin();
			}
		});

		mMessage.setText(null);

		updateLoginHistoryView();
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
	 * ログイン履歴のViewを更新
	 */
	void updateLoginHistoryView() {
		List<GaraponAccount> list = Prefs.getLoginHistory();

		mLoginHistory.setVisibility(list.size() > 0 ? View.VISIBLE : View.GONE);
		mLoginHistoryList.removeAllViews();

		for (final GaraponAccount account: list) {
			View v = View.inflate(mLoginHistoryList.getContext(),
					R.layout.login_history_row, null);
			LoginHistoryViewHolder vh = new LoginHistoryViewHolder(v);
			vh.bind(account);

			// アカウントの削除ボタン
			vh.mDeleteView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					confirmRemoveAccount(account.garaponId);
				}
			});

			// アカウントをタップしたらログイン
			vh.mTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mUserView.setText(account.garaponId);
					mPassView.setText(account.password);
					startLogin();
				}
			});

			mLoginHistoryList.addView(v);
		}
	}

	void confirmRemoveAccount(final String garaponId) {
		new AlertDialog.Builder(this)
		.setMessage(getString(R.string.confirmRemoveAccountFmt, garaponId))
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Prefs.removeLoginHistory(garaponId);
				updateLoginHistoryView();
			}
		})
		.setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener)null)
		.show();
	}

	private static class LoginHistoryViewHolder {
		public TextView mTextView;
		public View mDeleteView;

		public LoginHistoryViewHolder(View v) {
			mTextView = (TextView) v.findViewById(R.id.user);
			mDeleteView = v.findViewById(R.id.delete);
		}

		public void bind(GaraponAccount account) {
			mTextView.setText(account.garaponId);
		}
	}

	/**
	 * ログインを開始
	 */
	void startLogin() {
		if (mIsLoginProgress) {
			return;
		}

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

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
					Prefs.addLoginHistory(new GaraponAccount(user, pass));
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
