package jp.syoboi.android.garaponmate.view;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.Genre;
import jp.syoboi.android.garaponmate.data.GenreGroup;
import jp.syoboi.android.garaponmate.data.GenreGroupList;
import jp.syoboi.android.garaponmate.data.Program;

public class PlayerDetailView extends FrameLayout {

	TextView	mText;

	public PlayerDetailView(Context context, AttributeSet attrs) {
		super(context, attrs);

		inflate(context, R.layout.player_detail_view, this);

		mText = (TextView) findViewById(R.id.playerDetailText);
	}

	public void setProgram(Program p) {
		Context context = getContext();
		Resources res = context.getResources();

		SpannableStringBuilder sb = new SpannableStringBuilder();
		int start;

		if (!TextUtils.isEmpty(p.title)) {
			sb.append(p.title).append("\n\n");
		}

		if (!TextUtils.isEmpty(p.description)) {
			start = sb.length();
			sb.append(res.getString(R.string.programDetail))
			.append(":\n");
			sb.setSpan(new TextAppearanceSpan(context, R.style.playerUiSectionTextAppearance),
					start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			sb.append(p.description).append("\n\n");
		}

		if (p.genre.length > 0) {
			start = sb.length();
			sb.append(res.getString(R.string.genre)).append(":\n");
			sb.setSpan(new TextAppearanceSpan(context, R.style.playerUiSectionTextAppearance),
					start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

//			boolean added = false;
			for (int genre: p.genre) {
				GenreGroupList ggl = GenreGroupList.getInstance(getContext());
				GenreGroup g1 = ggl.findByValue(Genre.getGenre0(genre));
				if (g1 == null) {
					continue;
				}
				Genre g2 = g1.findByValue(Genre.getGenre1(genre));
				if (g2 == null) {
					continue;
				}
//				if (added) {
//					sb.append(", ");
//				}
				sb.append(g1.name + " > " + g2.name).append("\n");
//				added = true;
			}
		}

		sb.setSpan(
				new BackgroundColorSpan(res.getColor(R.color.playerUiBackgroundColor)),
				0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		mText.setText(sb);
	}

}
