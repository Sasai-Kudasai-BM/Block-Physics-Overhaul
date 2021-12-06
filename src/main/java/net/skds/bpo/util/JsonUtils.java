package net.skds.bpo.util;

import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtils {
	
	public static boolean getOrDefaultB(JsonObject jo, String key, Supplier<Boolean> defaultVal) {
		JsonElement je = jo.get(key);
		if (je == null) {
			return defaultVal.get();
		}
		return je.getAsBoolean();
	}

	public static int getOrDefaultI(JsonObject jo, String key, Supplier<Integer> defaultVal) {
		JsonElement je = jo.get(key);
		if (je == null) {
			return defaultVal.get();
		}
		return je.getAsInt();
	}

	public static float getOrDefaultF(JsonObject jo, String key, Supplier<Float> defaultVal) {
		JsonElement je = jo.get(key);
		if (je == null) {
			return defaultVal.get();
		}
		return je.getAsFloat();
	}
}
