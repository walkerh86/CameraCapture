package com.cj;

import com.camera.setting.servics.BootCameraService;
import com.camera.setting.utils.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

public class CjReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(final Context context, Intent intent) {
		String action = intent.getAction();
		Log.i(ConfigUtil.TAG_UPGRADE,"CjReceiver action="+action);
		if("android.intent.action.PACKAGE_REPLACED".equals(action)){
			Uri data = intent.getData();
			if(data != null){
				String updatepkg = data.getEncodedSchemeSpecificPart();
				Log.i(ConfigUtil.TAG_UPGRADE,"CjReceiver pkg="+updatepkg);
				String thisPkg = context.getPackageName();
				Log.i(ConfigUtil.TAG_UPGRADE,"CjReceiver thisPkg="+thisPkg);
				if(TextUtils.equals(updatepkg,thisPkg)){
					appHotReboot(context);
				}
			}
		}
	}

	private void appHotReboot(Context context){
		Log.i(ConfigUtil.TAG_UPGRADE, "appHotReboot");
		/*not work for system->data, only work for data->data
		Intent intent = new Intent();
		intent.setClass(context, BootCameraService.class);
		intent.setAction(Utils.CAMERA_START_ACTION);
		
		PendingIntent restartIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);       
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 50, restartIntent); 
		//context.startService(intent);
		android.os.Process.killProcess(android.os.Process.myPid());
		*/
		PowerManager powerManager=(PowerManager) context.getSystemService(Context.POWER_SERVICE); 
		powerManager.reboot("reboot");
	}
}
