package jp.syoboi.android.garaponmate.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.client.GaraponClient;

public class TSServer extends Service {
	static final String TAG = "TSServer";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, "onCreate");
		mServerThread.start();

	}


	Thread mServerThread = new Thread() {
		@Override
		public void run() {
			try {
				ServerSocket ss = new ServerSocket(50000);
				while (true) {
					Log.d(TAG, "accept");
					Socket socket = ss.accept();
					Log.d(TAG, "kita");

					ConnectionThread con = new ConnectionThread(socket);
					con.start();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		};
	};

	static class ConnectionThread extends Thread {

		static int COUNT = 0;
		final String TAG = "ConnectionThread" + (COUNT++);

		Socket mClient;
		public ConnectionThread(Socket socket) {
			mClient = socket;
		}
		@Override
		public void run() {
			super.run();

			Log.d(TAG, "connect from " + mClient.getInetAddress().toString());
			int dstPort = mClient.getPort();
			Log.d(TAG, "dstPort:" + dstPort);


			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
				String request;
				String inline = reader.readLine();
				request = inline;
	            while (reader.ready() && inline != null) {
	                Log.d(TAG, "INPUT:" + inline);

	                inline = reader.readLine();
	            }

//	            //"/cgi-bin/play/ts.cgi?file=" +
//	            String url = GaraponClient.getM3uUrl(Prefs.getGaraponHost(), "1SJP00211365519000", Prefs.getGtvSessionId());
//	            url = "http://" + Prefs.getGaraponHost()
//	            		+ "/cgi-bin/play/ts.cgi?file=21/1SJP00211365519000.ts&start=0&length=260004";
//	            InputStream is = new URL(url).openStream();
//
//	            OutputStream os = mClient.getOutputStream();
//
//	            byte [] buf = new byte [1024];
//	            int readSize;
//	            while ((readSize = is.read(buf)) != -1) {
//	            	os.write(buf, 0, readSize);
//	            }
//	            os.close();

	            OutputStream os = mClient.getOutputStream();

//	            int size = 1024;
//	            for (int j=0; j<10000000; j+=size) {
//	            	response("21/1SJP00211365519000.ts", os, j, j+size);
//	            }

	            if (request.contains(" /ts.cgi")) {
	            	Pattern ptn = Pattern.compile("ts.cgi.*file=([^&]+)&start=(\\d+)&length=(\\d+)");
	            	Matcher m = ptn.matcher(request);
	            	if (m.find()) {
	            		Log.v(TAG, "TSを返す");
	            		response(os, m.group(1),
	            				Integer.parseInt(m.group(2), 10),
	            				Integer.parseInt(m.group(3), 10));
	            	}
	            }
	            else {
            		Log.v(TAG, "m3uを返す");
	            	responseM3u(os);
	            }
	            os.flush();

	            reader.close();
	            mClient.close();

//	            Log.d(TAG, "url:" + url);

				Log.d(TAG, "END");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static void responseM3u(OutputStream os) throws MalformedURLException, IOException {
		String header = "HTTP/1.0 200 OK\r\n"
				+ "Content-Type: application/x-mpegurl\r\n"
				+ "Connection: closed\r\n"
				+ "\r\n";
		os.write(header.getBytes());

		String url = GaraponClient.getM3uUrl(
				Prefs.getGaraponHost(),
				"1SJP00211365519000",
				Prefs.getGtvSessionId());
		Log.d(TAG, "m3u:" + url);

        InputStream is = new URL(url).openStream();

        byte [] buf = new byte [1024];
        int readSize;
        while ((readSize = is.read(buf)) != -1) {
        	os.write(buf, 0, readSize);
        }
        is.close();
	}

	static void response(OutputStream os, String file, int start, int length) throws MalformedURLException, IOException {
		String header = "HTTP/1.0 200 OK\r\n"
				+ "Content-Type: video/mp2t\r\n"
				+ "Connection: closed\r\n"
				+ "\r\n";
		os.write(header.getBytes());

        String url = "http://" + Prefs.getGaraponHost()
        		+ "/cgi-bin/play/ts.cgi?file=" + file
        		+ "&start=" + start
        		+ "&length=" + length;
        InputStream is = new URL(url).openStream();

        byte [] buf = new byte [1024];
        int readSize;
        while ((readSize = is.read(buf)) != -1) {
        	os.write(buf, 0, readSize);
        }
        is.close();
	}
}
