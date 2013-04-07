package jp.syoboi.android.garaponmate.data;

import java.io.IOException;

import jp.syoboi.android.util.JksnUtils.JksnObject;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

public class GaraponAccount {
	public String garaponId;
	public String password;

	public GaraponAccount(String garaponId, String pass) {
		this.garaponId = garaponId;
		this.password = pass;
	}

	public static GaraponAccount parse(JksnObject j) {
		GaraponAccount account = new GaraponAccount(
			j.getString("garaponId"),
			j.getString("password"));
		return account;
	}

	public void write(JsonGenerator g) throws JsonGenerationException, IOException {
		g.writeStartObject();
		g.writeStringField("garaponId", garaponId);
		g.writeStringField("password", password);
		g.writeEndObject();
	}
}
