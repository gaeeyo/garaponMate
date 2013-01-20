package jp.syoboi.android.garaponmate.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;

import jp.syoboi.android.garaponmate.R;

public class ErrorDialogFragment extends DialogFragment {

	public static ErrorDialogFragment show(FragmentManager fm, Throwable throwable) {
		try {
			ErrorDialogFragment f = newInstance(null, throwable);
			f.show(fm, "errorDialog");
			return f;
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ErrorDialogFragment newInstance(String title, Throwable throwable) {
		if (throwable.getClass().getName().startsWith("jp.syoboi.android")
				|| throwable.getClass().equals(Exception.class)) {
			return newInstance(title, throwable.getMessage());
		}

		return newInstance(title, throwable.getMessage() + "\n" + throwable.toString());
	}

	public static ErrorDialogFragment newInstance(String title, String message) {
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, title);
		args.putString(Intent.EXTRA_TEXT, message);

		ErrorDialogFragment f = new ErrorDialogFragment();
		f.setArguments(args);
		return f;
	}


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String title = args.getString(Intent.EXTRA_TITLE);
		String message = args.getString(Intent.EXTRA_TEXT);

		if (title == null) {
			title = getString(R.string.error);
		}

		AlertDialog dlg = new AlertDialog.Builder(getActivity())
			.setTitle(title)
			.setMessage(message)
			.create();

		return dlg;
	}
}
