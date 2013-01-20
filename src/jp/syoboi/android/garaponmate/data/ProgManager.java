package jp.syoboi.android.garaponmate.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.garaponmate.client.SearchParam;
import jp.syoboi.android.garaponmate.client.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.utils.FileUtils;
import jp.syoboi.android.util.JksnUtils;

import org.codehaus.jackson.JsonGenerator;

public class ProgManager {

	private static final String TAG = "ProgManager";

	private static final String DB_NAME = "prog";
	private static final int DB_VERSION = 1;

	private static ProgManager sInstance;
	private File	mBaseDir;
	private ProgramSyncTask	mRefreshTask;
	private SQLiteDatabase mDB;

	public static void init(Context context, File dir) {
		if (sInstance == null) {
			sInstance = new ProgManager(context, dir);
		}
	}

	public static ProgManager getInstance() {
		return sInstance;
	}

	private ProgManager(Context context, File dir) {
		mBaseDir = dir;
		mDB = new OpenHelper(context).getWritableDatabase();

//		mDB.query(true, ProgColumns.TABLE_NAME, null,
//				selection, selectionArgs, groupBy, having, orderBy, limit, cancellationSignal)
	}

	public void refresh() {
		if (mRefreshTask != null) {
			return;
		}
		mRefreshTask = new ProgramSyncTask() {
			@Override
			protected void onPostExecute(Object result) {
				super.onPostExecute(result);
				finishTask();
			}
			@Override
			protected void onCancelled() {
				super.onCancelled();
				finishTask();
			}

			void finishTask() {
				mRefreshTask = null;
			}
		};
		mRefreshTask.execute(mBaseDir, mDB);
	}

	private static class ProgramSyncTask extends AsyncTask<Object, Object, Object> {

		@Override
		protected Object doInBackground(Object... params) {
			File baseDir = (File) params[0];
			SQLiteDatabase db = (SQLiteDatabase) params[1];

			String ipaddr = Prefs.getIpAdr();
			String sessionId = Prefs.getGtvSessionId();

			baseDir.mkdirs();

			SearchParam param = new SearchParam();
			try {
				int total = 0;
				for (int j=1; j<1000; j++) {
					param.page = j;
					param.count = 100;
					Log.v(TAG, "search page:" + j);
					SearchResult sr;
					try {
						sr = GaraponClient.search(ipaddr, sessionId, param);
						total += sr.program.size();

						saveProgList(new File(baseDir, "prog" + j + ".json"), sr.program);

						db.beginTransaction();
						try {
							ContentValues values = new ContentValues();
							for (Program p: sr.program) {
								values.put(ProgColumns.GTVID, p.gtvid);
								values.put(ProgColumns.FLAG, p.flag);
								values.put(ProgColumns.STARTDATE, p.startdate);
								values.put(ProgColumns.DURATION, p.duration);
								values.put(ProgColumns.TITLE, p.title);
								values.put(ProgColumns.DESCRIPTION, p.description);
								values.put(ProgColumns.CH, p.ch.ch);
								db.insert(ProgColumns.TABLE_NAME, "", values);
							}
							db.setTransactionSuccessful();
						} finally {
							db.endTransaction();
						}

						Time t = new Time();
						int idx = sr.program.size();
						if (idx > 0) {
							Program last = sr.program.get(idx - 1);
							t.set(last.startdate);
							Log.v(TAG, "最後の番組 " + t.format2445() + " title:" + last.title);
						}
						Log.v(TAG, "SearchResult hit:" + sr.hit + " program:" + sr.program.size()
								+ " total:" + total
								);

						if (sr.program.size() == 0) {
							break;
						}
					} catch (MalformedURLException e1) {
						e1.printStackTrace();
						break;
					} catch (IOException e1) {
						e1.printStackTrace();
						break;
					}
					try {
						Thread.sleep(10*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			finally {
				FileUtils.copyFile(new File(db.getPath()), new File("/sdcard/epg.db"));
			}
			return null;
		}
	}

	static void saveProgList(File file, List<Program> items) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		JsonGenerator jg = null;
		try {
			jg = JksnUtils.getFactory().createJsonGenerator(fos);
			jg.writeStartObject();
			jg.writeFieldName("program");
			jg.writeStartArray();
			for (Program p: items) {
				p.write(jg);
			}
			jg.writeEndArray();
			jg.writeEndObject();

		} finally {
			try {
				if (jg != null) {
					jg.flush();
				}
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static class OpenHelper extends SQLiteOpenHelper {

		public OpenHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(ProgColumns.CREATE_TABLE);
			db.execSQL(ProgColumns.CREATE_INDEX_STARTDATE);
			db.execSQL(ProgColumns.CREATE_INDEX_CH);
			db.execSQL(ProgColumns.CREATE_INDEX_FLAG);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}

	private static class ProgColumns {
		public static final String TABLE_NAME = "Programs";

		public static final String GTVID = "gtvid";
		public static final String STARTDATE = "startdate";
		public static final String DURATION = "duration";
		public static final String CH = "ch";
		public static final String TITLE = "title";
		public static final String DESCRIPTION = "description";
		public static final String FLAG = "flag";

		public static final String CREATE_TABLE =
				"CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
				+ GTVID + " TEXT NOT NULL PRIMARY KEY ON CONFLICT IGNORE,"
				+ STARTDATE + " INTEGER NOT NULL,"
				+ DURATION + " INTEGER NOT NULL,"
				+ CH + " INTEGER NOT NULL,"
				+ TITLE + " TEXT NOT NULL,"
				+ DESCRIPTION + " TEXT NOT NULL,"
				+ FLAG + " INTEGER NOT NULL"
				+ ")"
				;
		public static final String CREATE_INDEX_STARTDATE =
				"CREATE INDEX IF NOT EXISTS "
				+ TABLE_NAME + "_" + STARTDATE
				+ " ON " + TABLE_NAME + "("
				+ STARTDATE + ")";
		public static final String CREATE_INDEX_CH =
				"CREATE INDEX IF NOT EXISTS "
				+ TABLE_NAME + "_" + CH
				+ " ON " + TABLE_NAME + "("
				+ CH + ")";
		public static final String CREATE_INDEX_FLAG =
				"CREATE INDEX IF NOT EXISTS "
				+ TABLE_NAME + "_" + FLAG
				+ " ON " + TABLE_NAME + "("
				+ FLAG + ")";
	}

}
