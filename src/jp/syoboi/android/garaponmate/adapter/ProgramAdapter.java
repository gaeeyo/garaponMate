package jp.syoboi.android.garaponmate.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.LeadingMarginSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.Caption;
import jp.syoboi.android.garaponmate.data.ImageLoader;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.utils.Utils;
import jp.syoboi.android.garaponmate.view.MyImageView;

public class ProgramAdapter extends BaseAdapter {

	ArrayList<Program>	mItems = new ArrayList<Program>();
	Context				mContext;
	String				mSelection;
	Matcher				mHighlightMatcher;
	int					mHighlightBgColor;

	public ProgramAdapter(Context context) {
		mContext = context;
		Resources res = context.getResources();
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
			v = View.inflate(mContext, R.layout.search_result_row, null);
			vh = new ViewHolder(v);
			v.setTag(vh);
		} else {
			vh = (ViewHolder) v.getTag();
		}

		Program p = mItems.get(position);
		vh.bind(p, mHighlightMatcher, mHighlightBgColor);

		boolean selected = TextUtils.equals(p.gtvid, mSelection);
		Drawable d = v.getBackground();
		d.setLevel(selected ? 1 : 0);

		return v;
	}

	public void setHighlightMatcher(Matcher m) {
		mHighlightMatcher = m;
		notifyDataSetChanged();
	}

	static final int STARTTIME_FMT = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
			| DateUtils.FORMAT_SHOW_WEEKDAY;
	static final int ENDTIME_FMT = DateUtils.FORMAT_SHOW_TIME;

	static final SpannableStringBuilder sTmpSb = new SpannableStringBuilder();

	public static class ViewHolder {
		public Program		mProgram;

		TextView	mTime;
		TextView	mChName;
		TextView	mTitle;
		TextView	mDescription;
		TextView	mCaption;
		MyImageView	mThumbnail;
		int			mIndent;
		ImageLoader	mImageLoader;

		public ViewHolder(View v) {
			mTime = (TextView) v.findViewById(R.id.time);
			mChName = (TextView) v.findViewById(R.id.chName);
			mTitle = (TextView) v.findViewById(R.id.title);
			mDescription = (TextView) v.findViewById(R.id.description);
			mCaption = (TextView) v.findViewById(R.id.caption);
			mThumbnail = (MyImageView) v.findViewById(R.id.thumbnail);
			mImageLoader = App.from(v.getContext()).getImageLoader();
		}

		public void bind(Program p, Matcher m, int highlightColor) {
			mProgram = p;

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

			if (TextUtils.isEmpty(p.description)) {
				mDescription.setVisibility(View.GONE);
			} else {
				mDescription.setText(deco(p.description, m, highlightColor));
				mDescription.setVisibility(View.VISIBLE);
			}

			if (p.caption.length > 0) {

				if (mIndent == 0) {
					mIndent = (int)mDescription.getPaint().measureText("00:00:00 ");
				}

				SpannableStringBuilder sb = sTmpSb;
				sb.clear();

				int start = sb.length();
				for (Caption caption: p.caption) {
					if (sb.length() > 0) {
						sb.append('\n');
					}
					sb.append(Utils.formatDuration(caption.time))
					.append(" ")
					.append(deco(caption.text, m, highlightColor));

				}
				sb.setSpan(new LeadingMarginSpan.Standard(0, mIndent), start, sb.length(),
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				mCaption.setText(sb);
				mCaption.setVisibility(View.VISIBLE);
			} else {
				mCaption.setVisibility(View.GONE);
			}

			if (mThumbnail != null) {
				String url = "http://" + Prefs.getGaraponHost() + "/thumbs/" + p.gtvid;
				mImageLoader.loadImage(mThumbnail, url, 0, 0, 0, false, 0, null);
			}
		}

		CharSequence deco(CharSequence text, Matcher m, int bgColor) {
			text = Utils.convertCoolTitle(text);
			return Utils.highlightText(text, m, 0, bgColor);
		}
	}

}
