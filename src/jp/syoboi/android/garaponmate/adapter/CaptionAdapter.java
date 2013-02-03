package jp.syoboi.android.garaponmate.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.data.Caption;
import jp.syoboi.android.garaponmate.utils.Utils;

public class CaptionAdapter extends ArrayAdapter<Caption> {

	public CaptionAdapter(Context context) {
		super(context, 0);
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		ViewHolder vh;
		if (v == null) {
			v = View.inflate(getContext(), R.layout.caption_row, null);
			vh = new ViewHolder(v);
			v.setTag(vh);
		} else {
			vh = (ViewHolder) v.getTag();
		}

		vh.bind(getItem(position));

		return v;
	}


	private static class ViewHolder {
		public TextView mTime;
		public TextView	mText;

		public ViewHolder(View v) {
			mTime = (TextView) v.findViewById(R.id.time);
			mText = (TextView) v.findViewById(R.id.text);
		}

		public void bind(Caption caption) {
			mTime.setText(Utils.formatDuration(caption.time));
			mText.setText(caption.text);
		}
	}
}
