package com.camera.setting.servics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.camera.setting.R;
import com.camera.setting.ftp.FTP;
import com.camera.setting.receiver.BootBroadcastReceiver;
import com.camera.setting.utils.ImgSize;
import com.camera.setting.utils.TakepictureTiem;
import com.camera.setting.utils.Utils;
import com.takeoff.MazeDomotics.AlytJni;
import com.zg.IO;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
//+ by hcj @{
import com.cj.ConfigUtil;
import com.cj.FileUtils;
import com.cj.LogManager;
import com.cj.ShellUtils;
import com.cj.StateRetManager;
import com.cj.MiscUtils;
import com.cj.UpgradeManager;
import com.cj.VarCommon;
import java.util.TimeZone;
import com.zg.IO1;
import android.app.AlarmManager;
import android.app.PendingIntent;

import java.util.Calendar;
//+ by hcj @}

public class BootCameraService extends Service implements PreviewCallback,SurfaceHolder.Callback{
	private static final String TAG = BootCameraService.class.getSimpleName();
	
	private Camera mCamera;
	private static boolean isCreateFloat = true;
	//定义浮动窗口布局  
	private FrameLayout mFloatLayout;  
    private WindowManager.LayoutParams wmParams;  
    //创建浮动窗口设置布局参数的对象
    private WindowManager mWindowManager;  
	private boolean preview = false;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
	private static boolean registed=false;
	private ExecutorService executor = Executors.newFixedThreadPool(3);//同时允许有三帧在保存
	private String imgName;
	private String imgPaht;
	
	private final String trigger584Model = "00-00-00";

	private String triggerModel = "1";//触发模式
    private int takingNum = 1;
    private int takingModel = 1;//1:单拍  2:两拍  3:三连拍
    private int takingID = 1;//拍摄编号
    private int fileSize = 4;
    
    private FTP ftpClient;

    public static OutputStream Power_out = null;
    private boolean gpiostatus=true;
	public static int uploadSleep;
	private static final int START_CAMERA = 10001;
	private static final int SET_GPIO = 10002;
	boolean key68=false;
	private Thread nThread=null;
	private static int isosetup = 0;
	private Intent intentapk = null;
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(ConfigUtil.TAG_UPGRADE, "BootCameraService onDestroy");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		//Log.i(TAG, "--onCreate");
		//Log.i(ConfigUtil.TAG_UPGRADE, "BootCameraService onCreate");
		if(isCreateFloat){
			ftpClient = new FTP(BootCameraService.this);
    			createFloatView();
    			isCreateFloat = false;
    		}
		Random random = new Random();
		uploadSleep=random.nextInt(59);
//+ by hcj @{
		//修复定时拍照模式到达时间段界限后不再拍照问题
		if(uploadSleep == 0){
			uploadSleep=1;
		}
		Log.i(ConfigUtil.TAG_DAYNIGHT,"BootCameraService onCreate current time:"+sdf.format(new Date()));
		DayNightModeInit();
		cjSetCameraConfig(true);
		if(BootBroadcastReceiver.getcurrYearfalg()){
			mTimeSyncDone = true;
			onTimeSyncDone();
		}
		new Thread(new UpgradeManager(BootCameraService.this)).start();
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

		startBroadcastListener();
//+ by hcj @}
		fileSize = Integer.parseInt(Utils.getProperty(this,Utils.KEY_FTP_FILESIZE));
		System.out.println("fileseize------------------"+fileSize);
		String triggerModel = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
		if("1".equals(triggerModel)){
			new Thread() {
		        @Override
		        public void run() {
		        	int fd=AlytJni.openSpiSrdy("/dev/input/event1");
		            while (true) {
		            	int key=AlytJni.readSpiSrdy(fd);
				Log.i(ConfigUtil.TAG,"trigger key="+key);
				if(67==key){
					cjTriggerWork(TRIGGER_BY_KEY_TEST);
					continue;
				}
		            	if(-1!=key && 88!=key && 68!=key){
		                  //  Message msg = new Message();
		                   // handler.sendEmptyMessage(START_CAMERA);
		                   /*- by hcj
		            		int times=Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
		            		System.out.println(photonum+"guosong times"+times);
		            		photonum+=times;
		            		Log.i(TAG, "photonum4="+String.valueOf(photonum));
		            		System.out.println("------------key-------------------"+photonum);
					Log.i(ConfigUtil.TAG_CONTINUES,"startWork by key trigger");
		            		startWork();
					*/
					cjTriggerWork(TRIGGER_BY_KEY_CAMERA);
		            	}
		            }
		        }
		    }.start();
		}else if("3".equals(triggerModel)){
			if(nThread == null){
				nThread = new Thread() {
			        @Override
			        public void run() {
			        	int fd=AlytJni.openSpiSrdy("/dev/input/event1");
			            while (true) {
			            	int key=AlytJni.readSpiSrdy(fd);
					Log.i(ConfigUtil.TAG,"timer test key="+key);
			            	if(67==key){
			                  //  Message msg = new Message();
			                   // handler.sendEmptyMessage(START_CAMERA);
			            		key68=true;
			            		if(mCamera!=null){
						/*- by hcj	
			            		Parameters params = mCamera.getParameters();
			            		Size size=params.getPreviewSize();
			            		if(size.height!=240){
			            			params.setPreviewSize(320, 240);
			            		}
			            		mCamera.setParameters(params);
			            		*/
			            		comSetPreviewSize(320, 240);//+ by hcj
			            		}
						/*- by hcj
			            		photonum=photonum+Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
			            		Log.i(TAG, "photonum5="+String.valueOf(photonum));
						Log.i(ConfigUtil.TAG_CONTINUES,"startWork by key 67");
			            		startWork();
			                    System.out.println("------------key--------67-----------"+photonum);
			                    */
			                    cjTriggerWork(TRIGGER_BY_KEY_TEST);//+ by hcj
			            	}
			            }
			        }
			    };
			    nThread.start();
			}
		}
	}

