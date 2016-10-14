package com.web.net.lib;

public class Response<T> {
	/***
	 * 返回的对象
	 */
	private T mode ;
	private String errorMsg ;
	private int code ;
	private Throwable e ;
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public Throwable getE() {
		return e;
	}
	public void setE(Throwable e) {
		this.e = e;
	}
	public T getMode() {
		return mode;
	}
	public void setMode(T t) {
		this.mode = t;
	}
}
