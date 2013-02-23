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

	public static int makeGenre(int genre0, int genre1) {
		return genre0 << 8 | genre1;
	}

	public static int getGenre0(int genre) {
		return (genre & 0xff00) >> 8;
	}
	public static int getGenre1(int genre) {
		return genre & 0xff;
	}
}
