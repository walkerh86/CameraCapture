package com.camera.setting.receiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.camera.setting.ftp.FTP;
import com.camera.setting.ftp.FTP.DownLoadProgressListener;
import com.camera.setting.servics.BootCameraService;
import com.camera.setting.servics.UploadService;
import com.camera.setting.utils.CameraStateItem;
import com.camera.setting.utils.SmsWriteOpUtil;
import com.camera.setting.utils.TakepictureTiem;
import com.camera.setting.utils.Utils;
import com.camera.setting.utils.XmlPull;
//import com.camera.setting.servics.utils.SerialPort;
//+ by hcj @{
import com.cj.NetworkUtils;
import com.cj.ConfigUtil;
import com.cj.MiscUtils;
import com.cj.StateRetManager;
import com.cj.VarCommon;
import com.camera.setting.ftp.FTP.UploadProgressListener;
//+ by hcj @}

/**
 * @author dahai.zhou
 * @data 2016年5月27日 12:37:00
 * @version V1.0
 */
public class BootBroadcastReceiver extends BroadcastReceiver{
	private static final String TAG = BootBroadcastReceiver.class.getSimpleName();
	private Context mContext;
	public static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	public static final String SAVE_PATH = "//sdcard/Download/";
	private static final int BOOT_COMPLETED = 10001;
	private static final int END_CALL = 10002;
	private static final int SET_TAKING_MODEL = 10003;
	private static final int DOWNLOAD_FILE = 10005;
	private static final int getkey = 10006;
	private TelephonyManager telephonyManager ;
    private static long lastClickTime;
    private static final int KET_TEST_TRIGGER = 67;//电平测试
    private static final int KET_LE_TRIGGER = 87;//电平正常
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	public boolean initflag=true;

//	private SerialPort sp;

