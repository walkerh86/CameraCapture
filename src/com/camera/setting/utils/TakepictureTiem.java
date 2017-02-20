package com.camera.setting.utils;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import com.camera.setting.receiver.AlarmReceiver;
import com.camera.setting.receiver.BootBroadcastReceiver;
import com.camera.setting.servics.BootCameraService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
//+ by hcj @{
import com.cj.ConfigUtil;
import com.cj.LogManager;

import android.util.Log;
//+ by hcj @}

/**
 * 
 * @author dahai.zhou
 * @data 2016年6月1日 14:51:45
 * @see 六个闹钟定时任务
 *      防止定时任务死锁
 */
public class TakepictureTiem {
//	private static final String TAG = TakepictureTiem.class.getSimpleName();
	public AlarmManager alarmManager;
	private Context mContext;
	public static PendingIntent sender1;
	public static PendingIntent sender2;
	public static PendingIntent sender3;
	public static PendingIntent sender4;
	public static PendingIntent sender5;
	public static PendingIntent sender6;
	public static int startMin=0;
	
	private int albarmTimers = 0;
	
	public TakepictureTiem(Context context) {
		this.alarmManager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		this.mContext = context;
	}
	
	/**
	 * 第一个定时抓拍任务
	 * @param context
	 * @param hour1  开始时间
	 * @param hour2  结束时间
	 * @param minute 每个多少分钟
	 */
	public void setTakepictureTask1(int hour1,int hour2,int minute) {
		startTakepictureTiem(Utils.DEIVCE_ALARM_START_1_ACTION, hour1,hour2, minute,sender1,1);
		closeTakepictureTiem(Utils.DEIVCE_ALARM_CANCEL_1_ACTION, hour2,1);
	}
	/**
	 * 第二个定时抓拍任务
	 * @param context
	 * @param hour1  开始时间
	 * @param hour2  结束时间
	 * @param minute 每个多少分钟
	 */
	public void setTakepictureTask2(int hour1,int hour2,int minute) {
		startTakepictureTiem(Utils.DEIVCE_ALARM_START_2_ACTION, hour1,hour2, minute,sender2,2);
		closeTakepictureTiem(Utils.DEIVCE_ALARM_CANCEL_2_ACTION, hour2,2);
	}
	/**
	 * 第三个定时抓拍任务
	 * @param context
	 * @param hour1  开始时间
	 * @param hour2  结束时间
	 * @param minute 每个多少分钟
	 */
	public void setTakepictureTask3(int hour1,int hour2,int minute) {
		startTakepictureTiem(Utils.DEIVCE_ALARM_START_3_ACTION, hour1,hour2, minute,sender3,3);
		closeTakepictureTiem(Utils.DEIVCE_ALARM_CANCEL_3_ACTION, hour2,3);
	}
	/**
	 * 第四个定时抓拍任务
	 * @param context
	 * @param hour1  开始时间
	 * @param hour2  结束时间
	 * @param minute 每个多少分钟
	 */
	public void setTakepictureTask4(int hour1,int hour2,int minute) {
		startTakepictureTiem(Utils.DEIVCE_ALARM_START_4_ACTION, hour1,hour2, minute,sender4,4);
		closeTakepictureTiem(Utils.DEIVCE_ALARM_CANCEL_4_ACTION, hour2,4);
	}
	/**
	 * 第五个定时抓拍任务
	 * @param context
	 * @param hour1  开始时间
	 * @param hour2  结束时间
	 * @param minute 每个多少分钟
	 */
	public void setTakepictureTask5(int hour1,int hour2,int minute) {
		startTakepictureTiem(Utils.DEIVCE_ALARM_START_5_ACTION, hour1,hour2, minute,sender5,5);
		closeTakepictureTiem(Utils.DEIVCE_ALARM_CANCEL_5_ACTION, hour2,5);
	}
	/**
	 * 第六个定时抓拍任务
	 * @param context
	 * @param hour1  开始时间
	 * @param hour2  结束时间
	 * @param minute 每个多少分钟
	 */
	public void setTakepictureTask6(int hour1,int hour2,int minute) {
		startTakepictureTiem(Utils.DEIVCE_ALARM_START_6_ACTION, hour1,hour2, minute,sender6,6);
		closeTakepictureTiem(Utils.DEIVCE_ALARM_CANCEL_6_ACTION, hour2,6);
	}
	
