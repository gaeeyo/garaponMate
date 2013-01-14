package jp.syoboi.android.garaponmate.data;

import android.content.Context;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import jp.syoboi.android.garaponmate.R;

public class GenreGroupList extends ArrayList<GenreGroup> {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static GenreGroupList sInstance;

	public static synchronized GenreGroupList getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new GenreGroupList(context);
		}
		return sInstance;
	}

	private GenreGroupList(Context context) {

		Resources res = context.getResources();

		BufferedReader is = new BufferedReader(new InputStreamReader(
				res.openRawResource(R.raw.genre)));
		try {
			GenreGroup cur = null;
			String line;
			while ((line = is.readLine()) != null) {
				if (line.length() == 0) {
					continue;
				}
				if (line.charAt(0) != ' ') {
					cur = new GenreGroup(line);
					add(cur);
				} else {
					cur.childs.add(new Genre(line.substring(1)));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


}
