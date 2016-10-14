package com.web;
public class Utils {

	private static Utils INSTANCE = null;
	
	/**主机*/
	private static String HOST = "127.0.0.1";// 公网
		
	/**端口*/
	private static int PORT = 8080 ; // 公网
	
	public String getURL_ROOT() {
		return "http://" + HOST + ":" + PORT + "/fm/router";
	}
	
	public static Utils getInstance() {
		if (INSTANCE == null) {
			synchronized (Utils.class) {
				if (INSTANCE == null) {
					INSTANCE = new Utils();
				}
			}
		}
		return INSTANCE;
	}
	
	
}
