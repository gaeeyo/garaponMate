package jp.syoboi.android.garaponmate.data;

import java.io.Serializable;

import jp.syoboi.android.garaponmate.client.GaraponClient;
import jp.syoboi.android.util.JksnUtils.JksnObject;

public class Caption implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -5129258415525339687L;

	public String text;
	public int time;

	public Caption(JksnObject j) {
		text = j.getString("caption_text");
		time = (int)GaraponClient.parseTimeStr(j.getString("caption_time"));
	}
}
