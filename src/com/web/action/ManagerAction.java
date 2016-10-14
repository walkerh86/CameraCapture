package com.web.action;

import java.util.HashMap;
import java.util.Map;

import android.os.SystemProperties;

import com.camera.setting.utils.Utils;
import com.web.json.JsonClient;
import com.web.net.lib.HttpResponseListener;
import com.web.net.lib.Request;
import com.web.net.lib.Response;
import com.web.vo.Device;
//com.deepsoft.fm.LoginActivity 
public class ManagerAction {
	public void addDeivce() {
    	String deivcesId = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
    	long systemTime = System.currentTimeMillis();
    	String time = Long.toString(systemTime);
    	Map<String, String> map = new HashMap<String, String>() ;
		map.put("deivcesId",deivcesId) ;
		map.put("time",time) ;
		new Request.Builder(com.web.Utils.getInstance().getURL_ROOT()).setMap(map)
		.setListener(new HttpResponseListener() {
			@Override
			public <T> void onSuccess(Response<T> t) {
				
			}
			@Override
			public <T> T onJson(String data) {
//				Device genericModel = JsonClient.client().parseOuterJson(data) ;
//				if(genericModel != null){
//					/***
//					 * 登录 和注册 返回json格式一样， 可通用
//					 */
//					return (T) JsonClient.client().parseRegister(genericModel.getData());
//				}
				return null;
			}
			@Override
			public void onFailure(int code, String msg, Throwable e) {
			}
		}).build().http();
	}  
      
      
//      private void setdata() {
//    	long systemTime = System.currentTimeMillis();
//  		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
//  		java.util.Date dt = new Date(systemTime);
//  		String dateTime = sdf1.format(dt);
//  		System.out.println(dateTime);
//	}
}