	@SuppressLint("SimpleDateFormat") 
	@Override
	public void onReceive(final Context context, Intent intent) {
		this.mContext = context;
		String action = intent.getAction();
		Log.i(TAG, "--action:" + action);
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)){
			//MainActivity.TEST_REBOOT  测试开机
			//Intent.ACTION_BOOT_COMPLETED  真实开机
			/*PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			WakeLock  wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Gank");
			     wl.acquire();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			/*- by hcj
			telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			boolean enabled = telephonyManager.getDataEnabled();
			Log.i(TAG, "--data enabled = "+enabled);
			if(!enabled){
				Log.i(TAG, "--open data enabled ");
				telephonyManager.setDataEnabled(true);
			}
			*/
			NetworkUtils.setNetworkOn(context);//+ by hcj
			File file = new File(Utils.XML_PATH);
			if(file.exists()){
				handler.sendEmptyMessage(BOOT_COMPLETED);
			}
			PackageInfo packInfo = null;
			try {
				packInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String version = String.valueOf(packInfo.versionCode);
			System.out.println("guosong BOOT_COMPLETED version="+version);
		 }else if (action.equals(ACTION_SMS_RECEIVED)) {
				SmsMessage msg = null;
				Bundle bundle = intent.getExtras();
				if (bundle != null) {
					Object[] pdusObj = (Object[]) bundle.get("pdus");
					for (Object p : pdusObj) {
						msg = SmsMessage.createFromPdu((byte[]) p);
						String msgTxt = msg.getMessageBody();// 接收消息的内容
						Date date = new Date(msg.getTimestampMillis());// 时间
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String receiveTime = format.format(date);//接收时间
						String senderNumber = msg.getOriginatingAddress();//发送人
						Log.i(TAG, "--发送人：" + senderNumber + "  短信内容：" + msgTxt+ "接收时间：" + receiveTime);
						if("dt2016-reboot-dt2016".equalsIgnoreCase(msgTxt)){
							deleteSMS(context,msgTxt);
							this.abortBroadcast();
							PowerManager manager=(PowerManager) context.getSystemService(Context.POWER_SERVICE); 
							manager.reboot("reboot");
						}else if("dt2016-camera-dt2016".equalsIgnoreCase(msgTxt)){
							sendBroadcast(ACTION_SMS_RECEIVED);
					//+ by hcj @{		
						}else if("dt2016-delete-dt2016".equalsIgnoreCase(msgTxt)){
							handler.post(new SmsActionRunnable(SMS_ACTION_CLEAR_PIC));
						}else if("dt2016-reset-dt2016".equalsIgnoreCase(msgTxt)){
							//handler.post(new SmsActionRunnable(SMS_ACTION_FACTORY_RST));
					//+ by hcj @{	
						}else{
							try{
								String [] arrText=msgTxt.split("-");
								if(arrText.length==7){
									Utils.saveProperty(context,Utils.KEY_FTP_USERNAME, arrText[4]);
									Utils.saveProperty(context,Utils.KEY_FTP_URL,arrText[2]);
									Utils.saveProperty(context,Utils.KEY_FTP_PASSWORD, arrText[5]);
									String hostName=Utils.getProperty(context, Utils.KEY_FTP_URL);
									String userName=Utils.getProperty(context, Utils.KEY_FTP_USERNAME);
									String password=Utils.getProperty(context, Utils.KEY_FTP_PASSWORD);
									System.out.println("hostName"+hostName+"userName"+userName+"password"+password);
									startUpdataxml(arrText[2],arrText[4],arrText[5]);
									Intent intent1 = new Intent();  
						        	intent1.setAction("com.gs.updateftpXml");
						        	context.sendBroadcast(intent1);
								}
							}catch(Exception e){
								e.printStackTrace();
							}
						}
						deleteSMS(context,msgTxt);
						this.abortBroadcast();
					}
					return;
				}
			}else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
				//TelephonyManager.ACTION_PHONE_STATE_CHANGED  //真实来电
				//MainActivity.TEST_PHONE 测试来电
				// 如果是来电
				TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
				//来电号码
				String incoming_number = intent.getStringExtra("incoming_number"); 
				switch (tm.getCallState()) {
				case TelephonyManager.CALL_STATE_RINGING: // 来电
					Log.i(TAG, "--来电号码 :"+ incoming_number); 
					if(null != incoming_number){
						handler.sendEmptyMessageDelayed(END_CALL, 1500);
					}
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK: // 接起电话
					Log.i(TAG, "--接起电话 :"+ incoming_number);  
					break;
				case TelephonyManager.CALL_STATE_IDLE: // 挂电话
					Log.i(TAG, "--挂电话 :"+ incoming_number); 
					break;
				}
			}else if (action.equals("android.camera.intent.action.TRIGGER_DATA")){
				/*int scanCode = intent.getIntExtra("KEY_SCAN_CODE", 1);
				Log.i(TAG, "--scanCode:"+scanCode);
				String triggerModel = Utils.getProperty(context,Utils.KEY_FTP_TRIGGERMODEL);
				if(triggerModel.equals("1") && scanCode == KET_LE_TRIGGER){//电平触发
					cameraTakepicture(scanCode);
				}
				if (scanCode == KET_TEST_TRIGGER) {//测试触发
					cameraTakepicture(scanCode);
				}*/
				
			}else if("android.intent.action.updatexml".equals(action)){
				try{
				//Thread.sleep(5000);//- by hcj
				Log.i(ConfigUtil.TAG,"start android.intent.action.updatexml");
				final String deivcesId = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
				executor.execute(new Runnable() {
					@Override
					public void run() {
						  try { 
								new FTP(mContext).downloadSingleFile(/*"/"+*/deivcesId+"/state.xml", SAVE_PATH,
										"state.xml", new DownLoadProgressListener() {
											@Override
											public void onDownLoadProgress(String currentStep,long downProcess, File file) {
												Log.i(ConfigUtil.TAG, "--updatexml" + currentStep);
												if (currentStep.equals(Utils.FTP_DOWN_SUCCESS)) {
													if(!readXml1(mContext,false,SAVE_PATH+ "state.xml")){
														Log.i(ConfigUtil.TAG,"ftp xml is wrong, override by local xml");
														handler.post(mUploadXmlRunnable);
													}else{
														Log.i(ConfigUtil.TAG,"ftp xml is new, override local xml");
														copyFile(SAVE_PATH+ "state.xml",Utils.XML_PATH);
													}
												}else if (currentStep.equals(Utils.FTP_FILE_NOTEXISTS)){
												/*
													Intent intent = new Intent();
													intent.setAction(Utils.CAMERA_UPLOAD_SERVICE_ACTION);
													intent.putExtra(Utils.INTENT_CAMERA_NAME_JPG, "state.xml");
													intent.putExtra(Utils.INTENT_CAMERA_PATH, Utils.XML_PATH);
													intent.putExtra(Utils.INTENT_UPLOAD_XML, true);
													intent.setClass(mContext, UploadService.class);
													mContext.startService(intent);
												*/
													Log.i(ConfigUtil.TAG,"ftp xml is missed, override by local xml");
													handler.post(mUploadXmlRunnable);
											} 
											}
										});

							StateRetManager retManager = StateRetManager.getInstance(mContext);
							String retFilePath = retManager.genRetFile();
							Log.i(ConfigUtil.TAG,"retFilePath="+retFilePath);
							if(retFilePath != null){
								VarCommon var = VarCommon.getInstance();
								var.uploadRequest(retFilePath,ConfigUtil.RET_STATE_REMOTE_DIR,new UploadProgressListener(){
									@Override
									public void onUploadProgress(String currentStep, long uploadSize, File file){
										if(currentStep.equals(Utils.FTP_UPLOAD_SUCCESS)){
											if(file != null)
											file.delete();
										}else if(currentStep.equals(Utils.FTP_UPLOAD_FAIL)){
											if(file != null)
											file.delete();
										}else if(currentStep.equals(Utils.FTP_CONNECT_FAIL)){
											if(file != null)
											file.delete();
										}
										Log.i(ConfigUtil.TAG,"upload ret file step="+currentStep);
									}
								});
							}
		                        } catch (Exception e) {  
		                              Log.i(ConfigUtil.TAG,"android.intent.action.updatexml e="+e);
		                     } 
					}
				});
				}catch(Exception e){
					e.printStackTrace();
				}
			}else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
	            String packageName = intent.getData().getSchemeSpecificPart();
	            System.out.println("guosong packageName="+packageName);
	            if("com.camera.setting".equals(packageName)){
	            	PowerManager manager=(PowerManager) context.getSystemService(Context.POWER_SERVICE); 
					manager.reboot("reboot");
	            }
	        }
		
		
	}
	 public void deleteSMS(Context context, String smscontent)
	 {
		 if (!SmsWriteOpUtil.isWriteEnabled(context)) {
	            SmsWriteOpUtil.setWriteEnabled(
	            		context, true);
		 }
	  try
	  {
	   // 准备系统短信收信箱的uri地址
	   Uri uri = Uri.parse("content://sms/inbox");// 收信箱
	   // 查询收信箱里所有的短信
	   Cursor isRead =
	     context.getContentResolver().query(uri, null, "read=" + 0,
	       null, null);
	   while (isRead.moveToNext())
	   {
	    // String phone =
	    // isRead.getString(isRead.getColumnIndex("address")).trim();//获取发信人
	    String body =
	      isRead.getString(isRead.getColumnIndex("body")).trim();// 获取信息内容
	    if (body.equals(smscontent))
	    {
	     int id = isRead.getInt(isRead.getColumnIndex("_id"));

	     context.getContentResolver().delete(
	       Uri.parse("content://sms"), "_id=" + id, null);
	    }
	   }
	  }
	  catch (Exception e)
	  {
	   e.printStackTrace();
	  }
	 }
	 
	 public void startUpdataxml(String ftpurl,String ftpUser,String pwd){
    	 SAXReader reader = new SAXReader();
    	    Document document;
    	    String filepath="//sdcard/state.xml";
    	    try {
    	    document = reader.read(new File(filepath));
    	    Element root = ((org.dom4j.Document) document).getRootElement();
    	    List<Element> worktimes = root.elements("ftpService");
    	    for (Element worktime : worktimes) {
    	    worktime.element("ftpUrl").setText(ftpurl);
    	    worktime.element("ftpUser").setText(ftpUser);
    	    worktime.element("ftpPassword").setText(pwd);
    	    }
    	    XMLWriter writer = new XMLWriter(new FileWriter(filepath));
    	    writer.write(document);
    	    writer.close();
    	    } catch (MalformedURLException e) {
    	    e.printStackTrace();
    	    } catch (DocumentException e) {
    	    e.printStackTrace();
    	    } catch (IOException e) {
    	    e.printStackTrace();
    	    }
    }
      /**
       * 开机初始化xml信息
       */
	private void bootInitXml(Context context) {
		String host = Utils.getProperty(context, Utils.KEY_FTP_URL);
		String userName=Utils.getProperty(context, Utils.KEY_FTP_USERNAME);
		String password=Utils.getProperty(context, Utils.KEY_FTP_PASSWORD);
		if (host != null && !host.equals("") && userName != null
				&& !userName.equals("") && password != null
				&& !password.equals("")) {
			startCamera(context);
		}
		//+ by hcj @{
		else if(ConfigUtil.DBG_NO_NET){
			startCamera(context);
		}else{
			Log.i(ConfigUtil.TAG,"bootInitXml skip startCamera!!");
		}
		//+ by hcj @}
		String triggerModel = Utils.getProperty(context,Utils.KEY_FTP_TRIGGERMODEL);
		System.out.println("guosong---triggerModel="+triggerModel);
		TakepictureTiem takepictureTiem = new TakepictureTiem(context);
		//takepictureTiem.getTimeflagDay();
		switch (Integer.parseInt(triggerModel)) {
		case 1:// 电平触发
			break;
		case 2://485触发
//			Utils.saveProperty(context, Utils.KEY_GPIO_OUT_3, "1");
//			IO.cmd(5); //3 (5(1) 6(0)) //开启485
			//打开485设备
//			try {
//				//设置485串口波特率 115200
////				sp = new SerialPort(new File("/dev/ttyMT1"), 115200, 0);
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			//读取485串口，判断串口触发数据： A5 ** ** ** FF  总计5个字节，中间三个字节是任意数据，需要记录在文件名里。
//			get485Info(sp);
			break;
		case 3://时间触发
			System.out.println("guoso=====时间触发");
			//setTiemTrigger(context);
			//try {
				//Thread.sleep(5*60*1000);
			if(getcurrYearfalg()){
					System.out.println("guo setTiemTriggerModel(context)");
					setTiemTriggerModel(context);
			}else{
				System.out.println("guo takepictureTiem.getTimeFlag();");
				takepictureTiem.getTimeFlag();
			}
			//} catch (Exception e) {
				//e.printStackTrace();
			//}
			break;
		default:
			break;
		}
		Log.i(TAG, "--Trigger model:" + triggerModel);
    	//rebootDevice();
    	//setDuringDayModel();
	}
	
	public void setTiemTrigger(final Context cont){
		new Thread(){
			public void run() {
				try {
					//Thread.sleep(3*60*1000);
					MiscUtils.threadSleep(3*60*1000);
					if(getcurrYearfalg()){
						System.out.println("guosong start 时间触发");
						setTiemTriggerModel(cont);
					}else{
						System.out.println("guosong start 等待时间校准");
						setTiemTrigger(cont);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			};
		}.start();
	}
	
//	private void get485Info(SerialPort sp1) {
//		FileInputStream inputStream = (FileInputStream) sp1.getInputStream();
//		int size;
//		try {
//			byte[] buffer = new byte[64];
//			if (inputStream == null){
//				return;
//			}
//			size = inputStream.read(buffer);
//			if (size > 0) {
//				for(int i=0 ;i <buffer.length; ++i){
//					int data = buffer[i]&0xFF;
//					Log.i(TAG, "--  buff[" + i + "]="  + Integer.toHexString(data));
//				}
//				//判断串口触发数据： A5 ** ** ** FF  总计5个字节，中间三个字节是任意数据，需要记录在文件名里。
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
//	}

      

	/**
	 * 解析XML
	 * @param context
	 * @param isInit 是否首次解析
	 * @param path
	 */
	private boolean readXml(Context context,boolean isInit,String path) {
		boolean xmlValid = true;
		File file = new File(path);
		Log.i(TAG, "-- xml:"+isInit);
		if (file != null) {
			InputStream stream = null;
			try {
				stream = new FileInputStream(file);
				CameraStateItem item = new XmlPull().readXml(stream);
//+ by hcj @{
				if(!item.isValid()){
					Log.i(ConfigUtil.TAG,"readXml checkStateItems fail path:"+path);
					return false;
				}
//+ by hcj @}								
				Utils.saveProperty(context,Utils.KEY_FTP_VERSION, item.getXmlVersion());
				//if(isInit){
					Utils.saveProperty(context,Utils.KEY_FTP_REBOOTIEM, item.getRebootTiem());
				//}
				String strTaking = item.getTakingModel().replaceAll(" ", "");
				int taking = Integer.parseInt(strTaking);
				if(taking > 3){
					Utils.saveProperty(context,Utils.KEY_FTP_TAKINGMODEL, "1");
				}else if(taking < 1){
					Utils.saveProperty(context,Utils.KEY_FTP_TAKINGMODEL, "1");
				}else{
					Utils.saveProperty(context,Utils.KEY_FTP_TAKINGMODEL, strTaking);
				}
				
				String strTrigger = item.getTriggerModel().replaceAll(" ", "");
				int trigger = Integer.parseInt(strTrigger);
				if(trigger > 3){
					Utils.saveProperty(context,Utils.KEY_FTP_TRIGGERMODEL, "1");
				}else if(trigger < 1){
					Utils.saveProperty(context,Utils.KEY_FTP_TRIGGERMODEL, "1");
				}else{
					Utils.saveProperty(context,Utils.KEY_FTP_TRIGGERMODEL, strTrigger);
				}
				Utils.saveProperty(context,Utils.KEY_FTP_USERNAME, item.getFtpUser());
				Utils.saveProperty(context,Utils.KEY_FTP_URL,item.getFtpUrl());
				Utils.saveProperty(context,Utils.KEY_FTP_PASSWORD, item.getFtpPassword());
				
				String strSize = item.getFileSize().replaceAll(" ", "");
				int size = Integer.parseInt(strSize);
				if(size > 12){
					Utils.saveProperty(context,Utils.KEY_FTP_FILESIZE, "4");
				}else if(size < 1){
					Utils.saveProperty(context,Utils.KEY_FTP_FILESIZE, "4");
				}else{
					Utils.saveProperty(context,Utils.KEY_FTP_FILESIZE, strSize);
				}
				Utils.saveProperty(context,Utils.KEY_FTP_IMG_TIEM, item.getImgTiem());
				Utils.saveProperty(context,Utils.KEY_FTP_IMG_BRIGHTNESS, item.getImgBrightness());
				Utils.saveProperty(context,Utils.KEY_FTP_IMG_WHITE_BALANCE, item.getImgWhiteValance());
				Utils.saveProperty(context,Utils.KEY_FTP_IMG_AUTO_LIGHTE, item.getAutoLight());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM1, item.getTiem1());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM2, item.getTiem2());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM3, item.getTiem3());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM4, item.getTiem4());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM5, item.getTiem5());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM6, item.getTiem6());
