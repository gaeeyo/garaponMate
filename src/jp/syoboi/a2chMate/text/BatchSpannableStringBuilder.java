package jp.syoboi.a2chMate.text;

import android.text.SpannableString;
import android.text.Spanned;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * StringBuilder と SpannableStringBuilder が使いにくいので変わりに作ったもの
 *
 * @author ikeno
 *
 */
public class BatchSpannableStringBuilder  {

	StringBuilder			mSb = new StringBuilder();
	ArrayList<SpanInfo> 	mSpans = new ArrayList<SpanInfo>();
	LinkedList<SpanInfo> 	mSpanRecyle = new LinkedList<SpanInfo>();
	int						mStart;

	public void clear() {
		for (SpanInfo si: mSpans) {
			mSpanRecyle.add(si);
		}
		mSpans.clear();
		mSb.delete(0, mSb.length());
	}

	public BatchSpannableStringBuilder start() {
		mStart = mSb.length();
		return this;
	}

	public int indexOf(String s, int start) {
		return mSb.indexOf(s, start);
	}

	public BatchSpannableStringBuilder append(CharSequence s, Object what) {
		SpanInfo si = obtainSpanInfo();
		si.what = what;
		si.start = mSb.length();
		si.end = si.start + s.length();
		mSpans.add(si);

		mSb.append(s);
		return this;
	}

	public int length() {
		return mSb.length();
	}

	public BatchSpannableStringBuilder append(CharSequence s) {
		mSb.append(s);
		return this;
	}

	public BatchSpannableStringBuilder append(int s) {
		mSb.append(Integer.toString(s));
		return this;
	}

	public BatchSpannableStringBuilder append(long s) {
		mSb.append(Long.toString(s));
		return this;
	}

	public BatchSpannableStringBuilder append(char s) {
		mSb.append(s);
		return this;
	}

	public StringBuilder getStringBuilder() {
		return mSb;
	}
	
	public BatchSpannableStringBuilder setSpan(Object what, int start, int end) {
		SpanInfo si = obtainSpanInfo();
		si.start = start;
		si.end = end;
		si.what = what;
		mSpans.add(si);
		return this;
	}

	public BatchSpannableStringBuilder setSpan(Object what) {
		return setSpan(what, mStart, mSb.length());
	}

	public Spanned toSpanned() {
		SpannableString ss = new SpannableString(mSb);
		for (SpanInfo si: mSpans) {
			ss.setSpan(si.what, si.start, si.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return ss;
	}


	private SpanInfo obtainSpanInfo() {
		SpanInfo si = mSpanRecyle.poll();
		if (si == null) {
			si = new SpanInfo();
		}
		return si;
	}

	private static class SpanInfo {
		public int start;
		public int end;
		public Object what;
	}
}
