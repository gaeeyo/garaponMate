package jp.syoboi.android.garaponmate.provider;

import android.content.SearchRecentSuggestionsProvider;

public class MySearchSuggestionsProvider extends SearchRecentSuggestionsProvider {

	public static final String AUTHORITY =
			"jp.syoboi.android.garaponmate.SearchRecentSuggestions";

	public static final int MODE = DATABASE_MODE_QUERIES;

	public MySearchSuggestionsProvider() {
		super();
		setupSuggestions(AUTHORITY, MODE);
	}
}
