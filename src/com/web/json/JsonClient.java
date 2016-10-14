package com.web.json;

import com.web.vo.Device;

public class JsonClient {

	private static final JsonClient client = new JsonClient();
	BusinessJsonParser parser = new BusinessJsonParser();

	public static JsonClient client() {
		return client;
	}

	public Device parseUpdate(String json) {
		return parser.parseDevice(json);
	}

}
