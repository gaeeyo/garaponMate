package jp.syoboi.android.garaponmate.data;


public class Genre {

	public int value;
	public String name;

	public Genre(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
