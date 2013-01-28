package jp.syoboi.android.garaponmate.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.utils.Utils;

public class ProgramAdapter extends BaseAdapter {

	ArrayList<Program>	mItems = new ArrayList<Program>();
	Context				mContext;
	String				mSelection;
	int					mSelectedBackgroundColor;
	Matcher				mHighlightMatcher;
	int					mHighlightBgColor;

	public ProgramAdapter(Context context) {
		mContext = context;
		Resources res = context.getResources();
		mSelectedBackgroundColor = res.getColor(R.color.selectedColor);
		mHighlightBgColor = res.getColor(R.color.searchHighlightBgColor);
	}

	public void setItems(List<Program> programs) {
		mItems.clear();
		addItems(programs);
	}

	public void addItems(List<Program> programs) {
		for (Program p: programs) {
			mItems.add(p);
		}
		notifyDataSetChanged();
	}

	public void clear() {
		mItems.clear();
		notifyDataSetChanged();
	}

	public void setSelection(String selection) {
		if (!TextUtils.equals(mSelection, selection)) {
			mSelection = selection;
			notifyDataSetChanged();
		}
	}

	@Override
	public int getCount() {
		return mItems.size();
	}

	@Override
	public Program getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		ViewHolder vh;
		if (v == null) {
			v = View.inflate(mContext, R.layout.program, null);
			vh = new ViewHolder(v);
			v.setTag(vh);
		} else {
			vh = (ViewHolder) v.getTag();
		}

		Program p = mItems.get(position);
		vh.setItem(p, mHighlightMatcher, mHighlightBgColor);

		boolean selected = TextUtils.equals(p.gtvid, mSelection);
		v.setBackgroundColor(selected ? mSelectedBackgroundColor : 0);

		return v;
	}

	public void setHighlightMatcher(Matcher m) {
		mHighlightMatcher = m;
		notifyDataSetChanged();
	}

	static final int STARTTIME_FMT = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
			| DateUtils.FORMAT_SHOW_WEEKDAY;
	static final int ENDTIME_FMT = DateUtils.FORMAT_SHOW_TIME;

	private static class ViewHolder {
		TextView	mTime;
		TextView	mChName;
		TextView	mTitle;
		TextView	mDescription;

		public ViewHolder(View v) {
			mTime = (TextView) v.findViewById(R.id.time);
			mChName = (TextView) v.findViewById(R.id.chName);
			mTitle = (TextView) v.findViewById(R.id.title);
			mDescription = (TextView) v.findViewById(R.id.description);
		}

		public void setItem(Program p, Matcher m, int highlightColor) {
			Context context = mTime.getContext();
			int min = (int)(p.duration / 1000 / 60);

			String timeStr = String.format(Locale.ENGLISH, "%s - %s (%d:%02d)",
					DateUtils.formatDateTime(context, p.startdate, STARTTIME_FMT),
					DateUtils.formatDateTime(context, p.startdate + p.duration, ENDTIME_FMT),
					min / 60, min % 60
					);

			mTime.setText(timeStr);
			mChName.setText(Utils.convertCoolTitle(p.ch.bc));
			mTitle.setText(deco(p.title, m, highlightColor));
			mDescription.setText(deco(p.description, m, highlightColor));
		}

		CharSequence deco(CharSequence text, Matcher m, int bgColor) {
			text = Utils.convertCoolTitle(text);
			return Utils.highlightText(text, m, 0, bgColor);
		}
	}

}
