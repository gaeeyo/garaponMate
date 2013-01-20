package jp.syoboi.android.garaponmate.data;

import java.util.ArrayList;

public class GenreGroup extends Genre {

	public ArrayList<Genre> childs = new ArrayList<Genre>();

	public GenreGroup(int value, String text) {
		super(value, text);
	}

	public Genre findByValue(int genre) {
		for (Genre g: childs) {
			if (g.value == genre) {
				return g;
			}
		}
		return null;
	}
}
