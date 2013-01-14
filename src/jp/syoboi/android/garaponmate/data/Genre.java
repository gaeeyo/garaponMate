package jp.syoboi.android.garaponmate.data;


public class Genre {

	public int value;
	public String name;

	public Genre(String text) {
		value = Integer.valueOf(text.substring(0, 2), 10);
		name = text.substring(2);
	}
}
