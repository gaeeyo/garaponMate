package jp.syoboi.android.garaponmate.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.ProgSearch;

public class ProgSearchEditFragment extends DialogFragment {

	public static final String EXTRA_PROGSEARCH = "progSearch";
	public static final String EXTRA_INDEX = "index";

	private EditText	mKwAnd;
	private EditText	mKwOr;
	private EditText	mKwNot;
	private EditText	mTitleAnd;
	private EditText	mTitleOr;
	private EditText	mTitleNot;
	private EditText	mChOr;
	private EditText	mDurationMax;
	private EditText	mDurationMin;

	public static ProgSearchEditFragment newInstance(int idx, ProgSearch progSearch) {

		Bundle args = new Bundle();
		args.putSerializable(EXTRA_PROGSEARCH, progSearch);
		args.putInt(EXTRA_INDEX, idx);

		ProgSearchEditFragment f = new ProgSearchEditFragment();
		f.setArguments(args);

		return f;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog dlg = new AlertDialog.Builder(getActivity())
			.setTitle(R.string.searchListSettings)
			.setNeutralButton(android.R.string.cancel, (OnClickListener)null)
			.setPositiveButton(android.R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setResult();
				}
			})
			.create();

		View v = View.inflate(dlg.getContext(), R.layout.prog_search_edit, null);

		View parent = v.findViewById(R.id.titleKeywords);
		mTitleAnd = (EditText)parent.findViewById(R.id.keywordAnd);
		mTitleOr  = (EditText)parent.findViewById(R.id.keywordOr);
		mTitleNot = (EditText)parent.findViewById(R.id.keywordNot);

		parent = v.findViewById(R.id.titleOrDescriptionKeywords);
		mKwAnd = (EditText)parent.findViewById(R.id.keywordAnd);
		mKwOr  = (EditText)parent.findViewById(R.id.keywordOr);
		mKwNot = (EditText)parent.findViewById(R.id.keywordNot);

		mChOr = (EditText)v.findViewById(R.id.chOr);
		mDurationMin = (EditText)v.findViewById(R.id.durationMin);
		mDurationMax = (EditText)v.findViewById(R.id.durationMax);

		setToDialog();

		dlg.setView(v);

		return dlg;
	}

	void setToDialog() {
		ProgSearch ps = (ProgSearch) getArguments().getSerializable(EXTRA_PROGSEARCH);
		mTitleAnd.setText(ps.titleAnd);
		mTitleOr.setText(ps.titleOr);
		mTitleNot.setText(ps.titleNot);
		mKwAnd.setText(ps.kwAnd);
		mKwOr.setText(ps.kwOr);
		mKwNot.setText(ps.kwNot);

		mChOr.setText(ps.chOr);
		if (ps.durationMin > 0) {
			mDurationMin.setText(String.valueOf(ps.durationMin));
		}
		if (ps.durationMax > 0) {
			mDurationMax.setText(String.valueOf(ps.durationMax));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle arg0) {
		super.onSaveInstanceState(arg0);
		setToArguments();
	}


	void setResult() {
		setToArguments();

		Intent data = new Intent();
		data.putExtras(getArguments());

		getTargetFragment().onActivityResult(
				getTargetRequestCode(),
				Activity.RESULT_OK, data);
	}

	void setToArguments() {

		ProgSearch ps = new ProgSearch();
		ps.titleAnd = mTitleAnd.getText().toString();
		ps.titleOr = mTitleOr.getText().toString();
		ps.titleNot = mTitleNot.getText().toString();
		ps.kwAnd = mKwAnd.getText().toString();
		ps.kwOr = mKwOr.getText().toString();
		ps.kwNot = mKwNot.getText().toString();
		ps.chOr = mChOr.getText().toString();
		ps.durationMin = getNumber(mDurationMin, 0);
		ps.durationMax = getNumber(mDurationMax, 0);

		Bundle args = getArguments();
		args.putSerializable(EXTRA_PROGSEARCH, ps);
	}

	public int getNumber(EditText edit, int fallback) {
		String str = edit.getText().toString();
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			return fallback;
		}
	}
}