/*	
	public void startWork(){
		//+ by hcj @{
		Log.i(ConfigUtil.TAG_CONTINUES,"mCamera="+mCamera);
		if(mStartWork){
			Log.i(ConfigUtil.TAG_CONTINUES,"startWork skip by key too fast!");
			return;
		}
		mStartWork = true;
		//+ by hcj @}
		
		//System.out.println("guosong statwork flagseep"+flagSeelp);
		//if (mTimer != null){
		//	return;
		//}
		
		//if(flagSeelp==0){
		//setSleepfalg(1);
		//}
		
		if(isCameraStandby()){
			setCameraIdle();
		}
		if(mCamera!=null){
			//IO.cmd(2);
           // if(!flag){
          //  }
            flag=true;
//+ by hcj @{
		  if(isLightEnable(BootCameraService.this)){
		  	if(setLight(LIGHT_ON)){
				Log.i(ConfigUtil.TAG,"startWork app set light on, delay to work");
				MiscUtils.threadSleep(ConfigUtil.WORK_AFTER_LIGHT_ON_DELAY_MS);//+ by hcj ,the picture would be black with out delay
		  	}
		  }
		  if(key68){
		  	key68 = false;
		  }else{
		  	Size size = mCamera.getParameters().getPreviewSize();
			fileSize=Integer.parseInt(Utils.getProperty(this,Utils.KEY_FTP_FILESIZE));
			String[] size1 = ImgSize.getImgSize(fileSize);
			height = Integer.parseInt(size1[0]);
		      width = Integer.parseInt(size1[1]);
			comSetPreviewSize(width, height);
		  }
//+ by hcj @}
            mCamera.setOneShotPreviewCallback(BootCameraService.this);
            getmTimer();
		}else{
			//startCamera1();
			cjStartCamera();
		}
	}
*/
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "--onBind");
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "--onStartCommand");
		flags = START_STICKY;
		/*
		if(intent!= null && intent.getAction().equals(Utils.CAMERA_START_ACTION)){
			startBroadcastListener();
		}
		//+ by hcj @{
		else if(ConfigUtil.DBG_BROADCAST_TAKE_PIC){
			Log.i(ConfigUtil.TAG,"debug startBroadcastListener");
			startBroadcastListener();
		}
		//+ by hcj @}
		*/
		return super.onStartCommand(intent, flags, startId);
	}
	
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
		Log.i(TAG, "--surfaceChanged format+"+format+" width:"+width+ " height:"+height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "--surfaceCreated holder+"+holder.isCreating());
		//startCamera(holder,false);
		cjStartCamera(holder);//+ by hcj
	}
	

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "--surfaceDestroyed");
		stopCamera("surfaceDestroyed");
	}

	@Override
	public void onPreviewFrame(byte[] arg0, Camera camera) {
		//System.out.println(photonum+"-------"+fileSize);
		//+ by hcj @{
		//Log.i(ConfigUtil.TAG,"main thread="+Thread.currentThread().getId());
		Log.i(ConfigUtil.TAG_CONTINUES,"aaaaa onPreviewFrame totaltime="+(System.currentTimeMillis()-mStartTime)); 
		mLogManager.Log("onPreviewFrame totaltime="+(System.currentTimeMillis()-mStartTime)+",photonum="+photonum);
		//setLight(LIGHT_OFF);
		//+ by hcj @}
		//IO.cmd(3);//- by hcj
		Log.i(TAG, "onPreviewFrame ?photonum");
		if (photonum>0) {
			Log.i(TAG, "onPreviewFrame photonum>0");
			photonum--;
			System.out.println("photonum="+photonum);
			if(photonum==0){
				Log.i(TAG, "onPreviewFrame photonum==0");
				//IO.cmd(3);//- by hcj
				flag = false;
				
				setLight(LIGHT_OFF);//+ by hcj
				mStartWork = false;//+ by hcj
			}
//+ by hcj @{			
			else if(photonum > 0){
				Log.i(ConfigUtil.TAG_CONTINUES,"cjStartWork by continues");
				cjStartWork(false);
			}
//+ by hcj @}
			Message msg = new Message();
			msg.obj = arg0;
			msg.what = 100;
			handler.sendMessage(msg);
		}
//+ by hcj @{		
		else{
			mStartWork = false;
		}
//+ by hcj @}		
	}
	
	ShutterCallback shutterCallback = new ShutterCallback() {  
        public void onShutter() {  
        	  //快门
        	  Log.i(TAG, "-- onShutter");   
        }  
    };
	
	private void startBroadcastListener() {
		//if(!registed){
		    //注册广播接收抓拍请求
			IntentFilter filter = new IntentFilter();
			filter.addAction(Utils.CAMERA_TAKEPICTURE_ACTION);
			filter.addAction("com.gs.getIso");
			filter.addAction("com.cj.state_changed");//+ by hcj
			filter.addAction("com.cj.img_upload.done");//+ by hcj
			filter.addAction("com.cj.alarm.check_day_night_time");//+ by hcj
			filter.addAction("android.deivce.DURING_DAY_MODEL");//+ by hcj
			filter.addAction(Intent.ACTION_TIME_CHANGED);//+ by hcj
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);//+ by hcj
			registerReceiver(receiver, filter);
			registed = true;
			Log.i(TAG, "--register CAMERA_TAKEPICTURE_ACTION Receiver success");
			Utils.registered = true;
		//}
	}
	
	
	/**
	 * 拍摄照片
	 */
	
	PictureCallback jpegCalback = new PictureCallback() {
        
        @SuppressLint("SimpleDateFormat") 
        public void onPictureTaken(final byte[] data, Camera camera) { 
            Log.i(TAG, "--onPictureTaken........");  
            executor.execute(new Runnable() {
				@Override
				public void run() {
					//IO.cmd(3);//- by hcj
					setLight(LIGHT_OFF);//+ by hcj
					String index = "A";
					switch (takingID) {
					case 2:
						index = "A";
						break;
					case 3:
						index = "B";
						break;
					case 4:
						index = "C";
						break;
					}
				    final SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
				    String imgName = sdf.format(new Date())+"-"+triggerModel+"-"+takingModel+"-"+index+"-"+trigger584Model;
					String jpgName =  imgName + ".jpg";
		            final String path = String.format(Utils.IMG_PATH+"%s.jpg",imgName);
		        	File file =new File(path);
		        	if(!file.getParentFile().exists()){
		        		file.getParentFile().mkdirs();
		        	}
		        	try {
		        		Log.i(TAG, "--compress="+data.length);  
		        		FileOutputStream outStream = new FileOutputStream(file);        
		        		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		        		bitmap.compress(Bitmap.CompressFormat.JPEG, 81, outStream); //85 for 100k and 90 for 300k
		        		Log.i(TAG, "--bitmap ="+bitmap.getByteCount()+"kb");
			        	bitmap.recycle();
			        	outStream.close();
						preview = true;
			        	
			        	Intent intent = new Intent();  
			        	intent.setAction(Utils.CAMERA_UPLOAD_SERVICE_ACTION);
			        	intent.putExtra(Utils.INTENT_CAMERA_NAME_JPG, jpgName);
			        	intent.putExtra(Utils.INTENT_CAMERA_PATH, path);
		        		intent.setClass(BootCameraService.this, UploadService.class);
		        		//startService(intent);
		        		sendBroadcast(intent);
		        		System.gc();
		        		System.runFinalization();
			        	
		        	}catch(Exception e){
		        		Log.e(TAG, "--save file exception",e);
		        	}
				}
            });
	        stopCamera("jpegCalback");
        }  
    }; 
    
    private BroadcastReceiver receiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {
			Log.i(ConfigUtil.TAG, "BootCameraService onReceive:"+intent.getAction());
			if(intent.getAction().equals(Utils.CAMERA_TAKEPICTURE_ACTION)){
				//triggerModel = Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TRIGGERMODEL);
				//takingModel = intent.getIntExtra(Utils.INTENT_TAKINGMODEL, 1);
				//fileSize = intent.getIntExtra(Utils.INTENT_FILESIZE, 4);
				//handler.sendEmptyMessage(START_CAMERA);
				if(ConfigUtil.FEATURE_SMS_PIC_SIZE_FIX &&  intent.hasExtra("dx")){
					if(mCamera!=null){
						/*- by hcj
						System.out.println("guosong mcamera!=null");
						Parameters params = mCamera.getParameters();
						params.setPreviewSize(320, 240);
						mCamera.setParameters(params);
						Log.i(TAG, "mCamera.setParameters");
						*/
						comSetPreviewSize(320, 240);//+ by hcj
						key68=true;
					}else{
						System.out.println("guosong mcamera==null");
						key68=true;
						Log.i(TAG, "mCamera null");
					}
				}
				//+ by hcj @{
				cjTriggerWork(intent.hasExtra("dx") ? TRIGGER_BY_SMS : TRIGGER_BY_TIMER);//+ by hcj
				//+ by hcj @}
				/*
				photonum=photonum+Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
				Log.i(TAG, "photonum1="+String.valueOf(photonum));
				Log.i(ConfigUtil.TAG_CONTINUES,"startWork by timer");
				startWork();
				*/
/*- by hcj
			}else if("com.gs.getIso".equals(intent.getAction())){
				System.out.println("guosong set 10 min get ISo vaule");
				isosetup = 0;
				handler.removeMessages(103);
				if(photonum == 0){
				    getIsoVaule();
				} else {
					Message msg = new Message();
					msg.what = 103;
					handler.sendMessageDelayed(msg, 5000);
				}
*/				
			}
		//+ by hcj @{
			else if("com.cj.state_changed".equals(intent.getAction())){
				cjSetCameraConfig(false);
			}else if("com.cj.img_upload.done".equals(intent.getAction())){
				getmTimer();
			}else if("com.cj.alarm.check_day_night_time".equals(intent.getAction())){
				Log.i(ConfigUtil.TAG_DAYNIGHT,"onReceive com.cj.alarm.check_day_night_time");
				fullSleepTimeCheck();
			}else if("android.deivce.DURING_DAY_MODEL".equals(intent.getAction())){
				if(mVarCommon.isCameraFullSleep()){
					Log.i(ConfigUtil.TAG_DAYNIGHT,"onReceive DURING_DAY_MODEL skip by mVarCommon.isCameraFullSleep()");
					return;
				}
				setGetIsoAlarm();
				//from "com.gs.getIso"
				isosetup = 0;
				handler.removeMessages(103);
				if(photonum == 0){
				    getIsoVaule();
				} else {
					Message msg = new Message();
					msg.what = 103;
					handler.sendMessageDelayed(msg, 5000);
				}
			}else if(Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
				||Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())){
				if(BootBroadcastReceiver.getcurrYearfalg()){
					if(!mTimeSyncDone){
						mTimeSyncDone = true;
						onTimeSyncDone();
					}
				}
			}
		//+ by hcj @}
		}
	};
	public void getIsoVaule(){
		//+ by hcj @{
		if(mVarCommon.isCameraFullSleep()){
			Log.i(ConfigUtil.TAG_DAYNIGHT,"getIsoVaule skip by mVarCommon.isCameraFullSleep()");
			return;
		}
		Log.i(ConfigUtil.TAG,"getIsoVaule FEATURE_DAY_NIGHT_CHECK_POLICY="+ConfigUtil.FEATURE_DAY_NIGHT_CHECK_POLICY);
		if(ConfigUtil.FEATURE_DAY_NIGHT_CHECK_POLICY == ConfigUtil.DAY_NIGHT_CHECK_ONLY_AUTOLIGHT_ON){
			String auto_lighte=Utils.getProperty(this, Utils.KEY_FTP_IMG_AUTO_LIGHTE);
			if("0".equals(auto_lighte)){
				Log.i(ConfigUtil.TAG_DAYNIGHT,"getIsoVaule skip by auto light off!");
				setLight(LIGHT_OFF,this);
				DayNightModeSet(true,2);
				return;
			}
		}
		//+ by hcj @}
		String triggerModel = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
		if(isCameraStandby()){
			setCameraIdle();
			getmTimer();
		}
		if(/*"3".equals(triggerModel)*/true){//m by hcj
			/*
			Log.i(TAG, "---startCamera getIsoVaule");
			if(mCamera==null){
				mCamera = Camera.open();
				//IO.cmd(2);
				flag=true;
				Parameters params = mCamera.getParameters();
				fileSize=Integer.parseInt(Utils.getProperty(this,Utils.KEY_FTP_FILESIZE));
				String[] size = ImgSize.getImgSize(fileSize);
			    if("1".equals(Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL))){
					params.set("zsd-mode", "off");
				}
				if(key68){
					params.setPreviewSize(320, 240);
				}else{
					params.setPreviewSize(Integer.parseInt(size[1]), Integer.parseInt(size[0]));
				}
				mCamera.setParameters(params);
				mCamera.startPreview();
				preview = true;
				key68=false;
			}
			*/
			cjStartCamera();
		}else{
			if(flagSeelp==0){
				setSleepfalg(1);
			}
		}
		/*
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		/*
		MiscUtils.threadSleep(5000);
		flagDay(this);
		getmTimer();
		*/
		handler.postDelayed(mCheckDayNightRunnable,5000);
	}
	int height = 1088;//Integer.parseInt(size[0]);
    int width = 1920;//Integer.parseInt(size[1]);
    
