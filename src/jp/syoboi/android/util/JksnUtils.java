package jp.syoboi.android.util;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

public class JksnUtils {

	static JsonFactory sFactory = new JsonFactory();

	public static JsonFactory getFactory() {
		return sFactory;
	}

	public static class JksnObject extends HashMap<String,Object> {
		private static final long serialVersionUID = 0L;

		public String getString(String key) {
			Object value = get(key);
			if (value == null) {
				return null;
			}
			return value.toString();
		}

		public String getString(String key, String fallback) {
			if (containsKey(key)) {
				return getString(key);
			}
			return fallback;
		}

		public long getLong(String key) {
			try {
				return ((Long)get(key)).longValue();
			} catch (ClassCastException e) {
				return Long.valueOf((String)get(key));
			}
		}

		public long getLong(String key, long fallback) {
			try {
				return getLong(key);
			} catch (NullPointerException e) {
				return fallback;
			}
		}

		public boolean getBoolean(String key) {
			return ((Boolean)get(key)).booleanValue();
		}

		public boolean getBoolean(String key, boolean fallback) {
			try {
				return getBoolean(key);
			} catch (NullPointerException e) {
				return fallback;
			}
		}

		public int getInt(String key)  {
			try {
				return ((Long)get(key)).intValue();
			} catch (ClassCastException e) {
				return Integer.valueOf((String)get(key));
			}
		}

		public int getInt(String key, int fallback) {
			try {
				return getInt(key);
			} catch (NullPointerException e) {
				return fallback;
			}
		}

		public JksnArray getArray(String key) {
			return (JksnUtils.JksnArray)get(key);
		}

		public JksnObject getObject(String key) {
			return (JksnUtils.JksnObject)get(key);
		}

		public boolean isNull(String key)  {
			return get(key) == null;
		}

		public double getDouble(String key) {
			try {
				return ((Double)get(key)).doubleValue();
			} catch (ClassCastException e) {
				return Double.valueOf((String)get(key));
			}
		}

		public double getDouble(String key, double fallback) {
			try {
				return getDouble(key);
			} catch (NullPointerException e) {
				return fallback;
			}
		}

	}

	public static class JksnArray extends ArrayList<Object> {
		private static final long serialVersionUID = 0L;
		public JksnObject getObject(int index) {
			return (JksnObject)get(index);
		}
		public String getString(int index) {
			return get(index).toString();
		}
	}

	public static Object parseJson(String json) throws JsonParseException, IOException {
		return parse(sFactory.createJsonParser(json), null);
	}
	public static Object parseJson(String json, JksnArrayCallback callback) throws JsonParseException, IOException {
		return parse(sFactory.createJsonParser(json), callback);
	}

	public static Object parseJson(InputStream is, JksnArrayCallback callback) throws JsonParseException, IOException {
		return parse(sFactory.createJsonParser(is), callback);
	}

	public static Object parse(JsonParser jp, JksnArrayCallback callback) throws IOException {
		try {
			JsonToken token = jp.nextToken();
			if (token == null) {
				return null;
			}
			switch(token) {
			case START_ARRAY:
				return parseArray(jp, callback);
			case START_OBJECT:
				return parseObject(jp, callback);
			default:
				throw new IllegalStateException("JSON must be OBJECT OR ARRAY");
			}
		}
		finally {
			jp.close();
		}
	}


	static void dumpMap(JksnObject map, String indent) {
		for (String key : map.keySet()) {
			Object value = map.get(key);
			dumpObject(value, indent + " ", key);
		}
	}

	static void dumpArray(JksnArray list, String indent) {
		for (Object value : list) {
			dumpObject(value, indent + " ", "(array)");
		}
	}

	static void dumpObject(Object value, String indent, String name) {
		if (value instanceof HashMap) {
			Log.v("", indent + name + ": (map)");
			dumpMap((JksnObject) value, indent + " ");
		} else if (value instanceof ArrayList) {
			Log.v("", indent + name + ": (array)");
			dumpArray((JksnArray) value, indent + " ");
		} else {
			Log.v("", indent + name + ":" + value.toString());
		}
	}

	static JksnArray parseArray(JsonParser jp, JksnArrayCallback callback) throws JsonParseException,
			IOException {

		// Callback対象かどうか判定
		boolean isTarget = (callback != null && callback.isTargetArray(jp, jp.getCurrentName()));

		// Callback対象のオブジェクトの場合、入れ子になっているオブジェクトに対ししてCallbackが発生しないようにする
		JksnArrayCallback childCallback = callback;
		if (isTarget) {
			childCallback = null;
		}

		JksnArray items = new JksnArray();
		JsonToken token;
		while ((token = jp.nextToken()) != JsonToken.END_ARRAY) {
			switch (token) {
			case START_OBJECT:
				JksnObject obj = parseObject(jp, childCallback);
				if (!isTarget || !callback.onTargetObject(jp, obj)) {
					items.add(obj);
				}
				break;
			case START_ARRAY:
				items.add(parseArray(jp, childCallback));
				break;
			default:
				items.add(jp.getText());
				break;
			}
		}
		return items;
	}

	static JksnObject parseObject(JsonParser jp, JksnArrayCallback callback)
			throws JsonParseException, IOException {
		JksnObject map = new JksnObject();
		while ((jp.nextToken()) != JsonToken.END_OBJECT) {
			String fieldName = jp.getCurrentName();
			switch (jp.nextToken()) {
			case START_ARRAY:
				map.put(fieldName, parseArray(jp, callback));
				break;
			case START_OBJECT:
				map.put(fieldName, parseObject(jp, callback));
				break;
			case VALUE_NULL:
				map.put(fieldName, null);
				break;
			case VALUE_NUMBER_FLOAT:
				map.put(fieldName, jp.getDoubleValue());
				break;
			case VALUE_NUMBER_INT:
				map.put(fieldName, jp.getLongValue());
				break;
			case VALUE_TRUE:
				map.put(fieldName, true);
				break;
			case VALUE_FALSE:
				map.put(fieldName, false);
				break;
			default:
				map.put(fieldName, jp.getText());
				break;
			}
		}
		return map;
	}

	/**
	 * TreeとStreamの中間
	 * 配列中のオブジェクトが1つ完成するたびにCallbackする
	 */
	public static interface JksnArrayCallback {
		/**
		 * Callback対象の配列かどうかを判定して返す
		 * @param jp
		 * @return trueを返すと配列無いのオブジェクトが完成するごとにonTargetObjectがCallbackされる
		 */
		public boolean isTargetArray(JsonParser jp, String name);
		/**
		 * Callback対象のオブジェクトが完成したら呼び出される
		 * @param jp
		 * @param j
		 * @return JksnObjectを処理し、不要だったらtrueを返す。
		 * trueを返すと最終的に返されるオブジェクト内には含まれなくなる
		 */
		public boolean onTargetObject(JsonParser jp, JksnObject j);
	}
}
