package jp.syoboi.android.garaponmate.data;

import java.util.ArrayList;

public class GenreGroup extends Genre {

	public ArrayList<Genre> childs = new ArrayList<Genre>();

	public GenreGroup(String text) {
		super(text);
	}
}
