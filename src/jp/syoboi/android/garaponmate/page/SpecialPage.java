package jp.syoboi.android.garaponmate.page;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.text.format.Time;
import android.util.SparseArray;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;

import jp.syoboi.android.garaponmate.GaraponClient.GaraponClientException;
import jp.syoboi.android.garaponmate.GaraponClient.Program;
import jp.syoboi.android.garaponmate.GaraponClient.SearchResult;
import jp.syoboi.android.garaponmate.GaraponClientUtils;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.Utils;

import org.json.JSONException;

public class SpecialPage {

	private static final SparseArray<String> sTemplates = new SparseArray<String>();

	private static String getTemplate(Context context, int id) {

		String template = sTemplates.get(id);
		if (template == null) {
			try {
				template = Utils.readStream(context.getResources().openRawResource(id), "utf-8");
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			sTemplates.put(id, template);
		}
		return template;
	}

	public static String getBroadcastingPage(Context context)
			throws MalformedURLException, NoSuchAlgorithmException, NotFoundException, IOException, JSONException, GaraponClientException {

		String template = getTemplate(context, R.raw.broadcasting);

		SearchResult sr = GaraponClientUtils.searchNowBroadcasting();

		Collections.sort(sr.program, new Comparator<Program>() {
			@Override
			public int compare(Program lhs, Program rhs) {
				return rhs.ch.ch - lhs.ch.ch;
			}
		});

		StringBuilder sb = new StringBuilder();

		long now = System.currentTimeMillis();
		Time t = new Time();
		Time t2 = new Time();
		for (Program p: sr.program) {

			t.set(p.startdate);
			t2.set(p.startdate + p.duration);

			sb.append("<li>");

			sb.append("<div class='barBg'>&nbsp;</div>");

			sb.append("<div class='bar'")
			.append(" start='" + p.startdate + "' duration='" + p.duration + "'")
			.append(">")
			.append("&nbsp;</div>");

			sb.append("<a href='/play?gtvid=" + p.gtvid + "'>");

			long durMin = (p.duration / 1000) / 60;
			sb
			.append("<span class='start'>")
			.append(t.format("%H:%M "))
			.append("</span>")
			.append("<span class='duration'>")
			.append(String.format("(%02d:%02d)", durMin / 60, durMin % 60))
			.append("</span>")
			.append("<span class='bc'>")
			.append(Utils.h(Utils.convertCoolTitle(p.ch.bc)))
			.append("</span>")
			.append("<span class='end'>")
			.append(t2.format("%H:%M"))
			.append("</span>")
			.append("<span class='title'>")
			.append(Utils.h(Utils.convertCoolTitle(p.title)))
			.append("</span>")
			;

			if (p.description.length() > 0) {
				sb.append("<span class='cmnt'>")
				.append(Utils.h(Utils.convertCoolTitle(p.description)))
				.append("</span>");
			}

			sb.append("</a>");
		}

		return template.replace("%LIST%", sb.toString());
	}
}
