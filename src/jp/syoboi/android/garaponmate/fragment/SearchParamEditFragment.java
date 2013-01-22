package jp.syoboi.android.garaponmate.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.adapter.ChAdapter;
import jp.syoboi.android.garaponmate.adapter.GenreAdapter;
import jp.syoboi.android.garaponmate.client.SearchParam;
import jp.syoboi.android.garaponmate.data.Genre;
import jp.syoboi.android.garaponmate.data.GenreGroup;
import jp.syoboi.android.garaponmate.data.GenreGroupList;
import jp.syoboi.android.garaponmate.utils.Utils;

public class SearchParamEditFragment extends DialogFragment {

	public static final String EXTRA_SEARCH_PARAM = "searchParam";

	SearchParam		mSearchParam;
	TextView		mKeyword;
	Spinner			mCh;
	Spinner			mGenre0;
	Spinner			mGenre1;
	CheckBox		mFavoriteOnly;
	GenreGroupList	mGenreGroupList;

	public static SearchParamEditFragment newInstance(SearchParam sp) {
		Bundle args = new Bundle();
		args.putSerializable(EXTRA_SEARCH_PARAM, sp);

		SearchParamEditFragment f = new SearchParamEditFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchParam = (SearchParam) getArguments().getSerializable(EXTRA_SEARCH_PARAM);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog dlg = new AlertDialog.Builder(getActivity())
		.setTitle(R.string.searchCondition)
		.setPositiveButton(android.R.string.ok, (OnClickListener)null)
		.setNeutralButton(android.R.string.cancel, (OnClickListener)null)
		.create();

		dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
				|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		View v = View.inflate(dlg.getContext(), R.layout.search_param_form, null);

		mKeyword = (TextView) v.findViewById(R.id.keywordAnd);
		mCh = (Spinner) v.findViewById(R.id.ch);
		mGenre0 = (Spinner) v.findViewById(R.id.genre0);
		mGenre1 = (Spinner) v.findViewById(R.id.genre1);
		mFavoriteOnly = (CheckBox) v.findViewById(R.id.favoriteOnly);
		View saveBtn = v.findViewById(R.id.save);

		// 保存ボタン
		saveBtn.setVisibility(mSearchParam.id == 0 ? View.VISIBLE : View.GONE);
		saveBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onOk(true);
			}
		});

		// チャンネル
		mCh.setAdapter(new ChAdapter(getActivity(), R.string.notSelected));

		// ジャンル選択
		mGenreGroupList = GenreGroupList.getInstance(getActivity());

		final GenreAdapter ga0 = new GenreAdapter(getActivity(), R.string.notSelected);
		final GenreAdapter ga1 = new GenreAdapter(getActivity(), R.string.notSelected);
		mGenre0.setAdapter(ga0);
		mGenre1.setAdapter(ga1);

		ga0.setRoot(mGenreGroupList);

		// ジャンルの大項目が選択されたら、小項目のリストを再設定する
		mGenre0.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id) {

				if (id == -1) {
					mGenre1.setEnabled(false);
				} else {
					mGenre1.setEnabled(true);
					Genre genre = mGenreGroupList.findByValue((int)id);
					ga1.setRoot((GenreGroup)genre);

					if (mSearchParam.genre0 != SearchParam.GENRE_EMPTY) {
						Utils.spinnerSetSelectionById(mGenre1, mSearchParam.genre1);
					} else {
						Utils.spinnerSetSelectionById(mGenre1, null);
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});


		dlg.setView(v);

		dlg.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				final AlertDialog dlg = (AlertDialog) getDialog();
				dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onOk(false);
					}
				});
			}
		});

		setToView(mSearchParam);

		return dlg;
	}

	public boolean onOk(boolean save) {
		try {
			validate();

			if (save || mSearchParam.id != 0) {
				App.getSearchParamList().save(mSearchParam);
			}

			Fragment f = getTargetFragment();
			if (f != null) {
				Bundle args = getArguments();
				args.putSerializable(EXTRA_SEARCH_PARAM, mSearchParam);

				Intent data = new Intent();
				data.putExtras(args);

				f.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
			}
			dismiss();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			ErrorDialogFragment.show(getFragmentManager(), e);
			return false;
		}
	}

	public void validate() throws Exception {
		String keyword = mKeyword.getText().toString().trim();
		int ch = (int) mCh.getSelectedItemId();
		int genre0 = (int) mGenre0.getSelectedItemId();
		int genre1 = (int) mGenre1.getSelectedItemId();
		boolean favoriteOnly = mFavoriteOnly.isChecked();

		if (TextUtils.isEmpty(keyword)
				&& ch == 0
				&& genre0 == -1
				&& favoriteOnly == false) {
			throw new Exception(getString(R.string.searchConditionEmpty));
		}

		mSearchParam.keyword = keyword;
		mSearchParam.ch = ch;
		mSearchParam.genre0 = genre0;
		mSearchParam.genre1 = genre1;
		mSearchParam.rank = (favoriteOnly ? SearchParam.RANK_FAVORITE : 0);
	}

	public void setToView(SearchParam p) {
		mKeyword.setText(p.keyword);

		Utils.spinnerSetSelectionById(mCh, p.ch);
		Utils.spinnerSetSelectionById(mGenre0, p.genre0);
		Utils.spinnerSetSelectionById(mGenre1, p.genre1);

		mFavoriteOnly.setChecked(p.rank == SearchParam.RANK_FAVORITE);
	}
}