//				Log.i(TAG, "-- version:" + item.getXmlVersion());
//				Log.i(TAG, "-- RebooTiem:" + item.getRebootTiem());
//				Log.i(TAG, "-- takingModel:" + item.getTakingModel());
//				Log.i(TAG, "-- TriggerModel:" + item.getTriggerModel());
//				Log.i(TAG, "-- ftpUser:" + item.getFtpUser());
//				Log.i(TAG, "-- FtpUrl:" + item.getFtpUrl());
//				Log.i(TAG, "-- FtpPassword:" + item.getFtpPassword());
//				Log.i(TAG, "-- fileSize:" + item.getFileSize());
//				Log.i(TAG, "-- tiem1:" + item.getTiem1());
//				Log.i(TAG, "-- tiem2:" + item.getTiem2());
//				Log.i(TAG, "-- tiem3:" + item.getTiem3());
//				Log.i(TAG, "-- tiem4:" + item.getTiem4());
//				Log.i(TAG, "-- tiem5:" + item.getTiem5());
//				Log.i(TAG, "-- tiem6:" + item.getTiem6());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				xmlValid = false;
			}
//			Utils.saveProperty(context,Utils.KEY_GPIO_OUT_1, "0");
//			Utils.saveProperty(context,Utils.KEY_GPIO_OUT_2, "0");
//			Utils.saveProperty(context,Utils.KEY_GPIO_OUT_3, "0");
//			Utils.saveProperty(context,Utils.KEY_GPIO_OUT_4, "0");
			bootInitXml(context);

			notifyStateChanged();//+ by hcj
		}
		return xmlValid;//+ by hcj
	}
	private boolean readXml1(Context context,boolean isInit,String path) {
		//copyFile(path,Utils.XML_PATH);//- by hcj
		boolean xmlValid = true;
		File file = new File(path);
		Log.i(TAG, "-- xml:"+isInit);
		if (file != null) {
			InputStream stream = null;
			try {
				stream = new FileInputStream(file);
				CameraStateItem item = new XmlPull().readXml(stream);
//+ by hcj @{
				if(!item.isValid()){
					Log.i(ConfigUtil.TAG,"readXml1 checkStateItems fail!!!");
					return false;
				}
//+ by hcj @}								
				if(isInit){
					Utils.saveProperty(context,Utils.KEY_FTP_VERSION, item.getXmlVersion());
				}
				Utils.saveProperty(context,Utils.KEY_FTP_REBOOTIEM, item.getRebootTiem());
				
				String strTaking = item.getTakingModel().replaceAll(" ", "");
				int taking = Integer.parseInt(strTaking);
				if(taking > 3){
					Utils.saveProperty(context,Utils.KEY_FTP_TAKINGMODEL, "1");
				}else if(taking < 1){
					Utils.saveProperty(context,Utils.KEY_FTP_TAKINGMODEL, "1");
				}else{
					Utils.saveProperty(context,Utils.KEY_FTP_TAKINGMODEL, strTaking);
				}
				
				String strTrigger = item.getTriggerModel().replaceAll(" ", "");
				int trigger = Integer.parseInt(strTrigger);
				if(trigger > 3){
					Utils.saveProperty(context,Utils.KEY_FTP_TRIGGERMODEL, "1");
				}else if(trigger < 1){
					Utils.saveProperty(context,Utils.KEY_FTP_TRIGGERMODEL, "1");
				}else{
					Utils.saveProperty(context,Utils.KEY_FTP_TRIGGERMODEL, strTrigger);
				}
				String hostName=Utils.getProperty(context, Utils.KEY_FTP_URL);
				String userName=Utils.getProperty(context, Utils.KEY_FTP_USERNAME);
				String password=Utils.getProperty(context, Utils.KEY_FTP_PASSWORD);
				if(!hostName.equals(item.getFtpUrl())||!userName.equals(item.getFtpUser())||!password.equals(item.getFtpPassword())){
					Utils.saveProperty(context,Utils.KEY_FTP_USERNAME, item.getFtpUser());
					Utils.saveProperty(context,Utils.KEY_FTP_URL,item.getFtpUrl());
					Utils.saveProperty(context,Utils.KEY_FTP_PASSWORD, item.getFtpPassword());
					/*
					try {
						Thread.sleep(8000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
					//MiscUtils.threadSleep(8000);
					/*
					Intent intent1 = new Intent();  
		        	intent1.setAction("com.gs.updateftpXml");
		        	context.sendBroadcast(intent1);
		        	*/
		        		handler.postDelayed(mUploadXmlRunnable,8000);
					/*
		        	try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
					//MiscUtils.threadSleep(2000);
				}
				String strSize = item.getFileSize().replaceAll(" ", "");
				int size = Integer.parseInt(strSize);
				if(size > 12){
					Utils.saveProperty(context,Utils.KEY_FTP_FILESIZE, "4");
				}else if(size < 1){
					Utils.saveProperty(context,Utils.KEY_FTP_FILESIZE, "4");
				}else{
					Utils.saveProperty(context,Utils.KEY_FTP_FILESIZE, strSize);
				}
				Utils.saveProperty(context,Utils.KEY_FTP_IMG_TIEM, item.getImgTiem());
				Utils.saveProperty(context,Utils.KEY_FTP_IMG_BRIGHTNESS, item.getImgBrightness());
				Utils.saveProperty(context,Utils.KEY_FTP_IMG_WHITE_BALANCE, item.getImgWhiteValance());
				Utils.saveProperty(context,Utils.KEY_FTP_IMG_AUTO_LIGHTE, item.getAutoLight());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM1, item.getTiem1());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM2, item.getTiem2());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM3, item.getTiem3());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM4, item.getTiem4());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM5, item.getTiem5());
				Utils.saveProperty(context,Utils.KEY_FTP_TIEM6, item.getTiem6());
				String version=Utils.getProperty(context, Utils.KEY_FTP_VERSION);
				System.out.println(item.getXmlVersion()+"guosong ==xmlversion=="+version);
				if(!"".equals(version) && !version.equals(item.getXmlVersion())){
					System.out.println("guosong==enter reboot");
					Utils.saveProperty(context,Utils.KEY_FTP_VERSION, item.getXmlVersion());
					PowerManager manager=(PowerManager) context.getSystemService(Context.POWER_SERVICE); 
					manager.reboot("reboot");
				}
				notifyStateChanged();//+ by hcj
			} catch (FileNotFoundException e) {
				//e.printStackTrace();
				Log.i(ConfigUtil.TAG,"readXml1 e="+e);//+ by hcj
				xmlValid = false;
			}
		}

		return xmlValid;
	}
	public static void copyFile(String oldPath, String newPath) { 
        try { 
            int bytesum = 0; 
            int byteread = 0; 
            File oldfile = new File(oldPath); 
            if (oldfile.exists()) { //文件存在时 
                InputStream inStream = new FileInputStream(oldPath); //读入原文件 
                if(inStream.available()>=0){
	                FileOutputStream fs = new FileOutputStream(newPath); 
	                byte[] buffer = new byte[1444]; 
	                int length; 
	                while ( (byteread = inStream.read(buffer)) != -1) { 
	                    bytesum += byteread; //字节数 文件大小 
	                    System.out.println(bytesum); 
	                    fs.write(buffer, 0, byteread); 
	                } 
                }
                inStream.close(); 
                
            } 
        } 
        catch (Exception e) { 
            System.out.println("复制单个文件操作出错"); 
            e.printStackTrace(); 
        } 
    }
	public static int  getcurrHour(){
		Calendar calendar =Calendar.getInstance(Locale.getDefault());
		calendar.setTimeInMillis(System.currentTimeMillis()); 
		int currentHours = calendar.get(Calendar.HOUR_OF_DAY);
		return currentHours;
	}
	public static boolean  getcurrMir(){
		Calendar calendar =Calendar.getInstance(Locale.getDefault());
		calendar.setTimeInMillis(System.currentTimeMillis()); 
		int SECOND = calendar.get(Calendar.SECOND);
		if(getcurrMin()==0 && SECOND==0){
			return true;
		}
		return false;
	}
	 public static int  getcurrMin(){
			Calendar calendar =Calendar.getInstance(Locale.getDefault());
			calendar.setTimeInMillis(System.currentTimeMillis()); 
			int MINUTE = calendar.get(Calendar.MINUTE);
			return MINUTE;
		}
	public static boolean  getcurrYearfalg(){
		Calendar calendar =Calendar.getInstance(Locale.getDefault());
		calendar.setTimeInMillis(System.currentTimeMillis()); 
		int currentYear = calendar.get(Calendar.YEAR);
		if(currentYear<2016){
			return false;
		}
		return true;
	}
	
	public static void setTiemTriggerModel(Context context) {
		Calendar calendar =Calendar.getInstance(Locale.getDefault());
		calendar.setTimeInMillis(System.currentTimeMillis()); 
		int currentHours = calendar.get(Calendar.HOUR_OF_DAY);
		int currentMinutes = calendar.get(Calendar.MINUTE);
		Log.i(TAG,"guo--当前时间："+currentHours+"时"+currentMinutes+"分");
		String tiem1 = Utils.getProperty(context, Utils.KEY_FTP_TIEM1);
		String tiem2 = Utils.getProperty(context, Utils.KEY_FTP_TIEM2);
		String tiem3 = Utils.getProperty(context, Utils.KEY_FTP_TIEM3);
		String tiem4 = Utils.getProperty(context, Utils.KEY_FTP_TIEM4);
		String tiem5 = Utils.getProperty(context, Utils.KEY_FTP_TIEM5);
		String tiem6 = Utils.getProperty(context, Utils.KEY_FTP_TIEM6);
		if(ConfigUtil.DBG_TIMER){
			Log.i(ConfigUtil.TAG_TIMER,"setTiemTriggerModel,tiem1="+tiem1+",tiem2="+tiem2+",tiem3="+tiem3+",tiem4="+tiem4+",tiem5="+tiem5+",tiem6="+tiem6);
		}
		String[] tiem_1 = Utils.getTiem(tiem1);
		String[] tiem_2 = Utils.getTiem(tiem2);
		String[] tiem_3 = Utils.getTiem(tiem3);
		String[] tiem_4 = Utils.getTiem(tiem4);
		String[] tiem_5 = Utils.getTiem(tiem5);
		String[] tiem_6 = Utils.getTiem(tiem6);
		TakepictureTiem takepictureTiem = new TakepictureTiem(context);
		System.out.println("guo--gettimetime1="+tiem_1[0]+"tiem_1[0]"+tiem_1[1]+"isTiemLegal(tiem_1[0], tiem_1[1])"+isTiemLegal(tiem_1[0], tiem_1[1])+"currentHours < strToInt(currentHours,tiem_1[0])="+(currentHours < strToInt(currentHours,tiem_1[0])));
		//if(isTiemLegal(tiem_1[0], tiem_1[1])){
		if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_1[0]), Integer.parseInt(tiem_1[1]))){
			Log.i(TAG, "guo-- 第1个 时间间隔 :" + tiem1);
			takepictureTiem.setTakepictureTask1(Integer.parseInt(tiem_1[0]), Integer.parseInt(tiem_1[1]), Integer.parseInt(tiem_1[2]));	
			
		}else if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_2[0]), Integer.parseInt(tiem_2[1]))){
			Log.i(TAG, "guo-- 第2 个时间间隔 :" + tiem2);
			takepictureTiem.setTakepictureTask2(Integer.parseInt(tiem_2[0]), Integer.parseInt(tiem_2[1]), Integer.parseInt(tiem_2[2]));
			
		}else if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_3[0]), Integer.parseInt(tiem_3[1]))){
			Log.i(TAG, "guo-- 第3 个时间间隔 :" + tiem3);
			takepictureTiem.setTakepictureTask3(Integer.parseInt(tiem_3[0]), Integer.parseInt(tiem_3[1]), Integer.parseInt(tiem_3[2]));
			
		}else if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_4[0]), Integer.parseInt(tiem_4[1]))){
			Log.i(TAG, "guo-- 第4个 时间间隔 :" + tiem4);
			takepictureTiem.setTakepictureTask4(Integer.parseInt(tiem_4[0]), Integer.parseInt(tiem_4[1]), Integer.parseInt(tiem_4[2]));
			
		}else if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_5[0]), Integer.parseInt(tiem_5[1]))){
			Log.i(TAG, "guo-- 第5个 时间间隔 :" + tiem5);
			takepictureTiem.setTakepictureTask5(Integer.parseInt(tiem_5[0]), Integer.parseInt(tiem_5[1]), Integer.parseInt(tiem_5[2]));
			
		}else if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_6[0]), Integer.parseInt(tiem_6[1]))){
			Log.i(TAG, "guo-- 第6 个时间间隔 :" + tiem6);
			takepictureTiem.setTakepictureTask6(Integer.parseInt(tiem_6[0]), Integer.parseInt(tiem_6[1]), Integer.parseInt(tiem_6[2]));
		}
		//开始判断当当前时间不在上述时间范围内，则执行第一个时间比较接近的
		else if(Integer.parseInt(tiem_6[1])!=Integer.parseInt(tiem_1[0])){
			if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_6[1]), Integer.parseInt(tiem_1[0]))){
				Log.i(TAG, "guo2-- 第1个 时间间隔 :" + tiem1);
				takepictureTiem.setTakepictureTask1(Integer.parseInt(tiem_1[0]), Integer.parseInt(tiem_1[1]), Integer.parseInt(tiem_1[2]));	
			}
		}
		else if(Integer.parseInt(tiem_1[1])!=Integer.parseInt(tiem_2[0])){
			if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_1[1]), Integer.parseInt(tiem_2[0]))){
				Log.i(TAG, "guo2-- 第2个 时间间隔 :" + tiem2);
				takepictureTiem.setTakepictureTask2(Integer.parseInt(tiem_2[0]), Integer.parseInt(tiem_2[1]), Integer.parseInt(tiem_2[2]));	
			}
		}else if(Integer.parseInt(tiem_2[1])!=Integer.parseInt(tiem_3[0])){
			if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_2[1]), Integer.parseInt(tiem_3[0]))){
				Log.i(TAG, "guo2-- 第3个 时间间隔 :" + tiem3);
				takepictureTiem.setTakepictureTask3(Integer.parseInt(tiem_3[0]), Integer.parseInt(tiem_3[1]), Integer.parseInt(tiem_3[2]));	
			}
		}else if(Integer.parseInt(tiem_3[1])!=Integer.parseInt(tiem_4[0])){
			if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_3[1]), Integer.parseInt(tiem_4[0]))){
				Log.i(TAG, "guo2-- 第4个 时间间隔 :" + tiem4);
				takepictureTiem.setTakepictureTask4(Integer.parseInt(tiem_4[0]), Integer.parseInt(tiem_4[1]), Integer.parseInt(tiem_4[2]));	
			}
		}else if(Integer.parseInt(tiem_4[1])!=Integer.parseInt(tiem_5[0])){
			if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_4[1]), Integer.parseInt(tiem_5[0]))){
				Log.i(TAG, "guo2-- 第5个 时间间隔 :" + tiem5);
				takepictureTiem.setTakepictureTask5(Integer.parseInt(tiem_5[0]), Integer.parseInt(tiem_5[1]), Integer.parseInt(tiem_5[2]));	
			}
		}else if(Integer.parseInt(tiem_5[1])!=Integer.parseInt(tiem_6[0])){
			if(AlarmReceiver.compareToTime(Integer.parseInt(tiem_5[1]), Integer.parseInt(tiem_6[0]))){
				Log.i(TAG, "guo2-- 第6个 时间间隔 :" + tiem6);
				takepictureTiem.setTakepictureTask6(Integer.parseInt(tiem_6[0]), Integer.parseInt(tiem_6[1]), Integer.parseInt(tiem_6[2]));	
			}
		}else{
			Log.i(TAG, "guosong-- 设置时间无效，请核实。。。");
		}
	}
	
	
	private static Integer strToInt(int currentHours,String tiem){
		if(tiem == null){
			return currentHours;
		}else{
			return Integer.parseInt(tiem);
		}
	}
	
	private static boolean isTiemLegal(String tiem1,String tiem2) {
		if(tiem1 == null || tiem2 == null){
			return false;
		}else{
			int tiem_1 = Integer.parseInt(tiem1);
			int tiem_2 =Integer.parseInt(tiem2);
			if(tiem_1 >= 24  || tiem_1 < 0){
				return false;
			}else if(tiem_2 >= 24 || tiem_2 < 0){
				return false;
			}else if(tiem_2==0&&tiem_1<24){
				return true;
			}else{
				return tiem_1 <= tiem_2;
			}
		}
	}

	/**
	 * 定时关机任务
	 */
	private void rebootDevice() {
		String tiem = Utils.getProperty(mContext, Utils.KEY_FTP_REBOOTIEM);
		Log.i(TAG, "--reboot Device : " + tiem);
		String[] rebootTiem = Utils.getTiem(tiem);
		AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		Calendar calendar = Calendar.getInstance(Locale.getDefault());
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(rebootTiem[0])); // 是24小时制，第二参数几小时以后
		calendar.set(Calendar.MINUTE, Integer.parseInt(rebootTiem[1])); // 设置闹钟的分钟数
		calendar.set(Calendar.SECOND, 0); // 设置闹钟的秒数
		calendar.set(Calendar.MILLISECOND, 0); // 设置闹钟的毫秒数
		long firstTime = SystemClock.elapsedRealtime(); // 开机之后到现在的运行时间(包括睡眠时间)
		long selectTime = calendar.getTimeInMillis();// 选择的定时时间
		long systemTime = System.currentTimeMillis();
		// 计算现在时间到设定时间的时间差
		long time = selectTime - systemTime;
		firstTime += time;
		Intent intent = new Intent(mContext, AlarmReceiver.class);
		intent.setAction(Utils.DEIVCE_RBOOT_ACTION);
//		Intent reboot = new Intent(Intent.ACTION_REBOOT);   
//      reboot.putExtra("nowait", 1);   
//      reboot.putExtra("interval", 1);   
//      reboot.putExtra("window", 0);   
        Log.i(TAG, "--reboot Device HOUR_OF_DAY : " + rebootTiem[0]+" MINUTE :"+ rebootTiem[1]);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,firstTime, sender);
	}
	
	/**
	 * 白天/夜间模式任务
	 */
	private void setDuringDayModel() {
		AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(mContext, AlarmReceiver.class);
		intent.setAction(Utils.DURING_DAY_MODEL);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);
    	am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 15*60*1000, sender); 
	}
	
	/** 
     * 挂断电话 
     * @param context 
     */  
    public void endCall(Context context) {    
        try {    
            Object telephonyObject = getTelephonyObject(context);    
            if (null != telephonyObject) {    
                Class telephonyClass = telephonyObject.getClass();    
                Method endCallMethod = telephonyClass.getMethod("endCall");    
                endCallMethod.setAccessible(true);    
                endCallMethod.invoke(telephonyObject);    
            }    
        } catch (SecurityException e) {    
            e.printStackTrace();    
        } catch (NoSuchMethodException e) {    
            e.printStackTrace();    
        } catch (IllegalArgumentException e) {    
            e.printStackTrace();    
        } catch (IllegalAccessException e) {    
            e.printStackTrace();    
        } catch (InvocationTargetException e) {    
            e.printStackTrace();    
        }    
    }   
    
    private Object getTelephonyObject(Context context) {    
        Object telephonyObject = null;    
        try {    
            // 初始化iTelephony    
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);    
            // Will be used to invoke hidden methods with reflection    
            // Get the current object implementing ITelephony interface    
            Class telManager = telephonyManager.getClass();    
            Method getITelephony = telManager.getDeclaredMethod("getITelephony");    
            getITelephony.setAccessible(true);    
            telephonyObject = getITelephony.invoke(telephonyManager);    
        } catch (SecurityException e) {    
            e.printStackTrace();    
        } catch (NoSuchMethodException e) {    
            e.printStackTrace();    
        } catch (IllegalArgumentException e) {    
            e.printStackTrace();    
        } catch (IllegalAccessException e) {    
            e.printStackTrace();    
        } catch (InvocationTargetException e) {    
            e.printStackTrace();    
        }    
        return telephonyObject;    
    }   
    
    @SuppressLint("HandlerLeak") 
  	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
  			switch (msg.what) {
  			case BOOT_COMPLETED:
  			     Intent intent = new Intent();
  			     if(Utils.getSysProperty(mContext, Utils.KEY_SYS_INIT)){
  				    Log.i(TAG, "--xml create...");
					intent.setAction(Utils.CAMERA_UPLOAD_SERVICE_ACTION);
					intent.putExtra(Utils.INTENT_CAMERA_NAME_JPG, "state.xml");
					intent.putExtra(Utils.INTENT_CAMERA_PATH, Utils.XML_PATH);
					intent.putExtra(Utils.INTENT_UPLOAD_XML, true);
					intent.setClass(mContext, UploadService.class);
					mContext.startService(intent);
					
					Utils.saveSysProperty(mContext, Utils.KEY_SYS_INIT, false);
					readXml(mContext,true,Utils.XML_PATH);
  		          }else{
			//+ by hcj @{		
				//if(ConfigUtil.DBG_NO_NET){
					//readXml(mContext,false,Utils.XML_PATH);
					//break;
				//}
			//+ by hcj @}	
  		        	Log.i(TAG, "--xml camera upload file service...");
  		        	//intent.setAction(Utils.CAMERA_UPLOAD_FILE_SERVICE_ACTION);//- by hcj
  		        	intent.setAction("com.cj.start");
  		        	intent.setClass(mContext, UploadService.class);
  				    mContext.startService(intent);
  		        	handler.sendEmptyMessageDelayed(DOWNLOAD_FILE, 1000);//- by hcj
  		        	//handler.sendEmptyMessage(DOWNLOAD_FILE);//+ by hcj
  		        	handler.removeMessages(BOOT_COMPLETED);
  		          }
  				break;
  			case DOWNLOAD_FILE:
  	  			final String deivcesId = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
  	  		    Log.i(ConfigUtil.TAG, "--DOWNLOAD_FILE...");
				executor.execute(new Runnable() {
					@Override
					public void run() {
						  Log.i(ConfigUtil.TAG, "--start DOWNLOAD_FILE...");
						  try { 
								new FTP(mContext).downloadSingleFile(/*"/"+*/deivcesId+"/state.xml", SAVE_PATH,
										"state.xml", new DownLoadProgressListener() {
											@Override
											public void onDownLoadProgress(String currentStep,long downProcess, File file) {
												Log.i(ConfigUtil.TAG, "--" + currentStep);
												if (currentStep.equals(Utils.FTP_DOWN_SUCCESS)) {
													if(!readXml(mContext,false,SAVE_PATH+ "state.xml")){
														Log.i(ConfigUtil.TAG,"ftp xml is wrong, override by local xml");
														readXml(mContext,false,Utils.XML_PATH);
														handler.post(mUploadXmlRunnable);
													}else{
														Log.i(ConfigUtil.TAG,"ftp xml is new, override local xml");
														copyFile(SAVE_PATH+ "state.xml",Utils.XML_PATH);
													}
													Log.i(TAG, "readXml");
												}else if (currentStep.equals(Utils.FTP_FILE_NOTEXISTS)){
												/*
													Intent intent = new Intent();
													intent.setAction(Utils.CAMERA_UPLOAD_SERVICE_ACTION);
													intent.putExtra(Utils.INTENT_CAMERA_NAME_JPG, "state.xml");
													intent.putExtra(Utils.INTENT_CAMERA_PATH, Utils.XML_PATH);
													intent.putExtra(Utils.INTENT_UPLOAD_XML, true);
													intent.setClass(mContext, UploadService.class);
													mContext.startService(intent);
												*/
													Log.i(ConfigUtil.TAG,"ftp xml is missed, override by local xml");
													readXml(mContext,false,Utils.XML_PATH);
													handler.post(mUploadXmlRunnable);
													//mContext.sendBroadcast(intent);
													Log.i(TAG, "not readXml");
												} else if (currentStep.equals(Utils.FTP_DOWN_FAIL)
														|| currentStep.equals(Utils.FTP_CONNECT_FAIL)) {
													handler.sendEmptyMessageDelayed(DOWNLOAD_FILE, 1000);
													Log.i(ConfigUtil.TAG, "download ftp xml fail...");
												} 
											}
										});
		                        } catch (Exception e) {  
		                              //e.printStackTrace();
		                              Log.i(ConfigUtil.TAG,"boot DOWNLOAD_FILE e="+e);
		                     } 
					}
				});
				handler.removeMessages(DOWNLOAD_FILE);
  				break;
  			case END_CALL:
  				endCall(mContext);
				handler.sendEmptyMessageDelayed(SET_TAKING_MODEL, 2000);
  				break;
  			case SET_TAKING_MODEL:
  				Intent intent1 = new Intent();
  				String takingModel = Utils.getProperty(mContext,Utils.KEY_FTP_TAKINGMODEL);
  				String fileSize = Utils.getProperty(mContext,Utils.KEY_FTP_FILESIZE);
  				intent1.setAction(Utils.CAMERA_TAKEPICTURE_ACTION);
  				intent1.putExtra(Utils.INTENT_TAKINGMODEL, Integer.parseInt(takingModel));
  				intent1.putExtra(Utils.INTENT_FILESIZE,  Integer.parseInt(fileSize));
				intent1.putExtra("takepic_action","SET_TAKING_MODEL");//+ by hcj
  				mContext.sendBroadcast(intent1);
  				handler.removeMessages(END_CALL);
  				handler.removeMessages(SET_TAKING_MODEL);
  				break;
  			case getkey:
  				int scanCode=Integer.parseInt(msg.obj.toString());
  				//int scanCode = intent.getIntExtra("KEY_SCAN_CODE", 1);
				Log.i(TAG, "--scanCode:"+scanCode);
				String triggerModel = Utils.getProperty(mContext,Utils.KEY_FTP_TRIGGERMODEL);
				if(triggerModel.equals("1") && scanCode == KET_LE_TRIGGER){//电平触发
					cameraTakepicture(scanCode,"getkey KET_LE_TRIGGER");//m by hcj
				}
				else if (scanCode == KET_TEST_TRIGGER) {//测试触发
					cameraTakepicture(scanCode,"getkey KET_TEST_TRIGGER");//m by hcj
				}
  				break;
  			default:
  				break;
  			}
  		}
  	};
  	public void sendBroadcast(String takeAction){
  		Intent intent1 = new Intent();
		intent1.setAction(Utils.CAMERA_TAKEPICTURE_ACTION);
		intent1.putExtra("dx", true);
		intent1.putExtra("takepic_action",takeAction);//+ by hcj
		mContext.sendBroadcast(intent1);
  	}
  	 public synchronized  boolean isFastClick(final int scanCode) {
	        long time = System.currentTimeMillis();   
	        if ( time - lastClickTime < 1000) { 
	        	new Thread(){
	        		public void run() {
	        			//try {
							//Thread.sleep(1000);
							MiscUtils.threadSleep(1000);
							cameraTakepicture(scanCode,"isFastClick");
						//} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						//}
	        			
	        		};
	        	}.start();
	        	Log.i(TAG, "--is FastClick");
	            return true;   
	        }   
	        lastClickTime = time; 
	        Log.i(TAG, "--isn't FastClick");
	        return false;   
	    }
	
	 public void cameraTakepicture(int scanCode,String takeAction) {//m by hcj
	        if (!isFastClick(scanCode)) {
	        	
	        	Intent intent = new Intent();
				intent.setAction(Utils.CAMERA_TAKEPICTURE_ACTION);
				intent.putExtra("takepic_action",takeAction);//+ by hcj
				String takingModel = Utils.getProperty(mContext,Utils.KEY_FTP_TAKINGMODEL);
				String fileSize = Utils.getProperty(mContext,Utils.KEY_FTP_FILESIZE);
				if(scanCode == KET_TEST_TRIGGER){//电平测试
					intent.putExtra(Utils.INTENT_TAKINGMODEL, 1);
					intent.putExtra(Utils.INTENT_FILESIZE,  1);
				}else{
					//正常电平工作
					intent.putExtra(Utils.INTENT_TAKINGMODEL, Integer.parseInt(takingModel));
					intent.putExtra(Utils.INTENT_FILESIZE,  Integer.parseInt(fileSize));
				}
				Log.i(TAG, "--sendBroadcast:"+scanCode);
				mContext.sendBroadcast(intent);
	       }
	    }
	
	private void startCamera(Context context) {
		Intent intent = new Intent();
		intent.setClass(context, BootCameraService.class);
		intent.setAction(Utils.CAMERA_START_ACTION);
		context.startService(intent);
	}