	/**
	 * 开始抓拍任务
	 * @param context
	 * @param action 广播接收器
	 * @param hour 起始时间  
	 * @param minute 每个多少分钟
	 */
	public void startTakepictureTiem(String action,int hour,int hour2,int minute,PendingIntent sender1,int requestCode) {
		Intent intent = new Intent(mContext, AlarmReceiver.class);
		System.out.println("---------------------guo1---------------------startMin="+startMin);
		intent.setAction(action);
		PendingIntent sender = PendingIntent.getBroadcast(mContext,requestCode, intent,PendingIntent.FLAG_CANCEL_CURRENT);
		Calendar calendar =Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis()); 
		//+ by hcj @}
		if(ConfigUtil.DBG_TIMER){
			Log.i(ConfigUtil.TAG_TIMER,"startTakepictureTiem,currHour="+calendar.get(Calendar.HOUR)+",alarmHour="+hour+",alarmMin="+startMin);
		}
		//+ by hcj @}
		calendar.set(Calendar.HOUR_OF_DAY, hour);        //是24小时制，第二参数几小时以后 
        calendar.set(Calendar.MINUTE, startMin);            //设置闹钟的分钟数
       // Random random = new Random();
       // calendar.set(Calendar.SECOND, 0);                //设置闹钟的秒数
        calendar.set(Calendar.SECOND, BootCameraService.uploadSleep);
        calendar.set(Calendar.MILLISECOND, 0);  
        System.out.println("guo startTakepictureTiem=");
        int timeflag=BootBroadcastReceiver.getcurrHour();
        if(timeflag>hour){
        	if(hour2==0){
        		hour2=24;
        	}
        	if(timeflag<hour2){
        	}else{
        		System.out.println("guosong data is not chang");
        		calendar.add(Calendar.DATE, 1);
        	}
        }
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
		LogManager.getInstance().Log("startTakepictureTiem at:"+LogManager.mSdf.format(calendar.getTime())+",action="+action+",requestCode="+requestCode);
	}
	
	public  void setAlarmTime(String action,int timeInMillis, int requestCode) {
		System.out.println("guo -----------------------setAlarmTime");
	    Intent intent = new Intent(action);
	    PendingIntent sender = PendingIntent.getBroadcast(mContext, requestCode, intent,PendingIntent.FLAG_CANCEL_CURRENT);
	    alarmManager.cancel(sender);
	    Calendar calendar =Calendar.getInstance();
	    //calendar.add(calendar.MINUTE, timeInMillis);
	    if(action.equals(Utils.DURING_DAY_MODEL)){
		//+ by hcj @{	
		if(ConfigUtil.DBG_ISO){
			timeInMillis = 2;
		}
		//+ by hcj @}
	    	if(albarmTimers == 10){
	    		calendar.add(calendar.MINUTE, timeInMillis+1);
	    	} else {
	    		calendar.add(calendar.MINUTE, timeInMillis);
	    	}
	    } else {
	    	albarmTimers = timeInMillis;
	    	calendar.add(calendar.MINUTE, timeInMillis);
	    }
	    //calendar.clear(calendar.SECOND);
	    //Random random = new Random();
        calendar.set(Calendar.SECOND, BootCameraService.uploadSleep);   
	    calendar.clear(calendar.MILLISECOND);
	    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender); 
		LogManager.getInstance().Log("setAlarmTime at:"+LogManager.mSdf.format(calendar.getTime())+",action="+action+",requestCode="+requestCode);
	}

	/**
	 * 结束抓拍任务
	 * @param context 
	 * @param action 广播接收器
	 * @param hour 结束时间
	 */
	public void closeTakepictureTiem(String action,int hour,int requestCode) {
		/*Intent intent = new Intent(mContext, AlarmReceiver.class);
		PendingIntent sender=null;
		//if(BootBroadcastReceiver.getcurrYearfalg()){
		Calendar calendar =Calendar.getInstance(Locale.getDefault());
		calendar.setTimeInMillis(System.currentTimeMillis()); 
		calendar.set(Calendar.HOUR_OF_DAY, hour);        //是24小时制，第二参数几小时以后 
        calendar.set(Calendar.MINUTE, 0);            //设置闹钟的分钟数
        calendar.set(Calendar.SECOND, 0);                //设置闹钟的秒数
        calendar.set(Calendar.MILLISECOND, 0); 
        boolean curflag=calendar.before(Calendar.getInstance(Locale.getDefault()));
        System.out.println("guo closeTakepictureTiem="+curflag);
        if(BootBroadcastReceiver.getcurrHour()>hour && hour>0){
        	System.out.println("guosong===========cose");
    		calendar.add(Calendar.DATE, 1);
        }
        //设置闹钟的毫秒数
        long firstTime = SystemClock.elapsedRealtime(); // 开机之后到现在的运行时间(包括睡眠时间)  
        long selectTime = calendar.getTimeInMillis();//选择的定时时间
        long systemTime = System.currentTimeMillis();  
        // 计算现在时间到设定时间的时间差  
        long time = selectTime - systemTime;  
        long firstTime1=firstTime + time;
        
		intent.setAction(action);
		sender = PendingIntent.getBroadcast(mContext,requestCode, intent,PendingIntent.FLAG_CANCEL_CURRENT);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime1, sender); */
		/*}else{
			intent.setAction(Utils.DEIVCE_ALARM_TIME_ERROR_ACTION);
			intent.putExtra("action", action);
			intent.putExtra("hour", hour);
			intent.putExtra("requestCode", requestCode);
			sender = PendingIntent.getBroadcast(mContext,requestCode, intent,PendingIntent.FLAG_CANCEL_CURRENT);
			alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 2*60*1000, sender);
		}*/
	}
	public void getTimeFlag(){
		System.out.println("guosong settime =====start");
		Intent intent = new Intent(mContext, AlarmReceiver.class);
		PendingIntent sender=null;
		intent.setAction(Utils.DEIVCE_ALARM_TIME_ERROR_ACTION);
		sender = PendingIntent.getBroadcast(mContext,8, intent,PendingIntent.FLAG_CANCEL_CURRENT);
		alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, 2*60*1000, sender);
	}
	/*
	public void getTimeflagDay(){
		System.out.println("guosong DEIVCE_ALARM_TIME_FLAG_DAY_ACTION =====start");
		Intent intent = new Intent(mContext, AlarmReceiver.class);
		PendingIntent sender=null;
		intent.setAction(Utils.DURING_DAY_MODEL);
		sender = PendingIntent.getBroadcast(mContext,9, intent,PendingIntent.FLAG_CANCEL_CURRENT);
		alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, 60*1000, sender);
	}
	*/
	public  void closeTakepictureTiem(PendingIntent sender) {
		alarmManager.cancel(sender);
	}
	
}
