package jp.syoboi.android.garaponmate.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.DialogActivity;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.utils.Utils;

public class DownloadService extends Service {

	private static final String TAG = "DownloadService";

	static int 						sTaskId = 0;
	HashMap<String,DownloadTask>	mTaskMap = new HashMap<String,DownloadTask>();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (App.DEBUG) {
			Log.d(TAG, "onCreate");
		}
	}

	@Override
	public void onDestroy() {
		if (App.DEBUG) {
			Log.d(TAG, "onDestroy");
		}
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			if (Intent.ACTION_INSERT.equals(action)) {
				Program p = (Program) intent.getSerializableExtra(App.EXTRA_PROGRAM);

				final String id = p.gtvid;
				DownloadTask task = mTaskMap.get(id);
				if (task == null) {
					task = new DownloadTask(this, p, sTaskId++) {
						@Override
						protected void onPostExecute(Object result) {
							mTaskMap.remove(id);
							stopOrContinue();
						}
						@Override
						protected void onCancelled(Object result) {
							mTaskMap.remove(id);
							stopOrContinue();
						}
					};
					mTaskMap.put(id, task);
					task.execute();
				}
			}
			else if (Intent.ACTION_DELETE.equals(action)) {
				Program p = (Program) intent.getSerializableExtra(App.EXTRA_PROGRAM);
				final String id = p.gtvid;
				DownloadTask task = mTaskMap.get(id);
				if (task != null) {
					task.cancel(true);
				}
			}
		}
		stopOrContinue();
		return START_STICKY;
	}

	void stopOrContinue() {
		if (mTaskMap.size() == 0) {
			stopSelf();
		}
	}

	/**
	 * m3uファイルをダウンロードして、記述されているURLの配列を返す
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static ArrayList<String> downloadM3u(String url) throws IOException, InterruptedException {
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new URL(url).openStream(), "utf-8"));
		String line;

		ArrayList<String> urls = new ArrayList<String>();
		while ((line = br.readLine()) != null) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			if (line.startsWith("#")) {
				continue;
			}
			if (line.length() > 0) {
				urls.add(line);
			}
		}
		return urls;
	}


	/**
	 * 動画のダウンロードタスク
	 */
	private static class DownloadTask extends AsyncTask<Object,Object,Object> {

		private Program mProgram;
		private NotificationManager mNm;
		private Notification	mNotification;
		private Context			mContext;
		private PendingIntent	mPi;
		private long			mDownloadedSize = 0;
		private long			mTotalContentLength;
		private File			mDst;
		private long			mDownloadProgressUpdated;
		private int				mId;

		public DownloadTask(Context context, Program p, int id) {
			mId = id;
			mProgram = p;
			mContext = context;
			mNm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
			mDst = getDstFile(p);

			Intent intent = new Intent(mContext, DialogActivity.class);
			intent.setAction(App.ACTION_DOWNLOAD_NOTIFICATION_CLICKED);
			intent.putExtra(App.EXTRA_PROGRAM, mProgram);
			mPi = PendingIntent.getActivity(mContext, 1, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);

		}

		@Override
		protected Object doInBackground(Object... params) {

			RandomAccessFile out = null;
			boolean success = false;
			try {
				String m3u = GaraponClientUtils.getM3uUrl(mProgram.gtvid);

				out = new RandomAccessFile(mDst, "rw");

				ArrayList<String> urls = downloadM3u(m3u);

				calcContentLength(urls);

				for (int j=0; j<urls.size(); j++) {
					if (isCancelled()) {
						break;
					}

					String url = urls.get(j);
					String tsUrl = new URI(m3u).resolve(url).toString();

					downloadFile(tsUrl, out);
				}
				if (!isCancelled()) {
					success = true;
					showSuccessNotification();
				}
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				showErrorNotification(e);
				return e;
			} finally {
				mNm.cancel(App.NOTIFY_ID_DOWNLOADING + mId);
				if (out != null) {
					try {
						out.close();
						if (!success) {
							mDst.delete();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private void calcContentLength(List<String> urls) throws Exception {

			Matcher m = Pattern.compile("start=(\\d+)&length=(\\d+)").matcher("");

			// ファイルサイズを計算
			long total = 0;
			for (String url: urls) {
				m.reset(url);
				if (!m.find()) {
					throw new Exception("想定外のTSのURL");
				}
				long size = Long.valueOf(m.group(2), 10);
				total += size;
			}

			mTotalContentLength = total;
		}

		/**
		 * エラー通知
		 * @param e
		 */
		@SuppressWarnings("deprecation")
		void showErrorNotification(Exception e) {
			Notification notification = new Notification(
					android.R.drawable.stat_notify_error,
					mContext.getString(R.string.downloadError),
					System.currentTimeMillis());

			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(mContext,
					mContext.getString(R.string.downloadError),
					String.valueOf(e.getMessage()),
					mPi);
			mNm.notify(App.NOTIFY_ID_DOWNLOAD_RESULT + mId, notification);
		}

		/**
		 * ダウンロード完了通知
		 */
		@SuppressWarnings("deprecation")
		void showSuccessNotification() {
			Notification notification = new Notification(
					android.R.drawable.stat_sys_download_done,
					mContext.getString(R.string.downloadComplete),
					System.currentTimeMillis());

			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setDataAndType(Uri.fromFile(mDst), "video/ts");

			PendingIntent pi = PendingIntent.getActivity(mContext, mId, i,
					PendingIntent.FLAG_ONE_SHOT);

			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(mContext,
					mContext.getString(R.string.downloadComplete),
					mDst.getName(),
					pi);
			mNm.notify(App.NOTIFY_ID_DOWNLOAD_RESULT + mId, notification);
		}

		/**
		 * 進捗通知
		 */
		@SuppressWarnings("deprecation")
		void showDownloadProgress() {
			final long MB = 1024*1024;
			long now = System.currentTimeMillis();
			if (now - mDownloadProgressUpdated > DateUtils.SECOND_IN_MILLIS) {
				mDownloadProgressUpdated = now;
				String msg = String.format(Locale.ENGLISH,
						"%d%% (%dMB/%dMB) %s",
						mDownloadedSize * 100 / mTotalContentLength,
						mDownloadedSize / MB, mTotalContentLength / MB,
						mDst.getName());

				if (mNotification == null) {
					mNotification = new Notification(
							android.R.drawable.stat_sys_download,
							mContext.getString(R.string.downloading),
							System.currentTimeMillis());
					mNotification.flags = Notification.FLAG_ONGOING_EVENT;
				}
				mNotification.setLatestEventInfo(mContext,
						mContext.getString(R.string.downloading),
						msg, mPi);

				mNm.notify(App.NOTIFY_ID_DOWNLOADING + mId, mNotification);
			}
		}

		private int downloadFile(String url, RandomAccessFile out) throws MalformedURLException, IOException {
			InputStream is = null;
			int totalReadSize = 0;
			try {
				is = Utils.openConnection(url).getInputStream();

				byte [] buf = new byte [8192];
				int readSize;
				while ((readSize = is.read(buf)) >= 0) {
					out.write(buf, 0, readSize);
					totalReadSize += readSize;
					mDownloadedSize += readSize;
					showDownloadProgress();
				}
			} finally {
				if (is != null) {
					is.close();
				}
			}
			return totalReadSize;
		}

		static File getDstFile(Program p) {
			String fileName = getFilename(p);

			File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
			File file = new File(dir, fileName + ".ts");
			return file;
		}

		static String getFilename(Program p) {
			String filename =  p.title;
			if (filename == null) {
				filename = p.gtvid;
			}

			Time t = new Time();
			t.set(p.startdate);
			String dateTimeText = t.format("%Y%m%d-%H%M");

			filename = dateTimeText + " " + filename.replaceAll("./:\\*\\?\\|<>", "_")
					+ " [" + p.ch.bc + "]";

			return filename;
		}

	}
}
