package com.web.json;

import com.google.gson.Gson;
import com.web.vo.Device;

public class BusinessJsonParser {
	public Device parseDevice(String json) {
		Gson gson = new Gson() ;
		Device Device = gson.fromJson(json, Device.class);
		return Device;
	}
}
