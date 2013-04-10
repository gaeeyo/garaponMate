package jp.syoboi.android.garaponmate.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;

public class TSProvider extends ContentProvider {

	static final String TAG = "TSProvider";

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return "application/octet-stream";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		throw new UnsupportedOperationException("query not supported");
//		Log.v(TAG, "query uri:" + uri.toString() + "");
//		MatrixCursor cur = new MatrixCursor(new String [] {
//				MediaColumns._ID,
//				MediaColumns.TITLE,
//				MediaColumns.DATA,
//				MediaColumns.MIME_TYPE,
//				MediaColumns.SIZE,
//		});
//		cur.addRow(new Object [] {
//			1L,
//			"タイトル",
//			"content://jp.syoboi.android.garaponmate.TS/1",
//			"video/mp4",
//			10000,
//		});
//		return null;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {
		Log.v(TAG, "openFile uri:" + uri.toString() + " mode:" + mode);

		final FileInputStream is = new FileInputStream("/sdcard/Movies/20130316-1900 ＮＨＫニュース７ [NHK総合].ts");
//		return ParcelFileDescriptor.open(
//				new File("/sdcard/Movies/20130316-1900 ＮＨＫニュース７ [NHK総合].ts"),
//				ParcelFileDescriptor.MODE_READ_ONLY);
		//file, mode);fromFd(is.getFD().);
		Socket socket = new Socket() {
			@Override
			public void connect(SocketAddress remoteAddr) throws IOException {
//				super.connect(remoteAddr);
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return is;
			}
		};
		return ParcelFileDescriptor.fromSocket(socket);
//		Socket socket = new Socket(Prefs.ge, dstPort)
//		ParcelFileDescriptor.fromSocket(socket);
		// TODO Auto-generated method stub
//		return super.openFile(uri, mode);
	}


	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

}
