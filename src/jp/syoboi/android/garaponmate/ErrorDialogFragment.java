package jp.syoboi.android.garaponmate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ErrorDialogFragment extends DialogFragment {

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

		AlertDialog dlg = new AlertDialog.Builder(getActivity())
			.setTitle(title)
			.setMessage(message)
			.create();

		return dlg;
	}
}
