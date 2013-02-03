package jp.syoboi.android.garaponmate.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import jp.syoboi.android.garaponmate.data.Genre;
import jp.syoboi.android.garaponmate.data.GenreGroup;
import jp.syoboi.android.garaponmate.data.SearchParam;

public class GenreAdapter extends ArrayAdapter<Genre> {

	Genre	mEmpty;

	public GenreAdapter(Context context, int notSelectedTextId) {
		super(context, android.R.layout.simple_dropdown_item_1line);

		if (notSelectedTextId != 0) {
			mEmpty = new Genre(SearchParam.GENRE_EMPTY, context.getString(notSelectedTextId));
		}
	}

	public void setRoot(GenreGroup root) {
		clear();

		if (mEmpty != null) {
			add(mEmpty);
		}

		for (Genre genre: root.childs) {
			add(genre);
		}
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).value;
	}

}