/*    
	private void startCamera(SurfaceHolder holder,boolean mflag) {
		Log.i(TAG, "---startCamera");
		if(mCamera==null){
			mCamera = Camera.open();
			Parameters params = mCamera.getParameters();
			fileSize=Integer.parseInt(Utils.getProperty(this,Utils.KEY_FTP_FILESIZE));
			String[] size = ImgSize.getImgSize(fileSize);
			height = Integer.parseInt(size[0]);
		    width = Integer.parseInt(size[1]);
		    Log.i(TAG, "--width:+"+width+" height:"+height);
			params.setPictureSize(width, height);
			params.setPreviewSize(width, height);
			//params.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);
			//if("1".equals(Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL))){
			//params.set("zsd-mode", "off");}
			params.set("zsd-mode", (mWorkPolicy == WORK_POLICY_STANDBY_AFTER_WORK) ? "on" : "off");//+ by hcj
			//params.setPreviewFrameRate(30);
			//params.setBrightnessMode(value);
			//params.setb
			//params.setExposureCompensation(ImgSize.getExposure(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_TIEM))));
			//params.setWhiteBalance(ImgSize.getWhiteBalance(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_WHITE_BALANCE))));
			mCamera.setParameters(params);
			//mCamera.setPreviewCallback(BootCameraService.this);
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mCamera.startPreview();
			preview = true;
			if(mflag){
				//IO.cmd(2);
				//MiscUtils.threadSleep(250);
				flag=mflag;
				//photonum=photonum+Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
			}
			getmTimer();
		}
	}
	private void startCamera1() {
		Log.i(TAG, "---startCamera1");
		if(mCamera==null){
			mCamera = Camera.open();
			//IO.cmd(2);
			flag=true;
			
			Parameters params = mCamera.getParameters();
			fileSize=Integer.parseInt(Utils.getProperty(this,Utils.KEY_FTP_FILESIZE));
			String[] size = ImgSize.getImgSize(fileSize);
			height = Integer.parseInt(size[0]);
		    width = Integer.parseInt(size[1]);
		    //if("1".equals(Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL))){
			params.set("zsd-mode", (mWorkPolicy == WORK_POLICY_STANDBY_AFTER_WORK) ? "on" : "off");
		    Log.i(TAG, "--width:+"+width+" height:"+height);
			//params.setPictureSize(width, height);
			if(key68){
				params.setPreviewSize(320, 240);
			}else{
				params.setPreviewSize(width, height);
			}
			//params.setPreviewFrameRate(30);
			String triggerModel = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
			String auto_lighte=Utils.getProperty(BootCameraService.this, Utils.KEY_FTP_IMG_AUTO_LIGHTE);
			Log.i(ConfigUtil.TAG_LIGHT,"startCamera1 auto_lighte="+auto_lighte+",flagDay="+flagDay);
			//if("3".equals(triggerModel) && !"0".equals(auto_lighte)&&!flagDay){//- by hcj
			//if(isLightEnable(BootCameraService.this)){//+ by hcj
				//IO.cmd(14);
				//IO.cmd(2);//- by hcj
				//setLight(LIGHT_ON);
			//}
			//params.setExposureCompensation(ImgSize.getExposure(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_TIEM))));
			//params.setWhiteBalance(ImgSize.getWhiteBalance(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_WHITE_BALANCE))));
			mCamera.setParameters(params);
			mCamera.startPreview();
//+ by hcj @{		
			Log.i(ConfigUtil.TAG,"startCamera1 delay 1000 by open camera");
			MiscUtils.threadSleep(1000);
			mStartTime = System.currentTimeMillis();
			Log.i(ConfigUtil.TAG_CONTINUES,"aaaaa startWork time="+mStartTime);  
			if(isLightEnable(BootCameraService.this)){//+ by hcj
			  	if(setLight(LIGHT_ON)){
					Log.i(ConfigUtil.TAG,"startWork app set light on, delay to work");
					MiscUtils.threadSleep(ConfigUtil.WORK_AFTER_LIGHT_ON_DELAY_MS);//+ by hcj ,the picture would be black with out delay
			  	}
			}
//+ by hcj @}			
			mCamera.setOneShotPreviewCallback(BootCameraService.this);
			preview = true;
			key68=false;
			getmTimer();
			TakepictureTiem t=new TakepictureTiem(BootCameraService.this);
			t.setAlarmTime(Utils.DURING_DAY_MODEL,10,9);
		}
	}
*/
	
	Timer mTimer;
	public void getmTimer()
	{
		//+ by hcj @{
		if(mWorkPolicy == WORK_POLICY_WORK_ALWAYS){
			Log.i(ConfigUtil.TAG,"getmTimer skip by WORK_POLICY_WORK_ALWAYS");
			return;
		}else if((mWorkPolicy == WORK_POLICY_DAY_WORK_NIGHT_SLEEP) && !mVarCommon.isCameraFullSleep()){
			Log.i(ConfigUtil.TAG,"getmTimer skip by WORK_POLICY_DAY_WORK_NIGHT_SLEEP && !mVarCommon.isCameraFullSleep()");
			return;
		}
		//+ by hcj @}
		if (mTimer != null || "".equals(mTimer))
		{
			mTimer.cancel();
			mTimer=null;
		}
		final String triggerModel = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
		
		mTimer = new Timer();
		int ms = 5000;
		mTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				//IO.cmd(3);//- by hcj
				setLight(LIGHT_OFF);//+ by hcj
				if((mWorkPolicy == WORK_POLICY_SLEEP_AFTER_WORK) || (mWorkPolicy == WORK_POLICY_DAY_WORK_NIGHT_SLEEP)){
					setCameraPowerOffSleep();
				}else{
					setCameraStandby();
				}
				//stopCamera("getmTimer");//- by hcj
				mTimer.cancel();
				mTimer=null;
				//handler.sendEmptyMessage(101);
			}
		}, ms, ms);
	}
	
	/*static*/private Boolean flagDay=true;//true 白天 false 黑夜
	public /*static*/ void flagDay(Context context){
		try{
			/*- by hcj
			String auto_lighte=Utils.getProperty(context, Utils.KEY_FTP_IMG_AUTO_LIGHTE);
			System.out.println("guotest==auto="+auto_lighte);
			if("0".equals(auto_lighte)){
				flagDay=true;
				//IO.cmd(3);//- by hcj
				setLight(LIGHT_OFF,context);//+ by hcj
				//IO.cmd(15);//+ by hcj
				//setDev(2);
				DayNightModeSet(true,2);
				return;
			}
			*/
			String iso_value=SystemProperties.get("camera.iso.value");
			String exptime=SystemProperties.get("camera.exptime.value");//getDev();//
			int getSetExp=ImgSize.getExposure(Integer.parseInt(Utils.getProperty(context, Utils.KEY_FTP_IMG_TIEM)));
			Boolean curDay=true;
			Log.i(ConfigUtil.TAG_DAYNIGHT,"flagDay iso_value="+iso_value+",ftp_exptime="+getSetExp+",real_exptime="+exptime);//+ by hcj
			DayNightModeCheck(context, Integer.parseInt(iso_value), getSetExp, Integer.parseInt(exptime));
			/*
			System.out.println((flagDay!=curDay)+"=guoiso_value="+iso_value+"getSetExp="+getSetExp+"extime="+exptime);
			if(getSetExp!=2 && getSetExp<Integer.parseInt(exptime)){
				System.out.println("guosong--getSetExp<=Integer.parseInt(exptime)");
				Log.i(ConfigUtil.TAG_LIGHT,"Night mode by getSetExp!=2 && getSetExp<Integer.parseInt(exptime)");//+ by hcj
				setDev(getSetExp);
				IO.cmd(14);
				flagDay=false;
			}else if(getSetExp==2){
			}
			else if(Integer.parseInt(iso_value)<=200){
				if(getSetExp<Integer.parseInt(exptime)){
					Log.i(ConfigUtil.TAG_LIGHT,"Night mode by iso_value <= 200 && getSetExp<Integer.parseInt(exptime)");//+ by hcj
					System.out.println("guosong1 iso_value<=200");
					IO.cmd(14);
					flagDay=false;
					setDev(getSetExp);
				}else{
					Log.i(ConfigUtil.TAG_LIGHT,"Day mode by iso_value <= 200 && getSetExp>=Integer.parseInt(exptime)");//+ by hcj
					System.out.println("guosong2 iso_value<=200");
					IO.cmd(15);
					flagDay=true;
					//setCameraState(flagDay);
					setDev(2);
				}
			}else{
				Log.i(ConfigUtil.TAG_LIGHT,"Day mode by iso_value > 200");//+ by hcj
					System.out.println("guosong iso_value>=200");
				IO.cmd(15);
				flagDay=true;
				//setCameraState(flagDay);
				setDev(2);
			}
			*/
			}catch(Exception e){
				System.out.println("guoso------------------erro");
				e.printStackTrace();
			}
	}
	public int flagSeelp=0;
	public void setSleepfalg(int flag){
		System.out.println("flagSeelp="+flagSeelp+"flag="+flag);
		String triggerModel = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
		if(!"3".equals(triggerModel)){
			//setDev(flag);
			flagSeelp=flag;
        }
	}
	public static void setDev(Object o){
		String cmd = String.format("echo %s > %s\n", o, "/sys/bus/platform/drivers/extval/extval_val");
		Log.i(ConfigUtil.TAG,"setDev cmd="+cmd);
		ShellUtils.CommandResult result = ShellUtils.execCommand(cmd, false, true);
		Log.i(ConfigUtil.TAG,"setDev result.result="+result.result+",result.successMsg="+result.successMsg+",result.errorMsg="+result.errorMsg);
/*- by hcj
		//System.out.println("guosong setDev===="+cmd);
        try {
            Process exeEcho = Runtime.getRuntime().exec("sh");
            exeEcho.getOutputStream().write(cmd.getBytes());
            exeEcho.getOutputStream().flush();
        } catch (IOException e) {
        	//e.printStackTrace();
        	Log.i(ConfigUtil.TAG,"setDev e="+e);
        }
*/
	}
	private void stopCamera(String reseaon) {
		//Log.i(TAG, "--stopCamera");
		Log.i(ConfigUtil.TAG,"stopCamera by "+reseaon+",mCamera="+mCamera);
		if (mCamera != null) {
			if (preview){
				mCamera.setOneShotPreviewCallback(null);
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		}
	}
	
	private void createFloatView() {
		wmParams = new WindowManager.LayoutParams();  
        //获取的是WindowManagerImpl.CompatModeWrapper  
        mWindowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);  
        Log.i(TAG, "-- WindowManager:" + mWindowManager);  
        //设置window type  
        wmParams.type = LayoutParams.TYPE_PHONE;   
        //设置图片格式，效果为背景透明  
        wmParams.format = PixelFormat.RGBA_8888;   
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）  
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;        
        //调整悬浮窗显示的停靠位置为左侧置顶  
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;         
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity  
        wmParams.x = 0;  
        wmParams.y = 0;  
  
        //设置悬浮窗口长宽数据    
        wmParams.width = 100;  
        wmParams.height = 100;  
  
         /*// 设置悬浮窗口长宽数据 
        wmParams.width = 200; 
        wmParams.height = 80;*/  
     
        LayoutInflater inflater = LayoutInflater.from(getApplication());  
        //获取浮动窗口视图所在布局  
        mFloatLayout = (FrameLayout) inflater.inflate(R.layout.camera, null);  
        mSurfaceView = (SurfaceView)mFloatLayout.findViewById(R.id.surfaceview);
        //浮动窗口按钮  
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        
      //添加mFloatLayout  
        mWindowManager.addView(mFloatLayout, wmParams);  
        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,  
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec  
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)); 
	}
	int index=0;
	public int gettakingID(){
		int times=Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
		index+=1;
		if(index>times){
			index=1;
		}
		return index;
	}
    int photonum=0;
	@SuppressLint("HandlerLeak") 
	@SuppressWarnings("deprecation")
  	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			System.out.println("guosong msg.what"+msg.what);
  			switch (msg.what) {
  			case START_CAMERA:
  				System.out.println("------------key---ddd----------------");
  				Log.e(TAG, "---START_CAMERA"); 
  				new Thread(){
  					public void run() {
  						if(takingNum <= takingModel){
  		  					takingNum++;
  		  					takingID = takingNum;
  		  					 try {  
  		 			        	//startCamera(mSurfaceHolder,true);
  		 			        	cjStartCamera(mSurfaceHolder);//+ by hcj
  		 			        } catch(Exception e) {  
  		 			            Log.e(TAG, "--BroadcastReceiver error",e);  
  		 			        }
  		 					try {
  		 						//IO.cmd(2);//- by hcj
								//+ by hcj @{
								if(ConfigUtil.DBG_LIGHT){
									Log.i(ConfigUtil.TAG_LIGHT,"handleMessage IO.cmd(2)");
								}
								//+ by hcj @}
  		 						photonum++;
  		 						Log.i(TAG, "photonum2="+String.valueOf(photonum));
  		 						//mCamera.takePicture(null, null, jpegCalback);
  		 						flag=true;
  		 					} catch (Exception e) {
  		 						Log.e(TAG, "--takePicture failed",e);
  		 					}
  		 					//handler.removeMessages(START_CAMERA);
  		  					handler.sendEmptyMessageDelayed(START_CAMERA,10);
  		  				}else{
  		  					takingNum = 1;
  		  					handler.removeMessages(START_CAMERA);
  		  				}
  					};
  				}.start();
  				
  				break;
  			case SET_GPIO:
  				//IO.cmd(3);//- by hcj
  				setLight(LIGHT_OFF);//+ by hcj
				//+ by hcj @{
				if(ConfigUtil.DBG_LIGHT){
					Log.i(ConfigUtil.TAG_LIGHT,"SET_GPIO IO.cmd(3)");
				}
				//+ by hcj @}
	            handler.removeMessages(SET_GPIO);
  				break;
  			case 100:
				Log.i(ConfigUtil.TAG_CONTINUES,"aaaaa handleMessage time="+System.currentTimeMillis()); 
  				System.out.println("guosong msg.what=100="+photonum);
				mLogManager.Log("handleMessage time="+System.currentTimeMillis());
				/*
  				if(photonum>0){
  					Log.i(TAG, "photonum>0");
					Log.i(ConfigUtil.TAG_CONTINUES,"startWork by continues");
  					//startWork();
  					cjStartWork();//+ by hcj
  				}
  				*/
				final byte[] data = (byte[]) msg.obj;
				String index = "A";
				switch (gettakingID()) {
				case 1:
					index = "A";
					break;
				case 2:
					index = "B";
					break;
				case 3:
					index = "C";
					break;
				}
				String imgName = sdf.format(new Date()) +"-"+Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TRIGGERMODEL)+"-"+Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL)+"-"+index+"-"+trigger584Model;
				final String jpgName = imgName + ".jpg";
				final String path = String.format(Utils.IMG_PATH+"%s.jpg",imgName);
				final File file = new File(path);
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				System.out.println("-----key="+path);
				Log.i(TAG, "?2016");
				if(BootBroadcastReceiver.getcurrYearfalg()){
					Log.i(TAG, ">=2016");
				new Thread(){
	        		public void run() {
	        			try {
	        				Log.i(TAG, "pho thread");
	        				pho(data,file,path,jpgName);
	    				} catch (Exception e) {
	    					e.printStackTrace();
	    				}
	        		};
	        	}.start();
				}else{
					//System.out.println("guo test time < 2016 is 2010");
					Log.i(ConfigUtil.TAG,"test time < 2016");
					mLogManager.Log("handleMessage test time < 2016");
				}
				break;
  			case 101:
  				System.out.println("key4================");
  				Intent intent = new Intent();  
	        	intent.setAction("com.gs.camera");
			intent.putExtra("imgPath",(String)msg.obj);
	        	sendBroadcast(intent);
	        	Log.i(ConfigUtil.TAG, "broadcast com.gs.camera");
