package jp.syoboi.android.garaponmate.data;

import android.content.Context;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import jp.syoboi.android.garaponmate.GaraponClient.Program;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.data.ProgSearch.ProgSearchMatcher;
import jp.syoboi.android.util.JksnUtils;
import jp.syoboi.android.util.JksnUtils.JksnArray;
import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerator;

public class ProgSearchList extends ArrayList<ProgSearch> {

	private static final long serialVersionUID = -1013245574651926511L;

	private static ProgSearchList sInstance;

	public static synchronized ProgSearchList getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new ProgSearchList(context);
		}
		return sInstance;
	}

	private ProgSearchList(Context context) {

		try {
			JksnObject jo = (JksnObject) JksnUtils.parseJson(Prefs.getProgSearchList());
			JksnArray ja = jo.getArray("items");
			for (int j=0; j<ja.size(); j++) {
				JksnObject ji = ja.getObject(j);
				add(new ProgSearch(ji));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

//		ProgSearch f = new ProgSearch();
//		f.kwOr = "爆笑問題 菅野美穂 世界";
//
//		add(f);
	}

	@Override
	public boolean isEmpty() {
		for (ProgSearch p: this) {
			if (!p.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public String toJson() {
		StringWriter sw = new StringWriter();
		try {
			JsonGenerator jg = JksnUtils.getFactory().createJsonGenerator(sw);
			jg.writeStartObject();
			jg.writeFieldName("items");
			jg.writeStartArray();
			for (ProgSearch ps: this) {
				ps.toJson(jg);
			}
			jg.writeEndArray();
			jg.writeEndObject();
			jg.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sw.toString();

	}

	public ProgSearchListMatcher getMatchers() {
		return new ProgSearchListMatcher(this);
	}

	public static class ProgSearchListMatcher {

		ProgSearchMatcher [] mMatchers;

		public ProgSearchListMatcher(ProgSearchList list) {
			mMatchers = new ProgSearchMatcher[list.size()];
			for (int j=list.size()-1; j>=0; j--) {
				mMatchers[j] = list.get(j).getMatcher();
			}
		}

		public boolean match(Program p) {
			for (ProgSearchMatcher m: mMatchers) {
				if (m.match(p)) {
					return true;
				}
			}
			return false;
		}
	}
}
