package com.camera.setting.utils;

import java.io.File;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
public class Utils {
	private static final String TAG = Utils.class.getSimpleName();
	public static final String XML_INFO = "xml_info";
	
	public static final String FTP_CONNECT_SUCCESSS = "ftp connect success";
	public static final String FTP_CONNECT_FAIL = "ftp connect fail";
	public static final String FTP_DISCONNECT_SUCCESS = "ftp disconnect success";
	public static final String FTP_FILE_NOTEXISTS = "ftp file not exists";
	
	public static final String FTP_UPLOAD_SUCCESS = "ftp upload success";
	public static final String FTP_UPLOAD_FAIL = "ftp upload fail";
	public static final String FTP_UPLOAD_LOADING = "ftp upload loading";

	public static final String FTP_DOWN_LOADING = "ftp down loading";
	public static final String FTP_DOWN_SUCCESS = "ftp down success";
	public static final String FTP_DOWN_FAIL = "ftp down fail";
	
	public static final String FTP_DELETEFILE_SUCCESS = "ftp delete success";
	public static final String FTP_DELETEFILE_FAIL = "ftp delete fail";
	
	public static final String KEY_SYS_INIT = "key_sys_init";//是否第一次安装程序
	public static final String KEY_FTP_VERSION="key_ftp_version";
	public static final String KEY_FTP_REBOOTIEM="key_ftp_rebootiem";
	public static final String KEY_FTP_TRIGGERMODEL="key_ftp_triggermodel";//触发模式
	public static final String KEY_FTP_TAKINGMODEL="key_ftp_takingmodel";//拍照模式
	public static final String KEY_FTP_FILESIZE="key_ftp_filesize";
	public static final String KEY_FTP_IMG_TIEM="key_ftp_img_tiem";
	public static final String KEY_FTP_IMG_BRIGHTNESS="key_ftp_img_brightness";
	public static final String KEY_FTP_IMG_WHITE_BALANCE="key_ftp_img_white_balance";
	public static final String KEY_FTP_IMG_AUTO_LIGHTE="key_ftp_auto_light";
	public static final String KEY_FTP_URL="key_ftp_url";
	public static final String KEY_FTP_USERNAME="key_ftp_user";
	public static final String KEY_FTP_PASSWORD="key_ftp_pass";
	public static final String KEY_FTP_TIEM1="key_ftp_tiem1";
	public static final String KEY_FTP_TIEM2="key_ftp_tiem2";
	public static final String KEY_FTP_TIEM3="key_ftp_tiem3";
	public static final String KEY_FTP_TIEM4="key_ftp_tiem4";
	public static final String KEY_FTP_TIEM5="key_ftp_tiem5";
	public static final String KEY_FTP_TIEM6="key_ftp_tiem6";
	
	public static final String CAMERA_TAKEPICTURE_ACTION = "android.camera.TAKEPICTURE";
	public static final String CAMERA_START_ACTION = "android.camera.START";
	public static final String DEIVCE_RBOOT_ACTION = "android.deivce.ALARM_RBOOT";
	public static final String CAMERA_UPLOAD_SERVICE_ACTION = "android.camera.service.UPLOAD";
	public static final String CAMERA_UPLOAD_FILE_SERVICE_ACTION = "android.camera.service.UPLOAD_FILE";
	public static final String CAMERA_APK_UPDATE_ACTION = "android.camera.service.UPDATE_APK";
	
	public static final String DEIVCE_ALARM_START_1_ACTION = "android.deivce.ALARM_START_1";//开启定时
	public static final String DEIVCE_ALARM_START_2_ACTION = "android.deivce.ALARM_START_2";
	public static final String DEIVCE_ALARM_START_3_ACTION = "android.deivce.ALARM_START_3";
	public static final String DEIVCE_ALARM_START_4_ACTION = "android.deivce.ALARM_START_4";
	public static final String DEIVCE_ALARM_START_5_ACTION = "android.deivce.ALARM_START_5";
	public static final String DEIVCE_ALARM_START_6_ACTION = "android.deivce.ALARM_START_6";
	
