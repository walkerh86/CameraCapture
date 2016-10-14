package com.web.net.lib;

public interface HttpResponseListener {
	
	/***
	 * Json 解析
	 * @param <T>
	 * @param data
	 * @return
	 */
	public <T> T onJson(String data) ;
	/***
	 * http 请求成功
	 * @param <T>
	 * @param response
	 */
	public <T> void onSuccess(Response<T> response) ;
	/***
	 * http 请求失败
	 * @param code
	 * @param msg
	 * @param e
	 */
	public void onFailure(int code , String msg , Throwable e) ;
}
