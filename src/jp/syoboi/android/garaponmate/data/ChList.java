package jp.syoboi.android.garaponmate.data;

import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.client.GaraponClient.Ch;
import jp.syoboi.android.util.JksnUtils;
import jp.syoboi.android.util.JksnUtils.JksnArray;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;

public class ChList {

	private static final String TAG = "ChList";

	private File	mFile;

	SparseArray<Ch> mMap;
	Ch []			mSorted;

	public ChList(File f) {
		mFile = f;
	}

	private synchronized void ensureItems() {
		if (mMap == null) {
			mMap = new SparseArray<Ch>();
			mSorted = new Ch [0];
			try {
				setItems(loadFromFile(mFile));
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int size() {
		return mMap.size();
	}

	public Ch get(int index) {
		return mMap.valueAt(index);
	}

	public synchronized Ch getCh(int number) {
		ensureItems();
		return mMap.get(number);
	}

	public Collection<Ch> toArray(boolean sort) {
		ensureItems();

		ArrayList<Ch> items = new ArrayList<Ch>();
		for (Ch ch: mSorted) {
			items.add(ch);
		}
		return items;
	}

	public synchronized void setCh(Collection<Ch> items) {
		ensureItems();
		if (setItems(items)) {
			saveToFile(mFile);
		}
	}

	private synchronized boolean setItems(Collection<Ch> items) {
		if (items.size() == mMap.size()) {
			boolean changed = false;
			for (Ch ch: items) {
				if (!ch.equals(mMap.get(ch.ch))) {
					changed = true;
					break;
				}
			}
			if (!changed) {
				if (App.DEBUG) Log.v(TAG, "変更が無かったので保存しない");
				return false;
			}
		}

		if (App.DEBUG) Log.v(TAG, "ChList 保存");

		mMap.clear();
		mSorted = new Ch [items.size()];

		int idx = 0;
		for (Ch ch: items) {
			mMap.put(ch.ch, ch);
			mSorted[idx++] = ch;
		}

		Arrays.sort(mSorted, new Comparator<Ch>() {
				@Override
				public int compare(Ch lhs, Ch rhs) {
					return rhs.ch - lhs.ch;
				}
		});

		return true;
	}

	void saveToFile(File file) {
		File tmpFile = new File(file.getPath() + ".tmp");
		try {
			if (saveToFileInternal(tmpFile)) {
				tmpFile.renameTo(mFile);
			} else {
				tmpFile.delete();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	boolean saveToFileInternal(File f) throws FileNotFoundException {
		FileOutputStream fos = new FileOutputStream(f);
		try {
			JsonGenerator jg = JksnUtils.getFactory().createJsonGenerator(fos);
			jg.writeStartObject();
			jg.writeFieldName("items");
			jg.writeStartArray();
			for (Ch ch: mSorted) {
				ch.write(jg);
			}
			jg.writeEndArray();
			jg.writeEndObject();
			jg.flush();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	static ArrayList<Ch> loadFromFile(File file) throws JsonParseException, IOException {

		ArrayList<Ch> items = new ArrayList<Ch>();

		FileInputStream fis = new FileInputStream(file);
		try {
			JksnObject jo = (JksnObject) JksnUtils.parseJson(fis, null);
			JksnArray ja = jo.getArray("items");

			for (Object o: ja) {
				if (o instanceof JksnObject) {
					Ch ch = new Ch((JksnObject)o);
					items.add(ch);
				}
			}

		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return items;
	}

}
