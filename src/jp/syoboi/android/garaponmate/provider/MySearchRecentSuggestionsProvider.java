package jp.syoboi.android.garaponmate.provider;

import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;

public class MySearchRecentSuggestionsProvider extends SearchRecentSuggestionsProvider {

	public static final String AUTHORITY =
			"jp.syoboi.android.garaponmate.SearchRecentSuggestions";

	public static final int MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES;

	public MySearchRecentSuggestionsProvider() {
		super();
		setupSuggestions(AUTHORITY, MODE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return super.query(uri, projection, selection, selectionArgs, sortOrder);
	}
}
