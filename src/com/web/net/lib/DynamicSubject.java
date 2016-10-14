package com.web.net.lib;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
/***
 * 动态代理 
 * @author 罗政 上午11:41:06
 */
public class DynamicSubject implements InvocationHandler{
	private Object obj ;
	public DynamicSubject(Object obj) {
		this.obj = obj ;
	}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		
		Object res = method.invoke(obj, args) ;
		
		return res;
	}
}
