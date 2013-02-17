package jp.syoboi.android.garaponmate.view;

import android.app.Activity;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.LeadingMarginSpan;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.activity.MainActivity;
import jp.syoboi.android.garaponmate.client.SyoboiClient.Histories;
import jp.syoboi.android.garaponmate.client.SyoboiClient.History;
import jp.syoboi.android.garaponmate.client.SyoboiClientUtils;
import jp.syoboi.android.garaponmate.data.Caption;
import jp.syoboi.android.garaponmate.data.ImageLoader;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.fragment.base.MainBaseFragment;
import jp.syoboi.android.garaponmate.utils.Utils;

public class ProgramGridView extends FrameLayout {

	private static final String TAG = "ProgramGridView";

	private static ArrayList<Program> PROGRAM_EMPTY = new ArrayList<Program>();

	private int		mGridWidth;
	List<Program>	mPrograms = PROGRAM_EMPTY;
	int 			mGridMinWidth;
	SearchParam		mSearchParam;
	Matcher			mHighlightMatcher;
	int				mHighlightColor;
	int				mCols;
	Histories		mHistories;

	public ProgramGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mGridMinWidth = Math.round(130 * getResources().getDisplayMetrics().density);
		mHighlightColor = context.getResources().getColor(R.color.searchHighlightBgColor);
		mHistories = SyoboiClientUtils.getHistories(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();


		if (widthMode == MeasureSpec.EXACTLY) {
			updateGridWidth(widthSize);
		}

		int maxChildHeight = 0;
		boolean fillChildHeight = false;
		int childCount = getChildCount();
		int gridWidth = mGridWidth;
		for (int j=0; j<childCount; j++) {
			View child = getChildAt(j);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
			int marginWidth = lp.leftMargin + lp.rightMargin;
			int marginHeight = lp.topMargin + lp.bottomMargin;
			int childWidth = gridWidth - marginWidth;
			int childHeight = heightSize - marginHeight;
			child.measure(
					MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(childHeight, heightMode));

			childHeight = child.getMeasuredHeight() + marginHeight;

			if (j > 0 && childHeight != maxChildHeight) {
				fillChildHeight = true;
			}
			if (childHeight > maxChildHeight) {
				maxChildHeight = childHeight;
			}
		}
		if (fillChildHeight) {
			for (int j=0; j<childCount; j++) {
				View child = getChildAt(j);
				FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
				int marginWidth = lp.leftMargin + lp.rightMargin;
				int marginHeight = lp.topMargin + lp.bottomMargin;
				int childWidth = gridWidth - marginWidth;
				int childHeight = maxChildHeight - marginHeight;
				child.measure(
						MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
			}
		}

		setMeasuredDimension(widthSize, maxChildHeight);

//		if (App.DEBUG) {
//			Log.v(TAG, String.format("widthSpec:%s heightSpec:%s measured:%d, %d  grid:%d  cols:%d",
//					MeasureSpec.toString(widthMeasureSpec),
//					MeasureSpec.toString(heightMeasureSpec),
//					getMeasuredWidth(), getMeasuredHeight(),
//					mGridWidth, mCols));
//		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		int childCount = getChildCount();

		int x = getPaddingLeft();
		int y = getPaddingTop();

		for (int j=0; j<childCount; j++) {
			View child = getChildAt(j);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
			int cx = x + j * mGridWidth + lp.leftMargin;
			int cy = y + lp.topMargin;
			child.layout(cx, cy, cx + mGridWidth, cy + child.getMeasuredHeight());
		}
	}

	void updateGridWidth(int width) {
		int gridWidth = width / 6;
		if (gridWidth < mGridMinWidth) {
			gridWidth = width / (width / mGridMinWidth);
		}
		if (mGridWidth != gridWidth) {
			mGridWidth = gridWidth;
			int cols = width / gridWidth;
			if (cols != mCols) {
				mCols = cols;
				setupChilds(width);
			}
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

		if (mGridWidth <= 0) {
			return;
		}

		int cols = width / mGridWidth;

		int childCount = getChildCount();
		for (int j=childCount; j<cols; j++) {
			View.inflate(getContext(), R.layout.search_param_row_program, this);
		}

		for (int j=childCount-1; j>=cols; j--) {
			removeViewAt(j);
		}

		for (int j=0; j<cols; j++) {
			View child = getChildAt(j);
			if (j < mPrograms.size()) {
				child.setVisibility(View.VISIBLE);
				bindProgram(child, mPrograms.get(j));
			} else {
				child.setVisibility(View.INVISIBLE);
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
			vh2.mThumbContainer.setOnClickListener(mOnProgramClickListener);
			vh2.mThumbContainer.setOnLongClickListener(mOnProgramLongClickListener);
		}
		vh2.bind(p, mHighlightMatcher, mHighlightColor, mHistories);
	}

	View.OnClickListener mOnProgramClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Program program = getProgram(v);
			if (program != null) {
				if (getContext() instanceof MainActivity) {
					MainActivity activity = (MainActivity)getContext();
					activity.playVideo(program);
				}
			}
		}
	};

	View.OnLongClickListener mOnProgramLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			final Program program = getProgram(v);
			v.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
					Activity activity = (Activity) v.getContext();
					MainBaseFragment.inflateProgramMenu(activity, menu, v, menuInfo, program);
				}
			});
			v.showContextMenu();
			return true;
		}
	};

	static Program getProgram(View v) {
		v = (View)v.getParent();

		if (v.getVisibility() != View.VISIBLE) {
			return null;
		}
		Object tag = v.getTag();
		if (tag instanceof ViewHolder) {
			ViewHolder vh = (ViewHolder) tag;

			return vh.mProgram;
		}
		return null;
	}

	static SpannableStringBuilder sTmpSb = new SpannableStringBuilder();
	static final int STARTTIME_FMT = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
			| DateUtils.FORMAT_SHOW_WEEKDAY;

	public static class ViewHolder {
		public Program		mProgram;

		View		mThumbContainer;
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
			mThumbContainer = v.findViewById(R.id.thumbContainer);
			mTime = (TextView) v.findViewById(R.id.time);
			mDuration = (TextView) v.findViewById(R.id.duration);
			mChName = (TextView) v.findViewById(R.id.chName);
			mTitle = (TextView) v.findViewById(R.id.title);
			mDescription = (TextView) v.findViewById(R.id.description);
			mCaption = (TextView) v.findViewById(R.id.caption);
			mThumbnail = (MyImageView) v.findViewById(R.id.thumbnail);
			mImageLoader = App.from(v.getContext()).getImageLoader();
		}

		public void bind(Program p, Matcher m, int highlightColor, Histories histories) {
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
			mTitle.setText(Utils.convertCoolTitle(p.title));

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
				mDescription.setVisibility(View.GONE);
			} else {
				mCaption.setVisibility(View.GONE);
			}

			if (mThumbnail != null) {
				String url = "http://" + Prefs.getGaraponHost() + "/thumbs/" + p.gtvid;
				mImageLoader.loadImage(mThumbnail, url, 0, 0, 0, false,
						R.drawable.video_empty, null);

				History history = histories.get(p.gtvid);
				mThumbnail.setAlpha(history != null ? 0.4f : 1);
			}
		}

		CharSequence deco(CharSequence text, Matcher m, int bgColor) {
			text = Utils.convertCoolTitle(text);
			return Utils.highlightText(text, m, 0, bgColor);
		}
	}
}
