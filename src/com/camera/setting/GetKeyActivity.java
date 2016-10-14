package com.camera.setting;

import com.camera.setting.utils.Utils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
/**
 * 
 * @author Dahai.zhou
 * @see 电平触发  中断触发
 */
public class GetKeyActivity extends Activity {
	private static final String TAG = GetKeyActivity.class.getSimpleName();
    private TextView textView; 
    private static long lastClickTime;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "--onCreate");
		setContentView(R.layout.get_key);
		textView = (TextView)findViewById(R.id.get_key_TextView);
		String deivcesId = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
		textView.setText("设备ID:"+deivcesId);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "--onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "--onPause");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.i(TAG, "--keyCode:" + event.getScanCode());
		textView.setText("获取键值信息:"+event.getScanCode());
		if (event.getScanCode() == 184 || event.getScanCode() == 88 ) {
			cameraTakepicture(event.getScanCode());
		}
		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME){
			return true;
		}else{
			 return false; //默认返回 false
//			 return super.onKeyDown(keyCode, event);
		}
	}
	
	 public synchronized static boolean isFastClick() {
	        long time = System.currentTimeMillis();   
	        if ( time - lastClickTime < 3000) {   
	            return true;   
	        }   
	        lastClickTime = time;   
	        return false;   
	    }
	
	 public void cameraTakepicture(int scanCode) {
	        if (!isFastClick()) {
	        	Intent intent = new Intent();
				intent.setAction(Utils.CAMERA_TAKEPICTURE_ACTION);
				String takingModel = Utils.getProperty(GetKeyActivity.this,Utils.KEY_FTP_TAKINGMODEL);
				String fileSize = Utils.getProperty(GetKeyActivity.this,Utils.KEY_FTP_FILESIZE);
				if(scanCode == 184){//电平测试
					intent.putExtra(Utils.INTENT_TAKINGMODEL, 1);
					intent.putExtra(Utils.INTENT_FILESIZE,  1);
				}else{
					//正常电平工作
					intent.putExtra(Utils.INTENT_TAKINGMODEL, Integer.parseInt(takingModel));
					intent.putExtra(Utils.INTENT_FILESIZE,  Integer.parseInt(fileSize));
				}
				sendBroadcast(intent);
	        }
	    }
	 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "--onDestroy");
	}
}
