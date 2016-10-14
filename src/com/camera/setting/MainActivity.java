package com.camera.setting;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.camera.setting.receiver.AlarmReceiver;
import com.camera.setting.receiver.BootBroadcastReceiver;
import com.camera.setting.utils.Utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
//	public static final String TEST_REBOOT = "action.test.reboot";
//	public static final String TEST_MSM = "action.test.msm";
//	public static final String TEST_PHONE = "action.test.phone";
//	public static final String TEST_TAKEPICTURE = "android.test.takepicture";
	private BootBroadcastReceiver receiver;

	// ftpClient = new FtpClientUtil("120.55.65.142", 21, "ftp01", "Kand2004");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.i(TAG, "--onCreate");
		receiver = new BootBroadcastReceiver();
	}

	
	 /**
		 * 启动定时任务
		 * @param context
		 * @param firstime  开始时
		 * @param minutes 开始分
		 * @param minute 每隔多少秒
		 */
		private void startTakepictureAlarm(Context context,int hour,int minutes,int minute) {
			AlarmManager am = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
			Calendar calendar =Calendar.getInstance(Locale.getDefault());
			calendar.setTimeInMillis(System.currentTimeMillis()); 
			calendar.set(Calendar.HOUR_OF_DAY, hour);        //是24小时制，第二参数几小时以后 
	        calendar.set(Calendar.MINUTE, minutes);            //设置闹钟的分钟数
	        calendar.set(Calendar.SECOND, 0);                //设置闹钟的秒数
	        calendar.set(Calendar.MILLISECOND, 0);            //设置闹钟的毫秒数
	        long firstTime = SystemClock.elapsedRealtime(); // 开机之后到现在的运行时间(包括睡眠时间)  
	        long selectTime = calendar.getTimeInMillis();//选择的定时时间
	        long systemTime = System.currentTimeMillis();  
	        // 计算现在时间到设定时间的时间差  
	        long time = selectTime - systemTime;  
	        firstTime += time;
			Intent intent = new Intent(context, AlarmReceiver.class);
			intent.setAction(Utils.DEIVCE_ALARM_START_1_ACTION);
			PendingIntent sender = PendingIntent.getBroadcast(context,0, intent,PendingIntent.FLAG_CANCEL_CURRENT);
			am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, minute*1000, sender);  
		}
		
		
		/**
		 * 关闭定时任务
		 * @param context
		 * @param hour
		 */
		private void closeTakepictureAlarm(Context context,int hour,int minutes) {
			AlarmManager am = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
			Calendar calendar =Calendar.getInstance(Locale.getDefault());
			calendar.setTimeInMillis(System.currentTimeMillis()); 
			calendar.set(Calendar.HOUR_OF_DAY, hour);        //是24小时制，第二参数几小时以后 
	        calendar.set(Calendar.MINUTE, minutes);            //设置闹钟的分钟数
	        calendar.set(Calendar.SECOND, 0);                //设置闹钟的秒数
	        calendar.set(Calendar.MILLISECOND, 0);            //设置闹钟的毫秒数
	        long firstTime = SystemClock.elapsedRealtime(); // 开机之后到现在的运行时间(包括睡眠时间)  
	        long selectTime = calendar.getTimeInMillis();//选择的定时时间
	        long systemTime = System.currentTimeMillis();  
	        // 计算现在时间到设定时间的时间差  
	        long time = selectTime - systemTime;  
	        firstTime += time;
			Intent intent = new Intent(context, AlarmReceiver.class);
			intent.setAction(Utils.DEIVCE_ALARM_CANCEL_1_ACTION);
			PendingIntent sender = PendingIntent.getBroadcast(context,0, intent,PendingIntent.FLAG_CANCEL_CURRENT);
			am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, sender); 
		}


	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "--onResume");
		IntentFilter intent = new IntentFilter();
//		intent.addAction(TEST_REBOOT);
//		intent.addAction(TEST_MSM);
//		intent.addAction(TEST_PHONE);
//		intent.addAction(TEST_TAKEPICTURE);
		registerReceiver(receiver, intent);
//		startTakepictureAlarm(this, 22,35, 4);
//		closeTakepictureAlarm(this, 22,36);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "--onPause");
	}

	public void onReboot(View v) {
		Toast.makeText(getApplicationContext(), "模拟开机发送广播成功",
				Toast.LENGTH_SHORT).show();
		// 模拟开机
//		sendBroadcast(new Intent(TEST_REBOOT));
	}

	public void onMSM(View v) {
		Toast.makeText(getApplicationContext(), "模拟短信发送广播成功",
				Toast.LENGTH_SHORT).show();
		// 模拟短信
//		sendBroadcast(new Intent(TEST_PHONE));
	}

	public void onPhone(View v) {
		Toast.makeText(getApplicationContext(), "模拟来电发送广播成功",
				Toast.LENGTH_SHORT).show();
//		PowerManager pManager=(PowerManager) getSystemService(Context.POWER_SERVICE); 
//        pManager.reboot("reboot");
//		sendBroadcast(new Intent(TEST_PHONE));
	}


	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "--onDestroy");
		if (receiver != null) {
			unregisterReceiver(receiver);
		}
	}
}