//+ by hcj @{
				this.removeMessages(104);
				this.sendEmptyMessageDelayed(104,6000);//add delay in BootBroadcastReceiver
//+ by hcj @}
/*- by hcj for block onPreviewFrame()
	        	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	intent.setAction("android.intent.action.updatexml");
	        	sendBroadcast(intent);
	        	System.out.println("guosong update xml");
	        	Log.i(TAG, "android.intent.action.updatexml");
*/
  				break;
/*- by hcj
  			case 102:
  				System.out.println("guosong startWork"+msg.what);
  				//startWork();
				Log.i(ConfigUtil.TAG_CONTINUES,"cjTriggerWork by handleMessage 102");
  				cjTriggerWork("102");//+ by hcj
  				break;
*/
  			case 103:
  				if(photonum == 0){
  					isosetup = 0;
				    getIsoVaule();
				} else {
					if(isosetup >5){
						isosetup = 0;
						break;
					}
					Message msg1 = new Message();
					msg1.what = 103;
					handler.sendMessageDelayed(msg1, 5000);
					isosetup++;
				}
  				break;
		//+ by hcj @{		
			case 104:
				Log.i(ConfigUtil.TAG,"updatexml 1000ms after com.gs.camera");
		        	sendBroadcast(new Intent("android.intent.action.updatexml"));
				break;
		//+ by hcj @}		
  			default:
  				break;
  			}
  		}
  	};
  	long lastClickTime=0;
  	public synchronized  boolean isFastClick() {
        long time = System.currentTimeMillis();   
        if ( time - lastClickTime < 300) { 
        	
            return true;   
        }   
        lastClickTime = time;   
        return false;   
    }
  	static boolean flag = false;
  	final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
  	public void pho(byte[] data,File file,String path,String jpgName) {
  		System.out.println("key2================");
  		Size size = mCamera.getParameters().getPreviewSize();
  		//Log.i(TAG, "pho");
  		Log.i(ConfigUtil.TAG,"pho size="+size.width+"x"+size.height);
		mLogManager.Log("pho jpgName="+jpgName);
		try {
			FileOutputStream outStream = new FileOutputStream(file);
			YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
			if (image != null) {
				Log.i(ConfigUtil.TAG, "pho image !null");
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
				BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
				stream.writeTo(outStream);
				stream.close();
				preview = true;
				Log.i(TAG, "photonum3="+String.valueOf(photonum));
				//if(photonum==0){//- by hcj
					//Log.i(TAG, "photonum == 0");
					//System.out.println("key3================");
	        	//Intent intent = new Intent();  
	        	//intent.setAction(Utils.CAMERA_UPLOAD_SERVICE_ACTION);
	        	//intent.putExtra(Utils.INTENT_CAMERA_NAME_JPG, jpgName);
	        	//intent.putExtra(Utils.INTENT_CAMERA_PATH, path);
	        	//sendBroadcast(intent);
        		//System.gc();
        		//System.runFinalization();
				//handler.sendEmptyMessage(101);//- by hcj
//+ by hcj @{
				//Message.obtain(handler,101,path).sendToTarget();
				Intent intent = new Intent();  
		        	intent.setAction("com.gs.camera");
				intent.putExtra("imgPath",path);
		        	sendBroadcast(intent);
					
				handler.removeMessages(104);
				handler.sendEmptyMessageDelayed(104,6000);//add delay in BootBroadcastReceiver
//+ by hcj @}
				//}//- by hcj
			} else {
				Log.i(ConfigUtil.TAG, "image null");
			}
		} catch (Exception ex) {
			//Log.e("Sys", "Error:" + ex.getMessage());
			Log.i(ConfigUtil.TAG, "pho Exception:"+ex);
			mLogManager.Log("pho Exception:"+ex);
		}
		// }
	}
	public String getImgName() {
		return imgName;
	}

	public void setImgName(String imgName) {
		this.imgName = imgName;
	}

	public String getImgPaht() {
		return imgPaht;
	}

	public void setImgPaht(String imgPaht) {
		this.imgPaht = imgPaht;
	}
	
  	//+ by hcj @{
  	private boolean mStartWork = false;
	private long mStartTime;

	private void cjIoCmd(int cmd){
		Log.i(ConfigUtil.TAG_LIGHT,"cjIoCmd cmd="+cmd);
		IO.cmd(cmd);
	}

	private void cjIoMode(int mode){
		Log.i(ConfigUtil.TAG_LIGHT,"cjIoMode mode="+mode);
		IO1.mode(mode);
	}
	
  	private static final int LIGHT_ON = 2;
	private static final int LIGHT_OFF = 3;
  	private /*static*/ boolean setLight(int state, Context context){
		if(!"3".equals(Utils.getProperty(context,Utils.KEY_FTP_TRIGGERMODEL))){
			if(ConfigUtil.FEATURE_FLASHLIGHT_CTRL_POLICY == ConfigUtil.FLASHLIGHT_CTRL_BY_DRV ){
				Log.i(ConfigUtil.TAG_LIGHT,"setLight skip by FLASHLIGHT_CTRL_BY_DRV");
				return false;
			}else if(state == LIGHT_ON){
				Log.i(ConfigUtil.TAG_LIGHT,"setLight skip by FLASHLIGHT_CTRL_BY_APP && LIGHT_ON");
				return false;
			}
		}
		if(flagDay && (state == LIGHT_ON)){
			Log.i(ConfigUtil.TAG_LIGHT,"setLight skip by flagDay && LIGHT_ON");
			return false;
		}
		Log.i(ConfigUtil.TAG_LIGHT,"setLight state="+state);
		cjIoCmd(state);
		return (state == LIGHT_ON);
  	}

	private boolean setLight(int state){
		return setLight(state,this);
	}

	private void setLightMode(){
		int cmd = 18;
		if(mContinuesNum == 1){
			cmd = 18;
		}else if(mContinuesNum == 2){
			cmd = 19;
		}else if(mContinuesNum == 3){
			cmd = 20;
		}
		if(mWorkPolicy == WORK_POLICY_STANDBY_AFTER_WORK){
			cmd += 3;
		}
		Log.i(ConfigUtil.TAG_LIGHT,"setLightMode cmd="+cmd);
		cjIoMode(cmd);
	}

	private boolean mLightEnable;
	private void setLightEnable(){
		String lightEn=Utils.getProperty(this, Utils.KEY_FTP_IMG_AUTO_LIGHTE);
		boolean enable = (!"0".equals(lightEn) && !flagDay && !mVarCommon.isCameraFullSleep() && mTimeSyncDone);
		Log.i(ConfigUtil.TAG_LIGHT,"setLightEnable lightEn="+lightEn+",flagDay="+flagDay
			+",isCameraFullSleep="+mVarCommon.isCameraFullSleep()+",mTimeSyncDone="+mTimeSyncDone);
		
		int cmd = enable ? 14 : 15;
		cjIoCmd(cmd);
		
		mLightEnable = enable;

		setLightMode();
	}

	private boolean isLightEnable(Context context){
	/*
		  String auto_lighte=Utils.getProperty(context, Utils.KEY_FTP_IMG_AUTO_LIGHTE);
		  boolean enable = (!"0".equals(auto_lighte) && !flagDay);
		  Log.i(ConfigUtil.TAG_LIGHT,"isLightEnable enable="+enable);
		  return enable;
	  */
	  	  return mLightEnable;
	}

	private boolean isFullSleepTime(){
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		//if(ConfigUtil.DBG_DAYNIGHT){
		Log.i(ConfigUtil.TAG_DAYNIGHT,"isFullSleepTime current time1:"+sdf.format(calendar.getTime()));
		//}
		Log.i(ConfigUtil.TAG_DAYNIGHT,"isFullSleepTime hour="+hour+",mRebootTime1="+mRebootTime1+",mRebootTime2="+mRebootTime2);
		//return !(hour >= mRebootTime2 && hour < mRebootTime1);
		return (mRebootTime1 < mRebootTime2 && (hour >= mRebootTime1 && hour < mRebootTime2))
			|| (mRebootTime1 > mRebootTime2 && !(hour >= mRebootTime2 && hour < mRebootTime1));
	}

	private void DayNightModeInit(){
		Log.i(ConfigUtil.TAG_DAYNIGHT,"DayNightModeInit");
/*
		fullSleepTimeCheck();//first cjSetCameraConfig(true) could not call it
		//init state is not full sleep ,so if still not full sleep here, we should reset alarm since fullSleepTimeCheck() skip rest by same mode
		if(!mVarCommon.isCameraFullSleep()){
			cjResetFullSleepAlarm();
		}
*/
		flagDay = false;
		DayNightModeSet(true,2);
	}

	private /*static*/ void DayNightModeSet(boolean isDay, int configExp){
		Log.i(ConfigUtil.TAG_DAYNIGHT,"DayNightModeSet isDay="+isDay);
		if(isDay == flagDay){
			//Log.i(ConfigUtil.TAG_LIGHT,"DayNightModeSet skip by same mode");
			//return;
		}
		flagDay = isDay;
		setLightEnable();
		if(isDay){
			setDev(2);
		}else{
			setDev(configExp);
		}
	}

	private void fullSleepTimeCheck(){
		if(!mTimeSyncDone){
			Log.i(ConfigUtil.TAG_DAYNIGHT,"fullSleepTimeCheck skip by mTimeSyncDone=false");
			return;
		}
		if(mWorkPolicy != WORK_POLICY_DAY_WORK_NIGHT_SLEEP){
			if(mVarCommon.isCameraFullSleep()){
				setCameraFullSleep(false);
			}
			return;
		}
		boolean isFullSleep = isFullSleepTime();
		Log.i(ConfigUtil.TAG_DAYNIGHT,"fullSleepTimeCheck isFullSleep="+isFullSleep);
		setCameraFullSleep(isFullSleep);
	}

	private /*static*/ void DayNightModeCheck(Context context, int isoValue, int configExp, int realExp){
		boolean isDay = true;
		Log.i(ConfigUtil.TAG_DAYNIGHT,"DayNightModeCheck iso threhold="+ConfigUtil.DAY_NIGHT_ISO_THRESHOLD);
		if(configExp != 2 && configExp < realExp){
			Log.i(ConfigUtil.TAG_LIGHT,"Night mode by configExp != 2 && configExp< realExp");
			isDay = false;
		}else if(configExp == 2){
			isDay = flagDay;
		}else if(isoValue <= ConfigUtil.DAY_NIGHT_ISO_THRESHOLD){
			if(configExp < realExp){
				Log.i(ConfigUtil.TAG_DAYNIGHT,"Night mode by iso_value <= threshold && configExp < realExp");
				isDay = false;
			}else{
				Log.i(ConfigUtil.TAG_DAYNIGHT,"Day mode by iso_value <= threshold && configExp>=realExp");
				isDay = true;
			}
		}else{
			Log.i(ConfigUtil.TAG_DAYNIGHT,"Day mode by iso_value > threshold");//+ by hcj
			isDay = true;
		}
		/*
		if(flagDay){
			if(configExp != 2 && configExp < realExp){
				isDay = false;
			}
		}else{
			if(isoValue >= ConfigUtil.DAY_NIGHT_ISO_THRESHOLD){
				isDay = false;
			}
		}
		*/
		if(ConfigUtil.DBG_LIGHT){
			isDay = true;
		}
		DayNightModeSet(isDay,configExp);
	}

	private Runnable mCheckDayNightRunnable = new Runnable(){
		@Override
		public void run(){
			if(mVarCommon.isCameraFullSleep()){
				Log.i(ConfigUtil.TAG_DAYNIGHT,"mCheckDayNightRunnable.run skip by mVarCommon.isCameraFullSleep()");
				return;
			}
			flagDay(BootCameraService.this);
			getmTimer();
		}
	};

	private void comSetPreviewSize(int width, int height){
		if(mCamera == null){
			return;
		}
		Parameters params = mCamera.getParameters();
		Size size=params.getPreviewSize();
		if(size.width == width && size.height == height){
			Log.i(ConfigUtil.TAG,"comSetPreviewSize skip by same size");
			return;
		}
		Log.i(ConfigUtil.TAG,"comSetPreviewSize width="+width+",height="+height);
		params.setPreviewSize(width, height);
		mCamera.setParameters(params);
		mCamera.stopPreview();
		mCamera.startPreview();
	}

	public static String getDev(){
		String retValue = null;
		String cmd = String.format("cat /sys/bus/platform/drivers/extval/extval_val");
		Log.i(ConfigUtil.TAG,"getDev cmd="+cmd);
		ShellUtils.CommandResult result = ShellUtils.execCommand(cmd, false, true);
		if(result != null){
			if(result.result == 0){
				retValue = result.successMsg;
			}else{
				Log.i(ConfigUtil.TAG,"getDev result.result="+result.successMsg+",result.errorMsg="+result.errorMsg);
			}
		}else{
			Log.i(ConfigUtil.TAG,"getDev result is null");
		}
		return retValue;
	}

	private static final int CAMERA_STATE_FLAG_IDLE = 0;
	private static final int CAMERA_STATE_FLAG_SLEEP = 1<<0;
	private static final int CAMERA_STATE_FLAG_STANDBY = 1<<1;
	private static final int CAMERA_STATE_FLAG_FULL_SLEEP = 1<<2;
	private int mCameraState;
	private void setCameraFullSleep(boolean isFullSleep){
		Log.i(ConfigUtil.TAG_POWER,"setCameraFullSleep,isFullSleep="+isFullSleep);
		boolean isFullSleepCurr = mVarCommon.isCameraFullSleep();
		if(isFullSleep){
			mCameraState |= CAMERA_STATE_FLAG_FULL_SLEEP;
		}else{
			mCameraState &= (~CAMERA_STATE_FLAG_FULL_SLEEP);
		}
		mVarCommon.setCameraFullSleep(isFullSleep);

		if(isFullSleepCurr != isFullSleep){
			//reset light
			setLightEnable();
			//reset alarm
			cjResetFullSleepAlarm();
			//delay to poweroff sleep
			if(isFullSleep){
				getmTimer();
			}
		}
		//20161224 + for reboot time change work imediately
		else if((mConfigChangeMask & CONFIG_MASK_REBOOT_TIME) != 0){
			cjResetFullSleepAlarm();
		}
	}

	private boolean isCameraFullSleep(){
		return (mCameraState&CAMERA_STATE_FLAG_FULL_SLEEP) != 0;
	}
	
	private void setCameraPowerOffSleep(){
		stopCamera("setCameraPowerOffSleep");
		mCameraState |= CAMERA_STATE_FLAG_SLEEP;
		Log.i(ConfigUtil.TAG_POWER,"setCameraPowerOffSleep,mCameraState="+mCameraState);
		mLogManager.Log("setCameraPowerOffSleep");
	}

	private void setCameraStandby(){
		if(mVarCommon.isImgUploading()){
			Log.i(ConfigUtil.TAG_POWER,"setCameraStandby,skip by isImgUploading()");
			mLogManager.Log("setCameraStandby,skip by isImgUploading()");
			return;
		}
		cjSetCameraZsd(true);
		
		setDev(0);
		mCameraState |= CAMERA_STATE_FLAG_STANDBY;
		Log.i(ConfigUtil.TAG_POWER,"setCameraStandby,mCameraState="+mCameraState);
		mLogManager.Log("setCameraStandby");
	}

	private void setCameraIdle(){
		setDev(1);
		mCameraState &= (~CAMERA_STATE_FLAG_STANDBY);
		Log.i(ConfigUtil.TAG_POWER,"setCameraIdle,mCameraState="+mCameraState);
		mLogManager.Log("setCameraIdle");
	}

	private boolean isCameraSleep(){
		return /*(mCameraState&CAMERA_STATE_FLAG_SLEEP) != 0*/(mCamera == null);
	}

	private boolean isCameraStandby(){
		return (mCameraState&CAMERA_STATE_FLAG_STANDBY) != 0;
	}

	private void cjResetFullSleepAlarm(){
		if(!mTimeSyncDone){
			Log.i(ConfigUtil.TAG_DAYNIGHT,"cjResetFullSleepAlarm skip by mTimeSyncDone=false");
			return;
		}
		Log.i(ConfigUtil.TAG_DAYNIGHT,"cjResetFullSleepAlarm");
		if(mWorkPolicy != WORK_POLICY_DAY_WORK_NIGHT_SLEEP){
			cancelFullSleepCheckAlarm();
			//set iso alarm
			setGetIsoAlarm();
			return;
		}
		
		setFullSleepCheckAlarm(mVarCommon.isCameraFullSleep());
		if(mVarCommon.isCameraFullSleep()){
			cancelGetIsoAlarm();
		}else{
			setGetIsoAlarm();
		}
	}

	private static final int WORK_POLICY_WORK_ALWAYS = 0;
	private static final int WORK_POLICY_DAY_WORK_NIGHT_SLEEP = 1;
	private static final int WORK_POLICY_SLEEP_AFTER_WORK = 2;
	private static final int WORK_POLICY_STANDBY_AFTER_WORK = 3;
	private int mWorkPolicy = -1;
	private void cjSetCameraWorkPolicy(){
		String wb = Utils.getProperty(this,Utils.KEY_FTP_IMG_WHITE_BALANCE);
		int oldPolicy = mWorkPolicy;
		if(mCameraTriggerMode == CAMERA_TRIGGER_MODE_TIMER){
			mWorkPolicy = WORK_POLICY_SLEEP_AFTER_WORK;
		}else{
			if("1".equals(wb)){
				mWorkPolicy = WORK_POLICY_WORK_ALWAYS;
			}else if("2".equals(wb)){
				mWorkPolicy = WORK_POLICY_DAY_WORK_NIGHT_SLEEP;
			}else if("3".equals(wb)){
				mWorkPolicy = WORK_POLICY_SLEEP_AFTER_WORK;
			}else if("4".equals(wb)){
				mWorkPolicy = WORK_POLICY_STANDBY_AFTER_WORK;
			}
		}
		Log.i(ConfigUtil.TAG,"setCameraWorkPolicy mWorkPolicy="+mWorkPolicy+",oldPolicy="+oldPolicy);
		
		if(oldPolicy != mWorkPolicy && oldPolicy != -1){
			mConfigChangeMask |= CONFIG_MASK_WORK_POLICY;
		}
	}

	private static final int CAMERA_TRIGGER_MODE_KEY = 0;
	private static final int CAMERA_TRIGGER_MODE_TIMER = 1;
	private int mCameraTriggerMode = CAMERA_TRIGGER_MODE_KEY;
	private void cjSetCameraTriggerMode(){
		String mode = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
		if("1".equals(mode)){
			mCameraTriggerMode = CAMERA_TRIGGER_MODE_KEY;
		}else if("3".equals(mode)){
			mCameraTriggerMode = CAMERA_TRIGGER_MODE_TIMER;
		}
		Log.i(ConfigUtil.TAG,"setCameraTriggerMode mCameraTriggerMode="+mCameraTriggerMode);
	}

	private int mContinuesNum = -1;
	private void cjSetCameraContinuesNum(){
		int oldNum = mContinuesNum;
		mContinuesNum = MiscUtils.strToInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL),1);
		//setLightEnable();
		if(oldNum != mContinuesNum && oldNum != -1){
			mConfigChangeMask |= CONFIG_MASK_CONTINUS_NUM;
		}
		Log.i(ConfigUtil.TAG,"cjSetCameraContinuesNum mContinuesNum="+mContinuesNum);
	}

	private int mRebootTime1 = -1;
	private int mRebootTime2 = -1;
	private void cjSetCameraRebootTime(){
		String rebootTime = Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_REBOOTIEM);
		Log.i(ConfigUtil.TAG,"cjSetCameraRebootTime rebootTime="+rebootTime);
		if(rebootTime == null){
			return;
		}
		String[] splitStrs = rebootTime.split("-");
		if(splitStrs == null || splitStrs.length != 2){
			Log.i(ConfigUtil.TAG,"cjSetCameraRebootTime splitStrs invalid");
			return;
		}
		int time1 = MiscUtils.strToInt(splitStrs[0],-1);
		int time2 = MiscUtils.strToInt(splitStrs[1],-1);
		if((mRebootTime1 != time1 && mRebootTime1 != -1) || (mRebootTime2 != time2 && mRebootTime2 != -1)){
			mConfigChangeMask |= CONFIG_MASK_REBOOT_TIME;
		}
		mRebootTime1 = time1;
		mRebootTime2 = time2;
		Log.i(ConfigUtil.TAG,"cjSetCameraRebootTime mRebootTime1="+mRebootTime1+",mRebootTime2="+mRebootTime2);
	}

	private int mUploadMode = -1;
	private boolean mClrOldPictures;
	private void cjSetCameraUploadMode(){
		String brightness = Utils.getProperty(this,Utils.KEY_FTP_IMG_BRIGHTNESS);
		int mode = ConfigUtil.UPLOAD_MODE_SAVE;
		mClrOldPictures = false;
		
		if("1".equals(brightness)){
			mode = ConfigUtil.UPLOAD_MODE_SAVE;
		}else if("2".equals(brightness)){
			mode = ConfigUtil.UPLOAD_MODE_REALTIME;
			mClrOldPictures = true;
		}else if("3".equals(brightness)){
			mode = ConfigUtil.UPLOAD_MODE_FULL;
		}else if("4".equals(brightness)){
			mode = ConfigUtil.UPLOAD_MODE_SAVE;
			mClrOldPictures = true;
		}else if("5".equals(brightness)){
			mode = ConfigUtil.UPLOAD_MODE_REALTIME;
			mClrOldPictures = true;
		}else if("6".equals(brightness)){
			mode = ConfigUtil.UPLOAD_MODE_FULL;
			mClrOldPictures = true;
		}else{
			Log.i(ConfigUtil.TAG,"initUploadMode policy invalide config compose: brightness="+brightness);
		}
		if(mUploadMode != mode){
			mUploadMode = mode;
			mVarCommon.setUploadMode(mUploadMode);
			mConfigChangeMask |= CONFIG_MASK_UPLOAD_MODE;
		}
		else if(mClrOldPictures){
			mConfigChangeMask |= CONFIG_MASK_UPLOAD_MODE;
		}
		Log.i(ConfigUtil.TAG,"cjSetCameraUploadMode mUploadMode="+mUploadMode+",mClrOldPictures="+mClrOldPictures);
	}

	private void cjHandleConfigChange(boolean isInit){
		Log.i(ConfigUtil.TAG,"cjHandleConfigChange mConfigChangeMask="+mConfigChangeMask);
		if((mConfigChangeMask & CONFIG_MASK_WORK_POLICY) != 0){
			//mVarCommon.setDayNightCheckByTime(mWorkPolicy == WORK_POLICY_DAY_WORK_NIGHT_SLEEP);
			//reset Camera work, so on
			if((mWorkPolicy == WORK_POLICY_WORK_ALWAYS) || ((mWorkPolicy == WORK_POLICY_DAY_WORK_NIGHT_SLEEP) && !mVarCommon.isCameraFullSleep())){
				if(isCameraStandby()){
					setCameraIdle();
				}
				if(isCameraSleep()){
					cjStartCamera();
				}
			}else{
				getmTimer();
			}
		}
		
		if((mConfigChangeMask & (CONFIG_MASK_WORK_POLICY|CONFIG_MASK_REBOOT_TIME)) != 0){
			fullSleepTimeCheck();
		}
		if((mConfigChangeMask & CONFIG_MASK_CONTINUS_NUM) != 0){
			setLightEnable();
		}
		if((mConfigChangeMask & CONFIG_MASK_UPLOAD_MODE) != 0){
			if(mClrOldPictures){
				Log.i(ConfigUtil.TAG,"cjHandleConfigChange clear Utils.IMG_PATH");
				MiscUtils.deleteSubFiles(Utils.IMG_PATH);
				resetUploadMode();
			}
			if(isInit && !mClrOldPictures){
				Log.i(ConfigUtil.TAG,"cjHandleConfigChange upload Utils.IMG_PATH");
				Intent intent = new Intent("com.cj.init_upload");
				intent.setClass(this, UploadService.class);
				this.startService(intent);
			}
		}
	}

	private static final int CONFIG_MASK_TRIGGER_MODE = 1<<0;
	private static final int CONFIG_MASK_REBOOT_TIME = 1<<1;
	private static final int CONFIG_MASK_WORK_POLICY = 1<<2;
	private static final int CONFIG_MASK_CONTINUS_NUM = 1<<3;
	private static final int CONFIG_MASK_UPLOAD_MODE = 1<<4;
	private int mConfigChangeMask;
	private void cjSetCameraConfig(boolean isInit){
		mConfigChangeMask = 0;
		cjSetCameraTriggerMode();//must set first
		cjSetCameraRebootTime();//must before set work policy
		cjSetCameraWorkPolicy();
		cjSetCameraContinuesNum();
		cjSetCameraUploadMode();
		cjHandleConfigChange(isInit);
		mConfigChangeMask = 0;
	}

	private static final int TRIGGER_BY_KEY_CAMERA = 0;
	private static final int TRIGGER_BY_KEY_TEST = 1;
	private static final int TRIGGER_BY_TIMER = 2;
	private static final int TRIGGER_BY_SMS = 3;
	private static final int TRIGGER_BY_ISO = 4;
	private int mTriggerBy;
	//private int mTriggerByLast;

	private boolean cjUseLowestPicSize(){
		return (mTriggerBy == TRIGGER_BY_KEY_TEST);
	}
	
	private void cjTriggerWork(int triggerBy){
		if(!mTimeSyncDone){
			Log.i(ConfigUtil.TAG_DAYNIGHT,"cjTriggerWork skip by mTimeSyncDone=false");
			mLogManager.Log("cjTriggerWork skip by mTimeSyncDone=false");
			return;
		}
		
		if((mCameraTriggerMode == CAMERA_TRIGGER_MODE_KEY) && mVarCommon.isCameraFullSleep()){
			Log.i(ConfigUtil.TAG,"cjTriggerWork skip by CAMERA_TRIGGER_MODE_KEY && mVarCommon.isCameraFullSleep()");
			//cjGetFtpStateXml();
			mLogManager.Log("cjTriggerWork skip by CAMERA_TRIGGER_MODE_KEY && mVarCommon.isCameraFullSleep()");
			return;
		}

		if(mStartWork){
			Log.i(ConfigUtil.TAG_CONTINUES,"cjStartWork skip by key too fast!");
			//photonum=photonum+Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
			//Log.i(ConfigUtil.TAG,"cjTriggerWork photonum="+photonum);
			mLogManager.Log("cjStartWork skip by key too fast!");
			return;
		}
		mStartWork = true;
		mTriggerBy = triggerBy;
		Log.i(ConfigUtil.TAG,"cjTriggerWork by "+triggerBy+", start time="+System.currentTimeMillis());
		mLogManager.Log("cjTriggerWork by "+triggerBy+", start time="+System.currentTimeMillis());

		boolean delayed = false;
		if(isCameraStandby()){
			cjSetCameraZsd(false);
			setCameraIdle();
			Log.i(ConfigUtil.TAG, "cjStartWork setCameraIdle delay");
			MiscUtils.threadSleep(50);
			delayed = true;
		}

		setLight(LIGHT_ON);
		if(/*(ConfigUtil.FEATURE_FLASHLIGHT_CTRL_POLICY == ConfigUtil.FLASHLIGHT_CTRL_BY_DRV)
			&&*/ isLightEnable(BootCameraService.this)
			&& !delayed
			&& (mCameraTriggerMode == CAMERA_TRIGGER_MODE_KEY)){
			Log.i(ConfigUtil.TAG,"cjTriggerWork drv set light on, delay to work");
			MiscUtils.threadSleep(ConfigUtil.WORK_AFTER_LIGHT_ON_DELAY_MS);//+ by hcj ,the picture would be black with out delay
		}

		index = 0;//for picture index
		photonum=photonum+Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
		Log.i(ConfigUtil.TAG,"cjTriggerWork photonum="+photonum);
		mLogManager.Log("cjTriggerWork photonum="+photonum);
		
		cjStartWork(true);
	}

	private void cjStartWork(boolean newTrigger){
		Log.i(ConfigUtil.TAG, "cjStartWork newTrigger="+newTrigger);
		mLogManager.Log("cjStartWork newTrigger="+newTrigger);
		if(isCameraStandby()){
			setCameraIdle();
		}
		if(isCameraSleep()){
			cjStartCamera();
		}else if(newTrigger){
			if(cjUseLowestPicSize()){
				width = 320;
				height = 240;
			}else{
				fileSize=Integer.parseInt(Utils.getProperty(this,Utils.KEY_FTP_FILESIZE));
				String[] size1 = ImgSize.getImgSize(fileSize);
				height = Integer.parseInt(size1[0]);
				width = Integer.parseInt(size1[1]);
			}
			comSetPreviewSize(width, height);
		}
		
		cjTakPicture();
	}

	private void cjStartCamera(){
		cjStartCamera(null);
	}

	private void cjStartCamera(SurfaceHolder holder){
		if(mCamera != null){
			Log.i(ConfigUtil.TAG, "cjStartCamera mCamera != null");
			return;
		}
		Log.i(ConfigUtil.TAG, "cjStartCamera holder="+holder);
		if(mLogManager != null)
		mLogManager.Log("cjStartCamera holder="+holder);
		
		try{
			mCamera = Camera.open();
		}catch(Exception e){
			Log.i(ConfigUtil.TAG, "cjStartCamera e="+e);
			if(mLogManager != null)
			mLogManager.Log("cjStartCamera e="+e);
		}

		if(mCamera == null){
			Log.i(ConfigUtil.TAG, "cjStartCamera return by Camera.open() fail");
			if(mLogManager != null)
			mLogManager.Log("cjStartCamera return by Camera.open() fail");
			return;
		}
		
		flag=true;
		
		Parameters params = mCamera.getParameters();
		fileSize=Integer.parseInt(Utils.getProperty(this,Utils.KEY_FTP_FILESIZE));
		String[] size = ImgSize.getImgSize(fileSize);
		height = Integer.parseInt(size[0]);
	       width = Integer.parseInt(size[1]);
		   Log.i(ConfigUtil.TAG, "cjStartCamera key68="+key68+",mWorkPolicy="+mWorkPolicy);
		if(cjUseLowestPicSize()){
 			params.setPreviewSize(320, 240);
		}else{
			params.setPreviewSize(width, height);
		}
		//params.setPreviewFrameRate(15);//deprecated
		//int[] range = new int[2];
		//params.getPreviewFpsRange(range);
		//Log.i(ConfigUtil.TAG, "cjStartCamera1 range[0]="+range[0]+",range[1]="+range[1]);//min:5000, max:60000
		//params.setPreviewFpsRange(ConfigUtil.CAM_PARAM_PREVIEW_FPS,ConfigUtil.CAM_PARAM_PREVIEW_FPS);
		
		String triggerModel = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
		String auto_lighte=Utils.getProperty(BootCameraService.this, Utils.KEY_FTP_IMG_AUTO_LIGHTE);
		//params.setExposureCompensation(ImgSize.getExposure(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_TIEM))));
		//params.setWhiteBalance(ImgSize.getWhiteBalance(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_WHITE_BALANCE))));
		params.set("zsd-mode", (mWorkPolicy == WORK_POLICY_STANDBY_AFTER_WORK) ? "on" : "off");
		//params.setEdgeMode("high");
		params.setContrastMode("high");
		mCamera.setParameters(params);
		if(holder != null){
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mCamera.startPreview();
		Log.i(ConfigUtil.TAG,"cjStartCamera delay 1000 by open camera");  
		if(mLogManager != null)
		mLogManager.Log("cjStartCamera delay 1000 by open camera");
		MiscUtils.threadSleep(1000);
		preview = true;
		
		getmTimer();
		//TakepictureTiem t=new TakepictureTiem(BootCameraService.this);
		//t.setAlarmTime(Utils.DURING_DAY_MODEL,ConfigUtil.DBG_ISO ? 2: 10,9);
		if(holder == null){
			setGetIsoAlarm();
		}
	}

	private void cjTakPicture(){
		if(mCamera == null){
			mStartWork = false;
			Log.i(ConfigUtil.TAG,"cjTakPicture skip by mCamera == null");
			mLogManager.Log("cjTakPicture skip by mCamera == null");
			return;
		}
		/*
		if(isLightEnable(BootCameraService.this) && setLight(LIGHT_ON)){
			Log.i(ConfigUtil.TAG,"cjTakPicture app set light on, delay to work");
			MiscUtils.threadSleep(ConfigUtil.WORK_AFTER_LIGHT_ON_DELAY_MS);//+ by hcj ,the picture would be black with out delay
		}
		*/
		mStartTime = System.currentTimeMillis();
		Log.i(ConfigUtil.TAG_CONTINUES,"cjTakPicture time="+mStartTime);
		mLogManager.Log("cjTakPicture time="+mStartTime);
		mCamera.setOneShotPreviewCallback(BootCameraService.this);
		getmTimer();
	}

	private void cjSetCameraZsd(boolean on){
		if(mCamera == null){
			Log.i(ConfigUtil.TAG, "cjSetCameraZsd skip by mCamera == null");
			return;
		}
		Log.i(ConfigUtil.TAG, "cjSetCameraZsd on="+on);
		Parameters params = mCamera.getParameters();
		params.set("zsd-mode", on ? "on" : "off");
		mCamera.setParameters(params);
	}

	private void cjGetFtpStateXml(){
		sendBroadcast(new Intent("android.intent.action.updatexml"));
	}

	//
	private VarCommon mVarCommon = VarCommon.getInstance();

	//alarm
	private PendingIntent getIsoCheckIntent(){
		Intent intent = new Intent(Utils.DURING_DAY_MODEL);
		return PendingIntent.getBroadcast(this, 9, intent,PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private boolean mIsoAlarmHasSet;
	private void setGetIsoAlarm(){
		//TakepictureTiem t=new TakepictureTiem(BootCameraService.this);
		//t.setAlarmTime(Utils.DURING_DAY_MODEL,10,9);
		PendingIntent pIntent = getIsoCheckIntent();
		cancelAlarm(pIntent);

		int timeInMillis = ConfigUtil.ISO_CHECK_PERIOD_MIN;
		if(!mIsoAlarmHasSet){
			mIsoAlarmHasSet = true;
			timeInMillis = 1;
		}
		Calendar calendar =Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//calendar.add(calendar.MINUTE, timeInMillis);
		//+ by hcj @{
		if(ConfigUtil.DBG_ISO && (timeInMillis == ConfigUtil.ISO_CHECK_PERIOD_MIN)){
			timeInMillis = 2;
		}
		//+ by hcj @}
		//if(albarmTimers == 10){
		//calendar.add(calendar.MINUTE, timeInMillis+1);
		//} else {
		calendar.add(calendar.MINUTE, timeInMillis);
		//}
		calendar.set(Calendar.SECOND, uploadSleep);   
		calendar.clear(calendar.MILLISECOND);
		//if(ConfigUtil.DBG_DAYNIGHT){
		Log.i(ConfigUtil.TAG_DAYNIGHT,"setGetIsoAlarm set alarm at:"+sdf.format(calendar.getTime()));
		//}
		if(mLogManager != null)
			mLogManager.Log("setGetIsoAlarm set alarm at:"+sdf.format(calendar.getTime()));
		startAlarm(pIntent,calendar.getTimeInMillis());
	}

	private void cancelGetIsoAlarm(){
		PendingIntent pIntent = getIsoCheckIntent();
		cancelAlarm(pIntent);
		if(mLogManager != null)
			mLogManager.Log("cancelGetIsoAlarm");
	}
	
	private PendingIntent getFullSleepCheckIntent(){
		Intent intent = new Intent("com.cj.alarm.check_day_night_time");
		return PendingIntent.getBroadcast(this,ALARM_REQUEST_DAY_NIGHT_TIME, 
			intent,PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	private static final int ALARM_REQUEST_DAY_NIGHT_TIME = 100;
	private void setFullSleepCheckAlarm(boolean isFullSleepCurr){
		if(mWorkPolicy != WORK_POLICY_DAY_WORK_NIGHT_SLEEP){
			Log.i(ConfigUtil.TAG_DAYNIGHT,"setFullSleepCheckAlarm skip by mWorkPolicy != WORK_POLICY_DAY_WORK_NIGHT_SLEEP");
			return;
		}
		PendingIntent pIntent = getFullSleepCheckIntent();
		cancelAlarm(pIntent);
		
		Calendar calendar =Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		int currHour = calendar.get(Calendar.HOUR_OF_DAY);
		//calendar.set(Calendar.HOUR_OF_DAY, isFullSleepCurr ? mRebootTime2 : mRebootTime1);
		calendar.set(Calendar.MINUTE, 0);            //设置闹钟的分钟数
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		/*
		if(isFullSleepCurr && (mRebootTime2 >=0 && mRebootTime2 < mRebootTime1)){
			//second day
			calendar.add(Calendar.DAY_OF_MONTH,1);
		}
		*/
		Log.i(ConfigUtil.TAG_DAYNIGHT,"setFullSleepCheckAlarm currHour:"+currHour);
		/*
		if(isFullSleepCurr && (currHour < 24 && currHour >= mRebootTime1)){
			calendar.add(Calendar.DAY_OF_MONTH,1);
		}*/
		int toHour = isFullSleepCurr ? mRebootTime2 : mRebootTime1;
		if(toHour > currHour){
			calendar.add(Calendar.HOUR_OF_DAY,toHour-currHour);
		}else{
			calendar.add(Calendar.HOUR_OF_DAY,24-(currHour-toHour));
		}

		//if(ConfigUtil.DBG_DAYNIGHT){
		Log.i(ConfigUtil.TAG_DAYNIGHT,"setFullSleepCheckAlarm set alarm at:"+sdf.format(calendar.getTime()));
		//}
		startAlarm(pIntent,calendar.getTimeInMillis());
	}

	private void cancelFullSleepCheckAlarm(){
		cancelAlarm(getFullSleepCheckIntent());
	}
		
	private AlarmManager mAlarmManager;

	private void startAlarm(PendingIntent intent, long timeInMillis){
		if(mAlarmManager == null){
			mAlarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
		}
		mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, intent); 
	}

	private void cancelAlarm(PendingIntent intent){
		Log.i(ConfigUtil.TAG_DAYNIGHT,"cancelAlarm");
		if(mAlarmManager == null){
			return;
		}
		mAlarmManager.cancel(intent);
	}

	//time sync
	private boolean mTimeSyncDone;
	private void onTimeSyncDone(){
		Log.i(ConfigUtil.TAG_DAYNIGHT,"onTimeSyncDone");
		mVarCommon.setTimeSyncDone(true);
		StateRetManager.getInstance(this).onTimeSyncDone();
		fullSleepTimeCheck();
		if(!mVarCommon.isCameraFullSleep()){
			cjResetFullSleepAlarm();
		}
		mLogManager = LogManager.getInstance();
	}

	private void resetUploadMode(){
		final String tmpXml = "//sdcard/state_w.xml";
		try{
			FileInputStream fis = new FileInputStream(Utils.XML_PATH);
			FileOutputStream fos = new FileOutputStream(tmpXml);
			InputStreamReader isr = new InputStreamReader(fis,"utf8");
			BufferedReader br = new BufferedReader(isr);
			OutputStreamWriter osw = new OutputStreamWriter(fos,"utf8");
			BufferedWriter bw = new BufferedWriter(osw);
			String line;
			while((line=br.readLine()) != null){
				String stripLine = line.trim();
				if(stripLine.startsWith("<img_brightness>")){
					line = "   <img_brightness>"+(mUploadMode+1)+"</img_brightness>";
				}
				bw.write(line);
				bw.write("\n");
			}
			bw.close();
			osw.close();
			br.close();
			isr.close();

			//override
			FileUtils.copyFile(tmpXml,Utils.XML_PATH);
			new File(tmpXml).delete();

			//upate ftp xml
			this.sendBroadcast(new Intent("com.gs.updateftpXml"));
		}catch(Exception e){
			Log.i(ConfigUtil.TAG,"resetUploadMode e="+e);
		}
	}

	private LogManager mLogManager;
//+ by hcj @}
}
