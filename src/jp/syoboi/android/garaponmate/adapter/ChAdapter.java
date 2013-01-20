package jp.syoboi.android.garaponmate.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.client.GaraponClient.Ch;

public class ChAdapter extends ArrayAdapter<Ch> {

	public ChAdapter(Context context, int notSelectedTextId) {
		super(context, android.R.layout.simple_dropdown_item_1line);

		if (notSelectedTextId != 0) {
			Ch ch = new Ch(0, context.getString(notSelectedTextId), "");
			add(ch);
		}

		for (Ch ch: App.getChList().toArray(true)) {
			add(ch);
		}
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).ch;
	}

}
