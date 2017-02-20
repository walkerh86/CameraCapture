package com.camera.setting.receiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.camera.setting.servics.BootCameraService;
import com.camera.setting.utils.TakepictureTiem;
import com.camera.setting.utils.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import com.cj.ConfigUtil;//+ by hcj
import com.cj.LogManager;
import com.cj.VarCommon;

public class AlarmReceiver extends BroadcastReceiver {
	private static final String TAG = AlarmReceiver.class.getSimpleName();
	private AlarmManager alarm;
    public Context mContext;
    public TakepictureTiem t;
	public boolean getflagTime(String time2){
		
		boolean flag=false;
		int hour= BootBroadcastReceiver.getcurrHour();
		if(hour==0){
			hour=24;
		}
		int time=Integer.parseInt(time2);
		if(time==0){
			time=24;
		}
		if(hour==time){
			flag=true;
		}
		return flag;
	}
	public static boolean compareToTime(int hour1,int hour2){
		if(BootBroadcastReceiver.getcurrMir()){
			return false;
		}
		int hour= BootBroadcastReceiver.getcurrHour();
		if(hour1==0){
			if(hour==0&& hour2>0){
				return true;
			}
			//hour1=24;
		}
		if(hour2==0){
			hour2=24;
		}
		if(hour==0){
			hour=24;
		}
		if(hour< hour2 && (hour >= hour1)){
			System.out.println("guosong time is in hour1 and hour2");
			return true;
		}else if(BootBroadcastReceiver.getcurrMin()==0 && hour==hour2){
			System.out.println("guosong time is in hour = hour2");
			return true;
		}else{
			System.out.println("guosong time is not in hour1 and hour2");
			return  false;
		}
	}
	String [] arr;
	int type=0;
	public void getFlagWork(){
		if(compareToTimeArr(mContext)){
			System.out.println("guo----------getFlagWork1");
			takepictureSendBroadcast(this.mContext,"getFlagWork");//m by hcj
		}else{
			System.out.println("guo----------getFlagWork2");
			TakepictureTiem.startMin=BootBroadcastReceiver.getcurrMin();
			//+ by hcj @{
			if(ConfigUtil.DBG_TIMER){
				//TakepictureTiem.startMin=0;
			}
			//+ by hcj @}
			BootBroadcastReceiver.setTiemTriggerModel(mContext);
		}
	}
	public Boolean compareToTimeArr(Context context){
		//JSONObject jb=new JSONObject();
		boolean jb=false;
		String [] arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM1));
		jb=get(arr,1);
		arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM2));
		jb=get(arr,2);
		arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM3));
		jb=get(arr,3);
		arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM4));
		jb=get(arr,4);
		arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM5));
		jb=get(arr,5);
		arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM6));
		jb=get(arr,6);
		return jb;
	}
	public Boolean get(String [] arr,int type){
		try{
			if(compareToTime(Integer.parseInt(arr[0]),Integer.parseInt(arr[1]))){
				if(BootBroadcastReceiver.getcurrHour()==Integer.parseInt(arr[0])){
					System.out.println("guo---------get(String [] arr,int type)");
					//jb.put("flag", true);
					//takepictureSendBroadcast(this.mContext);
					//t.startTakepictureTiem(getAlarmAction(type), Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1,type);
					t.setAlarmTime(getAlarmAction(type),Integer.parseInt(arr[2]),type);
					return true;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	public String getAlarmAction(int type){
		String action=null;
		switch (type) {
		case 1:
			action=Utils.DEIVCE_ALARM_START_1_ACTION;
			break;
		case 2:
			action=Utils.DEIVCE_ALARM_START_2_ACTION;
			break;
		case 3:
			action=Utils.DEIVCE_ALARM_START_3_ACTION;
			break;
		case 4:
			action=Utils.DEIVCE_ALARM_START_4_ACTION;
			break;
		case 5:
			action=Utils.DEIVCE_ALARM_START_5_ACTION;
			break;
		case 6:
			action=Utils.DEIVCE_ALARM_START_6_ACTION;
			break;
		default:
			break;
		}
		return action;
	}
	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.i(TAG, "--aciton:" + intent.getAction());
		LogManager.getInstance().Log("AlarmReciver onReceive aciton="+ intent.getAction());
		this.mContext=context;
		TakepictureTiem.startMin=0;
		alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		t=new TakepictureTiem(context);
		String triggerModel = Utils.getProperty(context,Utils.KEY_FTP_TRIGGERMODEL);
		if("3".equals(triggerModel)){
		if (intent.getAction().equals(Utils.DEIVCE_ALARM_START_1_ACTION)) {
			System.out.println("AlarmReciver="+Utils.DEIVCE_ALARM_START_1_ACTION);
			arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM1));
			if(!compareToTime(Integer.parseInt(arr[0]),Integer.parseInt(arr[1]))){
				getFlagWork();
				cancelTime(context,intent.getAction(),1);
				t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_1_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 1);
			}else{
				int minute=BootBroadcastReceiver.getcurrMin();
				if(minute==0 && BootBroadcastReceiver.getcurrHour()==Integer.parseInt(arr[1]))
				{	
					arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM2));
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_2_ACTION,Integer.parseInt(arr[2]),2);
					//t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_2_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 2);
				}else{
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_1_ACTION,Integer.parseInt(arr[2]),1);
				}
				takepictureSendBroadcast(context,Utils.DEIVCE_ALARM_START_1_ACTION);//m by hcj
			}
		}else if (intent.getAction().equals(Utils.DEIVCE_ALARM_START_2_ACTION)) {
			System.out.println("AlarmReciver="+Utils.DEIVCE_ALARM_START_2_ACTION);
			arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM2));
			if(!compareToTime(Integer.parseInt(arr[0]),Integer.parseInt(arr[1]))){
				getFlagWork();
				cancelTime(context,intent.getAction(),2);
				t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_2_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 2);
			}else{
				int minute=BootBroadcastReceiver.getcurrMin();
				if(minute==0 && BootBroadcastReceiver.getcurrHour()==Integer.parseInt(arr[1]))
				{	
					arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM3));
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_3_ACTION,Integer.parseInt(arr[2]),3);
				}else{
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_2_ACTION,Integer.parseInt(arr[2]),2);
				}
					takepictureSendBroadcast(context,Utils.DEIVCE_ALARM_START_2_ACTION);//m by hcj
			}
		} else if (intent.getAction().equals(Utils.DEIVCE_ALARM_START_3_ACTION)) {
			System.out.println("AlarmReciver="+Utils.DEIVCE_ALARM_START_3_ACTION);
			arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM3));
			if(!compareToTime(Integer.parseInt(arr[0]),Integer.parseInt(arr[1]))){
				getFlagWork();
				cancelTime(context,intent.getAction(),3);
				t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_3_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 3);
			}else{
				int minute=BootBroadcastReceiver.getcurrMin();
				if(minute==0 && BootBroadcastReceiver.getcurrHour()==Integer.parseInt(arr[1]))
				{	
					arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM4));
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_4_ACTION,Integer.parseInt(arr[2]),4);
					//t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_4_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 4);
				}else{
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_3_ACTION,Integer.parseInt(arr[2]),3);
				}
					takepictureSendBroadcast(context,Utils.DEIVCE_ALARM_START_3_ACTION);//m by hcj
			}
			
		}else if (intent.getAction().equals(Utils.DEIVCE_ALARM_START_4_ACTION)) {
			System.out.println("AlarmReciver="+Utils.DEIVCE_ALARM_START_4_ACTION);
			arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM4));
			if(!compareToTime(Integer.parseInt(arr[0]),Integer.parseInt(arr[1]))){
				getFlagWork();
				cancelTime(context,intent.getAction(),4);
				t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_4_ACTION, Integer.parseInt(arr[0]), Integer.parseInt(arr[1]),Integer.parseInt(arr[2]), t.sender1, 4);
			}else{
				int minute=BootBroadcastReceiver.getcurrMin();
				if(minute==0 && BootBroadcastReceiver.getcurrHour()==Integer.parseInt(arr[1]))
				{	
					//next time period
					arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM5));
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_5_ACTION,Integer.parseInt(arr[2]),5);
					//t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_5_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 5);
				}else{
				t.setAlarmTime(Utils.DEIVCE_ALARM_START_4_ACTION,Integer.parseInt(arr[2]),4);
				}
					takepictureSendBroadcast(context,Utils.DEIVCE_ALARM_START_4_ACTION);//m by hcj
			}
			
		}else if (intent.getAction().equals(Utils.DEIVCE_ALARM_START_5_ACTION)) {
			System.out.println("AlarmReciver="+Utils.DEIVCE_ALARM_START_5_ACTION);
			arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM5));
			if(!compareToTime(Integer.parseInt(arr[0]),Integer.parseInt(arr[1]))){
				getFlagWork();
				cancelTime(context,intent.getAction(),5);
				t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_5_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 5);
			}else{
				int minute=BootBroadcastReceiver.getcurrMin();
				if(minute==0 && BootBroadcastReceiver.getcurrHour()==Integer.parseInt(arr[1]))
				{	
					arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM6));
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_6_ACTION,Integer.parseInt(arr[2]),6);
					//t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_6_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 6);
				}else{
				t.setAlarmTime(Utils.DEIVCE_ALARM_START_5_ACTION,Integer.parseInt(arr[2]),5);
				}
					takepictureSendBroadcast(context,Utils.DEIVCE_ALARM_START_5_ACTION);//m by hcj
			}
			
		}else if (intent.getAction().equals(Utils.DEIVCE_ALARM_START_6_ACTION)) {
			System.out.println("AlarmReciver="+Utils.DEIVCE_ALARM_START_6_ACTION);
			arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM6));
			if(!compareToTime(Integer.parseInt(arr[0]),Integer.parseInt(arr[1]))){
				getFlagWork();
				cancelTime(context,intent.getAction(),6);
				t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_6_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 6);
			}else{
				int minute=BootBroadcastReceiver.getcurrMin();
				if(minute==0 && BootBroadcastReceiver.getcurrHour()==Integer.parseInt(arr[1]))
				{	
					arr=Utils.getTiem(Utils.getProperty(context, Utils.KEY_FTP_TIEM1));
					//t.startTakepictureTiem(Utils.DEIVCE_ALARM_START_1_ACTION, Integer.parseInt(arr[0]),Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), t.sender1, 1);
					t.setAlarmTime(Utils.DEIVCE_ALARM_START_1_ACTION,Integer.parseInt(arr[2]),1);
				}else{
				    t.setAlarmTime(Utils.DEIVCE_ALARM_START_6_ACTION,Integer.parseInt(arr[2]),6);
				}
				int type=6;
					takepictureSendBroadcast(context,Utils.DEIVCE_ALARM_START_6_ACTION);//m by hcj
			}
			
		}
		}
		if(intent.getAction().equals(Utils.DEIVCE_RBOOT_ACTION)){
			Log.i(TAG, "-- action reboot device...");
			long firstTime = SystemClock.elapsedRealtime(); // 开机之后到现在的运行时�?包括睡眠时间)
			if(firstTime/(60*60*1000) > 0){
				PowerManager manager=(PowerManager) context.getSystemService(Context.POWER_SERVICE); 
				manager.reboot("reboot");
			}
/*- by hcj
		}else if(intent.getAction().equals(Utils.DURING_DAY_MODEL)){
			System.out.println("AlarmReciver="+Utils.DURING_DAY_MODEL);
			if(VarCommon.getInstance().isCameraFullSleep()){
				Log.i(ConfigUtil.TAG_DAYNIGHT,"skip reset DURING_DAY_MODEL alarm by VarCommon.getInstance().isCameraFullSleep()");
				return;
			}
			t.setAlarmTime(Utils.DURING_DAY_MODEL,10,9);
			//BootCameraService.flagDay(context);
			Intent i=new Intent();
			i.setPackage("com.camera.setting");
			i.setAction("com.gs.getIso");
			context.sendBroadcast(i);
*/
		}else if(Utils.DEIVCE_ALARM_TIME_ERROR_ACTION.equals(intent.getAction())){
			System.out.println("time= is ok,");
			if(BootBroadcastReceiver.getcurrYearfalg()){
				BootBroadcastReceiver b=new BootBroadcastReceiver();
				b.setTiemTriggerModel(context);
			}else{ //如两分钟超时，可能后续时间为正确，需继续等待
				System.out.println("guo takepictureTiem.getTimeFlag();");
				TakepictureTiem takepictureTiem = new TakepictureTiem(context);
				takepictureTiem.getTimeFlag();
			}
		}else if(Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
				||Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())){
				Log.i(ConfigUtil.TAG,"time or zone changed");
			//时间变化需要重新计�?
			if(triggerModel!=null && triggerModel.trim().equals("3")){
				if(BootBroadcastReceiver.getcurrYearfalg()){
					System.out.println("guo setTiemTriggerModel(context)");
					BootBroadcastReceiver.setTiemTriggerModel(context);
				}
			}
		}
		
	}
	public static void cancelTime(Context context,String action,int requestCode){
		Intent intentAlarm = new Intent(context, AlarmReceiver.class);
		intentAlarm.setAction(action);
		PendingIntent sender = PendingIntent.getBroadcast(context, requestCode, intentAlarm, 0);
		AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(sender);
		LogManager.getInstance().Log("cancelTime action="+action+",requestCode="+requestCode);
	}
	public static String readTxtFile(String filePath) {
		String lineTxt1 = null;
		try {
			String encoding = "GBK";
			File file = new File(filePath);
			if (null != file && file.isFile() && file.exists()) { // 判断文件是否存在
				InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);// 考虑到编码格�?
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineTxt = null;
				while ((lineTxt = bufferedReader.readLine()) != null) {
					lineTxt1 += lineTxt;
				}
				read.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lineTxt1;
     
    }
     
	
	private void takepictureSendBroadcast(Context context,String takeAction) {
		System.err.println("guo time takepictureSendBroadcast ");
		Intent intent = new Intent();
		String takingModel = Utils.getProperty(context,Utils.KEY_FTP_TAKINGMODEL);
		String fileSize = Utils.getProperty(context,Utils.KEY_FTP_FILESIZE);
		intent.setAction(Utils.CAMERA_TAKEPICTURE_ACTION);
		intent.putExtra("takepic_action",takeAction);//+ by hcj
		intent.setPackage("com.camera.setting");
		intent.putExtra(Utils.INTENT_TAKINGMODEL, Integer.parseInt(takingModel));
		intent.putExtra(Utils.INTENT_FILESIZE,  Integer.parseInt(fileSize));
		context.sendBroadcast(intent);
		if(Utils.registered == true)
		   Log.i(TAG, "sendBroadcast registered CAMERA_TAKEPICTURE_ACTION");
		else 
		   Log.i(TAG, "sendBroadcast not registered CAMERA_TAKEPICTURE_ACTION");
	}

}