	public static final String DEIVCE_ALARM_CANCEL_1_ACTION = "android.deivce.ALARM_CANCEL_1";//清理定时
	public static final String DEIVCE_ALARM_CANCEL_2_ACTION = "android.deivce.ALARM_CANCEL_2";
	public static final String DEIVCE_ALARM_CANCEL_3_ACTION = "android.deivce.ALARM_CANCEL_3";
	public static final String DEIVCE_ALARM_CANCEL_4_ACTION = "android.deivce.ALARM_CANCEL_4";
	public static final String DEIVCE_ALARM_CANCEL_5_ACTION = "android.deivce.ALARM_CANCEL_5";
	public static final String DEIVCE_ALARM_CANCEL_6_ACTION = "android.deivce.ALARM_CANCEL_6";
	public static final String DEIVCE_ALARM_TIME_ERROR_ACTION = "android.deivce.ALARM_TIME_ERROR";
//	public static final String KEY_GPIO_OUT_1 = "key_gpio_out_1";
//	public static final String KEY_GPIO_OUT_2 = "key_gpio_out_2";
//	public static final String KEY_GPIO_OUT_3 = "key_gpio_out_3";//485触发时 把对应值改为 1;
//	public static final String KEY_GPIO_OUT_4 = "key_gpio_out_4";
	
	public static final String INTENT_TAKINGMODEL = "intent_takingmodel"; 
	public static final String INTENT_FILESIZE = "intent_file_szie";
	public static final String INTENT_AUTO_LIGHT = "intent_auto_light";
	public static final String INTENT_CAMERA_NAME_JPG = "intent_camera_name_jpg";
	public static final String INTENT_CAMERA_PATH = "intent_camera_path";
	public static final String INTENT_UPLOAD_XML = "intent_upload_xml";

	public static final String SYSTEM_KEY_DEIVCES_ID = "ro.serialno";
	public static final String DURING_DAY_MODEL= "android.deivce.DURING_DAY_MODEL";

	public static final String IMG_PATH = "//mnt/sdcard/DCIM/Camera/";
	public static final String XML_PATH = "//sdcard/state.xml";
	public static final String apk_PATH = "//sdcard/";
	
	public static boolean registered = false;
	
	public static void saveProperty(Context context,String key,String value){
		SharedPreferences preference = context.getSharedPreferences(XML_INFO, Context.MODE_PRIVATE);
		Editor editor = preference.edit();
		editor.putString(key, value);
		editor.apply();
	}
	
	public static String getProperty(Context context,String key){
		SharedPreferences preference = context.getSharedPreferences(XML_INFO, Context.MODE_PRIVATE);
		return preference.getString(key, "");
	}
	
	public static void saveSysProperty(Context context,String key,boolean isInit){
		SharedPreferences preference = context.getSharedPreferences(XML_INFO, Context.MODE_PRIVATE);
		Editor editor = preference.edit();
		editor.putBoolean(key, isInit);
		editor.apply();
	}
	
	public static boolean getSysProperty(Context context,String key){
		SharedPreferences preference = context.getSharedPreferences(XML_INFO, Context.MODE_PRIVATE);
		return preference.getBoolean(key, true);
	}
	
	/** 
	 * 删除单个文件 
	 * @param   file    被删除文件的文件名 
	 * @return 单个文件删除成功返回true，否则返回false 
	 */  
	public static boolean deleteFile(File file) {  
		boolean flag = false;  
	    // 路径为文件且不为空则进行删除  
	    if (file.isFile() && file.exists()) {  
	        file.delete();  
	        flag = true;  
	    }  
	    Log.i(TAG, "--deleteFile "+file.getAbsolutePath()+file.getName()+" "+flag);
	    return flag;  
	} 
	
	public static boolean isMsmSuccess(String info){
		if(info == null){
			return false;
		}else if (info.length() > 6 && info.substring(0,6).equals("123456")){
			return true;
		}else{
			return false;
		}
	}
	
	
	/**
	 * 获取从几点 到 几点, 多少分钟一次
	 * @param tiem
	 * @return
	 */
	public static String[] getTiem(String tiem) {
		String[] str = new String[]{"0","0","0"};
		String str1 = tiem.replaceAll(" ", ""); 
		  if(str1 == null || str1.equals("")){
			  return str;
		  }else if(str1.indexOf("-") == -1){
			  return str;
		  }else{
			  return str1.split("-");
		  }
	}
}
