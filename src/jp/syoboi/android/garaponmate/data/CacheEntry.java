package jp.syoboi.android.garaponmate.data;

import java.io.File;

public class CacheEntry {
	private final CacheFile 	parent;
	public final long 	id;
	public String		etag;
	public String		lastModified;

	public CacheEntry(CacheFile parent, long id) {
		this.parent = parent;
		this.id = id;
	}

	public File getFile() {
		return parent.getFile(this);
	}
}
