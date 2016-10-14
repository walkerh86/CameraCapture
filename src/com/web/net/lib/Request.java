package com.web.net.lib;

import java.util.HashMap;
import java.util.Map;

import android.os.Handler;

public class Request {
	/***
	 * 当前 请求类型 ,get/post
	 */
	private Type currentRequestType = null;
	/***
	 * 请求url
	 */
	private String url = null;
	/**
	 * post 请求 必须传递的参数map
	 */
	private Map<String, String> requestParms;

	private HttpResponseListener httpResponseListener;

	public static class Builder {
		/***
		 * 请求url
		 */
		private String url = null;
		/**
		 * post 请求 必须传递的参数map
		 */
		private Map<String, String> requestParms;

		private HttpResponseListener httpResponseListener;

		public Builder(String url) {
			this.url = url;
			this.requestParms = new HashMap<String, String>();
		}

		public Builder setMap(Map<String, String> requestParms) {
			this.requestParms.putAll(requestParms);
			return this;
		}

		public Builder setParam(String key, String value) {
			this.requestParms.put(key, value);
			return this;
		}

		public Builder setListener(HttpResponseListener httpResponseListener) {
			this.httpResponseListener = httpResponseListener;
			return this;
		}

		public Request build() {
			return new Request(this);
		}

	}

	public Request(Builder builder) {
		this.url = builder.url;
		this.requestParms = builder.requestParms;
		this.httpResponseListener = builder.httpResponseListener;
		if (requestParms == null || requestParms.size() == 0) {
			this.currentRequestType = Type.GET;
		} else {
			this.currentRequestType = Type.POST;
		}
	}
	/***
	 * 检测 必须参数是否合法或已经填写
	 * 
	 * @return
	 */
	public boolean checkArgs() {
		if (this.url == null) {
			return false;
		}
		if (this.currentRequestType == Type.POST) {
			if (this.requestParms == null) {
				this.requestParms = new HashMap<String, String>();
			}
		}
		return true;
	}

	public Type getCurrentRequestType() {
		return currentRequestType;
	}

	public String getUrl() {
		return url;
	}

	public Map<String, String> getRequestParms() {
		return requestParms;
	}
	/**
	 * 获取 http响应监听器
	 * 
	 * @return
	 */
	HttpResponseListener getHttpResponseListener() {
		return httpResponseListener;
	}

	@Override
	public String toString() {
		return "Request [currentRequestType=" + currentRequestType + ", url="
				+ url + ", requestParms=" + requestParms + "]";
	}

	public void http() {
		HttpEngine.getEngine().http(this);
	}

	public void http(long delayMillis) {
		if (delayMillis < 0 || delayMillis > 20000) {
			delayMillis = 0;
		}
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				HttpEngine.getEngine().http(Request.this);
			}
		}, delayMillis);
	}
}
