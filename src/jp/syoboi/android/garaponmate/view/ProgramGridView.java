package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.LeadingMarginSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.data.Caption;
import jp.syoboi.android.garaponmate.data.ImageLoader;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.utils.Utils;

public class ProgramGridView extends LinearLayout {

	private static final String TAG = "ProgramGridView";

	private static ArrayList<Program> PROGRAM_EMPTY = new ArrayList<Program>();

	private int		mGridWidth;
	List<Program>	mPrograms = PROGRAM_EMPTY;
	int 			mGridMinWidth;
	SearchParam		mSearchParam;
	Matcher			mHighlightMatcher;

	public ProgramGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOrientation(VERTICAL);
		mGridMinWidth = Math.round(130 * getResources().getDisplayMetrics().density);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int width = MeasureSpec.getSize(widthMeasureSpec);

		if (widthMode == MeasureSpec.EXACTLY) {
			updateGridWidth(width);
		}
//		Log.v(TAG, "widthMode:" + widthMode + " width:" + width);

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	}

	void updateGridWidth(int width) {
		int gridWidth = Math.max(mGridMinWidth, width / 5);
		if (mGridWidth != gridWidth) {
			mGridWidth = gridWidth;
			setupChilds(width);
		}
	}

	public void setPrograms(List<Program> programs, SearchParam sp) {
		if (programs == null) {
			mPrograms = PROGRAM_EMPTY;
		} else {
			mPrograms = programs;
		}
		mSearchParam = sp;

		mHighlightMatcher = null;
		if (mSearchParam != null) {
			mHighlightMatcher = mSearchParam.createMatcher();
		}

		setupChilds(getWidth());
	}

	void setupChilds(int width) {

//		Log.v(TAG, "setupChilds " + this);

		if (width <= mGridWidth) {
			for (int j=getChildCount()-1; j>=0; j--) {
				getChildAt(j).setVisibility(View.GONE);
			}
			return;
		}

		int rows = 1;
		for (int j=getChildCount(); j<rows; j++) {
			LinearLayout ll = new LinearLayout(getContext());
			addView(ll, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		}
		for (int j=getChildCount()-1; j>=rows; j--) {
			getChildAt(j).setVisibility(View.GONE);
		}


		int idx = 0;
		for (int j=0; j<rows; j++) {
			LinearLayout ll = (LinearLayout) getChildAt(j);

			int cols = width / mGridWidth;
//			Log.v(TAG, "setupChilds cols:"+ cols + " width:" + width + " gridWidth:" + mGridWidth);

			for (int x=ll.getChildCount(); x<cols; x++) {
				View.inflate(getContext(), R.layout.search_param_row_program, ll);
			}

			for (int x=ll.getChildCount()-1; x>=cols; x--) {
				ll.getChildAt(x).setVisibility(View.GONE);
			}

			for (int x=0; x<cols; x++, idx++) {
				View v = ll.getChildAt(x);
				if (idx < mPrograms.size()) {
					v.setVisibility(View.VISIBLE);
					bindProgram(v, mPrograms.get(idx));
				} else {
					v.setVisibility(View.INVISIBLE);
//					bindProgram(v, null);
				}
			}
		}
	}

	void bindProgram(View v, Program p) {
		Object tag = v.getTag();
		ViewHolder vh2;
		if (tag instanceof ViewHolder) {
			vh2 = (ViewHolder) tag;
		} else {
			vh2 = new ViewHolder(v);
			v.setTag(vh2);
			v.setOnClickListener(mOnProgramClickListener);
		}
		vh2.bind(p, mHighlightMatcher, 0);
	}

	View.OnClickListener mOnProgramClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.getVisibility() != View.VISIBLE) {
				return;
			}
			Object tag = v.getTag();
			if (tag instanceof ViewHolder) {
				ViewHolder vh = (ViewHolder) tag;

				if (getContext() instanceof MainActivity) {
					MainActivity activity = (MainActivity)getContext();
					activity.playVideo(vh.mProgram);
				}
			}
		}
	};



	static SpannableStringBuilder sTmpSb = new SpannableStringBuilder();
	static final int STARTTIME_FMT = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
			| DateUtils.FORMAT_SHOW_WEEKDAY;

	public static class ViewHolder {
		public Program		mProgram;

		TextView	mTime;
		TextView	mDuration;
		TextView	mChName;
		TextView	mTitle;
		TextView	mDescription;
		TextView	mCaption;
		MyImageView	mThumbnail;
		int			mIndent;
		ImageLoader	mImageLoader;

		public ViewHolder(View v) {
			mTime = (TextView) v.findViewById(R.id.time);
			mDuration = (TextView) v.findViewById(R.id.duration);
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

			long now = System.currentTimeMillis();
			CharSequence timeStr;
			if (now - p.startdate < DateUtils.DAY_IN_MILLIS) {
				timeStr = DateUtils.getRelativeDateTimeString(
						context, p.startdate, DateUtils.MINUTE_IN_MILLIS,
						DateUtils.DAY_IN_MILLIS, STARTTIME_FMT);
			} else {
				timeStr = DateUtils.formatDateTime(
						context, p.startdate, STARTTIME_FMT);
			}

			mDuration.setText(Utils.formatDurationMinute(p.duration));

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
				mImageLoader.loadImage(mThumbnail, url, 0, 0, 0, false,
						R.drawable.video_empty, null);
			}
		}

		CharSequence deco(CharSequence text, Matcher m, int bgColor) {
			text = Utils.convertCoolTitle(text);
			return Utils.highlightText(text, m, 0, bgColor);
		}
	}
}
