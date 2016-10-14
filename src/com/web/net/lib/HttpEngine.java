package com.web.net.lib;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import android.content.Context;

/***
 * http 引擎， 来 提供http请求
 * @author 罗政 上午11:21:54
 */
public class HttpEngine {
	private static HttpEngine CLIENT = null;
	private Context mContext ;
	private int timeout ;
	private boolean isCallInit = false ;
	
	public void init(Context context , int timeout){
		if(context == null){
			throw new NullPointerException("context is null ") ;
		}
		if(timeout <= 5000){
			timeout = 5000 ;
		}
		this.mContext = context ;
		isCallInit = true ;
	}
	public void init(Context context){
		if(context == null){
			throw new NullPointerException("context is null ") ;
		}
		timeout = 500 ;
		this.mContext = context ;
		isCallInit = true ;
	}
	public void destroy(){
		isCallInit = false ;
		mContext = null ;
		CLIENT = null ;
	}
	/***
	 * 发送 http 请求
	 * @param request
	 */
	public void http(Request request){
		if(null == request){
			throw new NullPointerException("request is null ") ;
		}
		if(!request.checkArgs()){
			throw new IllegalArgumentException(request.toString());
		}
		/***
		 * 不直接操作 真实的网络请求，而是生成代理对象，叫代理对象去进行网咯请求 
		 */
		HttpSubject realHttp = new HttpSubjectImpl() ; /*真实 主题 */
		InvocationHandler handler = new DynamicSubject(realHttp);
		HttpSubject proxyHttp = (HttpSubject) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
				HttpSubjectImpl.class.getInterfaces(), handler) ;  //代理 http
		proxyHttp.request(request); /*叫 代理 去请求http*/
	}
	
	public static HttpEngine getEngine(){
		if(CLIENT == null){
			synchronized (HttpEngine.class) {
				if(CLIENT == null){
					CLIENT = new HttpEngine() ;
				}
			}
		}
		return CLIENT ;
	}
	public Context getmContext() {
		return mContext;
	}
	public int getTimeout() {
		return timeout;
	}
	public boolean isCallInit() {
		return isCallInit;
	}
}