//+ by hcj @{
	private void updateFtpXml(){
	        mContext.sendBroadcast(new Intent("com.gs.updateftpXml"));
	}

	private Runnable mUploadXmlRunnable = new Runnable(){
		@Override
		public void run(){
			Log.i(ConfigUtil.TAG,"mUploadXmlRunnable broadcast com.gs.updateftpXml");
			updateFtpXml();
		}
	};

	private void notifyStateChanged(){
		mContext.sendBroadcast(new Intent("com.cj.state_changed"));
	}

	private static final int SMS_ACTION_CLEAR_PIC = 1;
	private static final int SMS_ACTION_FACTORY_RST = 2;
	private class SmsActionRunnable implements Runnable{
		private int mAction;
		
		public SmsActionRunnable(int action){
			mAction = action;
			Log.i(ConfigUtil.TAG_SMS,"SmsActionRunnable action="+action);
		}
		
		@Override
		public void run(){
			Log.i(ConfigUtil.TAG_SMS,"SmsActionRunnable run mAction="+mAction);
			if(mAction == SMS_ACTION_CLEAR_PIC){
				MiscUtils.deleteSubFiles(Utils.IMG_PATH);
			}else if(mAction == SMS_ACTION_FACTORY_RST){
				Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
				intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
				mContext.sendBroadcast(intent);
			}
		}
	}
//+ by hcj @}
}
