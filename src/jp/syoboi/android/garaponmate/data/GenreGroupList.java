package jp.syoboi.android.garaponmate.data;

import android.content.Context;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jp.syoboi.android.garaponmate.R;

public class GenreGroupList extends GenreGroup {
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
		super(-1, "root");

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
				if (line.charAt(0) == ' ') {
					line = line.substring(1);
					int value = Integer.valueOf(line.substring(0, 2), 10);
					String name = line.substring(2);
					cur.childs.add(new Genre(value, name));
				} else {
					int value = Integer.valueOf(line.substring(0, 2), 10);
					String name = line.substring(2);
					cur = new GenreGroup(value, name);
					childs.add(cur);
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

	@Override
	public GenreGroup findByValue(int genre) {
		return (GenreGroup) super.findByValue(genre);
	}
}
