package jp.syoboi.android.garaponmate.data;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.syoboi.android.util.JksnUtils;
import jp.syoboi.android.util.JksnUtils.JksnArray;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;

public class SearchParamList {

	private File		mFile;
	private ArrayList<SearchParam>	mItems;
	private DataSetObservable mObservable = new DataSetObservable();

	public SearchParamList(File f) {
		mFile = f;
	}

	public void registerDataSetObserver(DataSetObserver observer) {
		mObservable.registerObserver(observer);
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		mObservable.unregisterObserver(observer);
	}

	private synchronized void ensureItems() {
		if (mItems == null) {
			mItems = new ArrayList<SearchParam>();
			try {
				loadFromFile(mFile);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public List<SearchParam> items() {
		ensureItems();
		return mItems;
	}

	public synchronized void removeById(long id) {
		int idx = findById(id);
		if (idx != -1) {
			mItems.remove(idx);
			saveToFile(mFile);
		}
	}

	public synchronized void save(SearchParam p) {
		ensureItems();
		if (p.id == 0) {
			p.id = newId();
		}
		int idx = findById(p.id);
		if (idx != -1) {
			mItems.remove(idx);
			mItems.add(idx, p);
		} else {
			mItems.add(p);
		}
		saveToFile(mFile);
	}

	long newId() {
		ensureItems();
		long id = System.currentTimeMillis();
		for (int j=0; j<100; j++, id++) {
			if (findById(id) == -1) {
				return id;
			}
		}
		return id;
	}

	int findById(long id) {
		ensureItems();
		int index = 0;
		for (SearchParam p: mItems) {
			if (p.id == id) {
				return index;
			}
			index++;
		}
		return -1;
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
		mObservable.notifyChanged();
	}

	boolean saveToFileInternal(File f) throws FileNotFoundException {
		FileOutputStream fos = new FileOutputStream(f);
		try {
			JsonGenerator jg = JksnUtils.getFactory().createJsonGenerator(fos);
			jg.writeStartObject();
			jg.writeFieldName("items");
			jg.writeStartArray();
			for (SearchParam p: mItems) {
				p.write(jg);
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
					SearchParam p = new SearchParam((JksnObject)o);
					mItems.add(p);
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
