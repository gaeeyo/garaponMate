package jp.syoboi.android.garaponmate.data;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;

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
	private LinkedHashMap<Integer, Ch> mItems;

	public ChList(File f) {
		mFile = f;
	}

	private synchronized void ensureItems() {
		if (mItems == null) {
			mItems = new LinkedHashMap<Integer, Ch>();
			try {
				loadFromFile(mFile);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized Ch getCh(int number) {
		ensureItems();
		return mItems.get(number);
	}

	public Collection<Ch> toArray(boolean sort) {
		ensureItems();
		ArrayList<Ch> items = new ArrayList<Ch>(mItems.values());

		if (sort) {
			Collections.sort(items, new Comparator<Ch>() {
				@Override
				public int compare(Ch lhs, Ch rhs) {
					return rhs.ch - lhs.ch;
//					return lhs.ch - rhs.ch;
				}
			});
		}
		return items;
	}

	public synchronized void setCh(Collection<Ch> items) {

		ensureItems();

		if (items.size() == mItems.size()) {
			boolean changed = false;
			for (Ch ch: items) {
				if (!ch.equals(mItems.get(ch.ch))) {
					changed = true;
					break;
				}
			}
			if (!changed) {
				if (App.DEBUG) Log.v(TAG, "変更が無かったので保存しない");
				return;
			}
		}

		if (App.DEBUG) Log.v(TAG, "ChList 保存");

		mItems.clear();
		for (Ch ch: items) {
			mItems.put(ch.ch, ch);
		}

		saveToFile(mFile);
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
			for (Ch ch: mItems.values()) {
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

	void loadFromFile(File file) throws JsonParseException, IOException {
		mItems.clear();
		FileInputStream fis = new FileInputStream(file);
		try {
			JksnObject jo = (JksnObject) JksnUtils.parseJson(fis, null);
			JksnArray ja = jo.getArray("items");

			for (Object o: ja) {
				if (o instanceof JksnObject) {
					Ch ch = new Ch((JksnObject)o);
					mItems.put(ch.ch, ch);
				}
			}

		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
