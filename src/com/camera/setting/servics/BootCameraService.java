package com.camera.setting.servics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import com.cj.UpgradeManager;
import java.util.TimeZone;
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
		Log.i(TAG, "--onCreate");
		Log.i(ConfigUtil.TAG_UPGRADE, "BootCameraService onCreate");
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
		            	if(-1!=key && 88!=key && 68!=key){
					if(ConfigUtil.FEATURE_DISABLE_TAKEPIC_ON_NIGHT && !flagDay){
						Log.i(ConfigUtil.TAG,"skip take picture on night mode");
						continue;
					}
		                  //  Message msg = new Message();
		                   // handler.sendEmptyMessage(START_CAMERA);
		            		int times=Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
		            		System.out.println(photonum+"guosong times"+times);
		            		photonum+=times;
		            		Log.i(TAG, "photonum4="+String.valueOf(photonum));
		            		System.out.println("------------key-------------------"+photonum);
					Log.i(ConfigUtil.TAG_CONTINUES,"startWork by key trigger");
		            		startWork();
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
			            	if(67==key){
			                  //  Message msg = new Message();
			                   // handler.sendEmptyMessage(START_CAMERA);
			            		key68=true;
			            		if(mCamera!=null){
			            		Parameters params = mCamera.getParameters();
			            		Size size=params.getPreviewSize();
			            		if(size.height!=240){
			            			params.setPreviewSize(320, 240);
			            		}
			            		mCamera.setParameters(params);
			            		}
			            		photonum=photonum+Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
			            		Log.i(TAG, "photonum5="+String.valueOf(photonum));
						Log.i(ConfigUtil.TAG_CONTINUES,"startWork by key 67");
			            		startWork();
			                    System.out.println("------------key--------67-----------"+photonum);
			            	}
			            }
			        }
			    };
			    nThread.start();
			}
		}
//+ by hcj @{
		new Thread(new UpgradeManager(BootCameraService.this)).start();
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
		DayNightModeInit();
//+ by hcj @}
	}
	public void startWork(){
		//+ by hcj @{
		Log.i(ConfigUtil.TAG_CONTINUES,"mCamera="+mCamera);
		/*
		if(mStartWork){
			Log.i(ConfigUtil.TAG_CONTINUES,"startWork skip by key too fast!");
			return;
		}
		mStartWork = true;*/
		//+ by hcj @}
		
		//System.out.println("guosong statwork flagseep"+flagSeelp);
		//if (mTimer != null){
		//	return;
		//}
		if(flagSeelp==0){
		setSleepfalg(1);
		}
		if(mCamera!=null){
			/*IO.cmd(2);*/
           // if(!flag){
            try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
          //  }
            flag=true;
		  //+ by hcj @{
		  if(isLightEnable(BootCameraService.this)){
		  	setLight(LIGHT_ON);
		  }
		  //+ by hcj @}
		Log.i(ConfigUtil.TAG_CONTINUES,"aaaaa startWork time="+System.currentTimeMillis());  
            mCamera.setOneShotPreviewCallback(BootCameraService.this);
            
            getmTimer();
		}else{
			startCamera1();
		}
	}
	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "--onBind");
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "--onStartCommand");
		flags = START_STICKY;
		if(intent!= null && intent.getAction().equals(Utils.CAMERA_START_ACTION)){
			startBroadcastListener();
		}
		//+ by hcj @{
		else if(ConfigUtil.DBG_BROADCAST_TAKE_PIC){
			Log.i(ConfigUtil.TAG,"debug startBroadcastListener");
			startBroadcastListener();
		}
		//+ by hcj @}
		return super.onStartCommand(intent, flags, startId);
	}
	
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
		Log.i(TAG, "--surfaceChanged format+"+format+" width:"+width+ " height:"+height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "--surfaceCreated holder+"+holder.isCreating());
		startCamera(holder,false);
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
		Log.i(ConfigUtil.TAG_CONTINUES,"aaaaa onPreviewFrame time="+System.currentTimeMillis()); 
		//mStartWork = false;
		setLight(LIGHT_OFF);
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
				setLight(LIGHT_OFF);//+ by hcj
				flag = false;
			}
			Message msg = new Message();
			msg.obj = arg0;
			msg.what = 100;
			handler.sendMessage(msg);
		}
	}
	
	ShutterCallback shutterCallback = new ShutterCallback() {  
        public void onShutter() {  
        	  //快门
        	  Log.i(TAG, "-- onShutter");   
        }  
    };
	
	private void startBroadcastListener() {
		if(!registed){
		    //注册广播接收抓拍请求
			IntentFilter filter = new IntentFilter();
			filter.addAction(Utils.CAMERA_TAKEPICTURE_ACTION);
			filter.addAction("com.gs.getIso");
			registerReceiver(receiver, filter);
			registed = true;
			Log.i(TAG, "--register CAMERA_TAKEPICTURE_ACTION Receiver success");
			Utils.registered = true;
		}
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
			Log.i(TAG, "--action:"+intent.getAction());
			if(intent.getAction().equals(Utils.CAMERA_TAKEPICTURE_ACTION)){
				//triggerModel = Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TRIGGERMODEL);
				//takingModel = intent.getIntExtra(Utils.INTENT_TAKINGMODEL, 1);
				//fileSize = intent.getIntExtra(Utils.INTENT_FILESIZE, 4);
				//handler.sendEmptyMessage(START_CAMERA);
				if(intent.hasExtra("dx")){
					if(mCamera!=null){
						System.out.println("guosong mcamera!=null");
						Parameters params = mCamera.getParameters();
						params.setPreviewSize(320, 240);
						mCamera.setParameters(params);
						Log.i(TAG, "mCamera.setParameters");
					}else{
						System.out.println("guosong mcamera==null");
						key68=true;
						Log.i(TAG, "mCamera null");
					}
				}
				//+ by hcj @{
				if(intent.hasExtra("takepic_action")){
					Log.i(ConfigUtil.TAG_TAKEPIC,"takepic_action="+intent.getStringExtra("takepic_action"));
				}
				//+ by hcj @}
				photonum=photonum+Integer.parseInt(Utils.getProperty(BootCameraService.this,Utils.KEY_FTP_TAKINGMODEL));
				Log.i(TAG, "photonum1="+String.valueOf(photonum));
				Log.i(ConfigUtil.TAG_CONTINUES,"startWork by timer");
				startWork();
				
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
			}
		}
	};
	public void getIsoVaule(){
		//+ by hcj @{
		Log.i(ConfigUtil.TAG,"getIsoVaule FEATURE_DAY_NIGHT_CHECK_POLICY="+ConfigUtil.FEATURE_DAY_NIGHT_CHECK_POLICY);
		if(ConfigUtil.FEATURE_DAY_NIGHT_CHECK_POLICY == ConfigUtil.DAY_NIGHT_CHECK_ONLY_AUTOLIGHT_ON){
			String auto_lighte=Utils.getProperty(this, Utils.KEY_FTP_IMG_AUTO_LIGHTE);
			if("0".equals(auto_lighte)){
				Log.i(ConfigUtil.TAG,"getIsoVaule skip by auto light off!");
				setLight(LIGHT_OFF,this);
				DayNightModeSet(true,2);
				return;
			}
		}
		//+ by hcj @}
		String triggerModel = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
		if(/*"3".equals(triggerModel)*/true){//m by hcj
			
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
		}else{
			if(flagSeelp==0){
				setSleepfalg(1);
			}
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		flagDay(this);
		getmTimer();
	}
	int height = 1088;//Integer.parseInt(size[0]);
    int width = 1920;//Integer.parseInt(size[1]);
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
			if("1".equals(Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL))){
			params.set("zsd-mode", "off");}
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
				/*IO.cmd(2);
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
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
		    if("1".equals(Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL))){
				params.set("zsd-mode", "off");}
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
			if(isLightEnable(BootCameraService.this)){//+ by hcj
				IO.cmd(14);
				//IO.cmd(2);//- by hcj
				setLight(LIGHT_ON);
				//+ by hcj @{
				if(ConfigUtil.DBG_LIGHT){
					Log.i(ConfigUtil.TAG_LIGHT,"startCamera1 IO.cmd(2)");
				}
				//+ by hcj @}
			}
			//params.setExposureCompensation(ImgSize.getExposure(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_TIEM))));
			//params.setWhiteBalance(ImgSize.getWhiteBalance(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_WHITE_BALANCE))));
			mCamera.setParameters(params);
			mCamera.startPreview();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i(ConfigUtil.TAG_CONTINUES,"aaaaa startWork time="+System.currentTimeMillis());  
			mCamera.setOneShotPreviewCallback(BootCameraService.this);
			preview = true;
			key68=false;
			getmTimer();
			TakepictureTiem t=new TakepictureTiem(BootCameraService.this);
			t.setAlarmTime(Utils.DURING_DAY_MODEL,10,9);
		}
	}
	Timer mTimer;
	public void getmTimer()
	{
		//+ by hcj @{
		if(ConfigUtil.FEATURE_CAMERA_SLEEP_POLICY == ConfigUtil.CAMERA_SLEEP_NEVER){
			Log.i(ConfigUtil.TAG,"getmTimer skip by nerver sleep");
			return;
		}else if(ConfigUtil.FEATURE_CAMERA_SLEEP_POLICY == ConfigUtil.CAMERA_SLEEP_NIGHT_ONLY){
			if(flagDay) {
				Log.i(ConfigUtil.TAG,"getmTimer skip by day mode");
				return;
			}
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
				//+ by hcj @{
				if(ConfigUtil.DBG_LIGHT){
					Log.i(ConfigUtil.TAG_LIGHT,"getmTimer IO.cmd(3)");
				}
				//+ by hcj @}
				//if(!"3".equals(triggerModel)){
					//setSleepfalg(0);
				//}else{
				stopCamera("getmTimer");
				//}
				mTimer.cancel();
				mTimer=null;
				//handler.sendEmptyMessage(101);
			}
		}, ms, ms);
	}
	
	static Boolean flagDay=true;//true 白天 false 黑夜
	public static void flagDay(Context context){
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
			String exptime=SystemProperties.get("camera.exptime.value");
			int getSetExp=ImgSize.getExposure(Integer.parseInt(Utils.getProperty(context, Utils.KEY_FTP_IMG_TIEM)));
			Boolean curDay=true;
			Log.i(ConfigUtil.TAG_LIGHT,"flagDay iso_value="+iso_value+",ftp_exptime="+getSetExp+",real_exptime="+exptime);//+ by hcj
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
		//System.out.println("guosong setDev===="+cmd);
        try {
            Process exeEcho = Runtime.getRuntime().exec("sh");
            exeEcho.getOutputStream().write(cmd.getBytes());
            exeEcho.getOutputStream().flush();
        } catch (IOException e) {
        	//e.printStackTrace();
        	Log.i(ConfigUtil.TAG,"setDev e="+e);
        }
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
  		 			        	startCamera(mSurfaceHolder,true);
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
  				if(photonum>0){
  					Log.i(TAG, "photonum>0");
					Log.i(ConfigUtil.TAG_CONTINUES,"startWork by continues");
  					startWork();
  				}
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
					System.out.println("guo test time < 2016 is 2010");
				}
				break;
  			case 101:
  				System.out.println("key4================");
  				Intent intent = new Intent();  
	        	intent.setAction("com.gs.camera");
	        	sendBroadcast(intent);
	        	Log.i(TAG, "com.gs.camera");
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
  				break;
  			case 102:
  				System.out.println("guosong startWork"+msg.what);
				Log.i(ConfigUtil.TAG_CONTINUES,"startWork by handleMessage 102");
  				startWork();
  				break;
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
  		Log.i(TAG, "pho");
		try {
			FileOutputStream outStream = new FileOutputStream(file);
			YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
			if (image != null) {
				Log.i(TAG, "image !null");
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
				BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
				stream.writeTo(outStream);
				stream.close();
				preview = true;
				Log.i(TAG, "photonum3="+String.valueOf(photonum));
				if(photonum==0){
					Log.i(TAG, "photonum == 0");
					System.out.println("key3================");
	        	//Intent intent = new Intent();  
	        	//intent.setAction(Utils.CAMERA_UPLOAD_SERVICE_ACTION);
	        	//intent.putExtra(Utils.INTENT_CAMERA_NAME_JPG, jpgName);
	        	//intent.putExtra(Utils.INTENT_CAMERA_PATH, path);
	        	//sendBroadcast(intent);
        		//System.gc();
        		//System.runFinalization();
				handler.sendEmptyMessage(101);
				}
			} else {
				Log.i(TAG, "image null");
			}
		} catch (Exception ex) {
			Log.e("Sys", "Error:" + ex.getMessage());
			Log.i(TAG, "image Exception");
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
  	//private boolean mStartWork = false;
	
  	private static final int LIGHT_ON = 2;
	private static final int LIGHT_OFF = 3;
  	private static void setLight(int state, Context context){
		if(!"3".equals(Utils.getProperty(context,Utils.KEY_FTP_TRIGGERMODEL))){
			Log.i(ConfigUtil.TAG_LIGHT,"setLight skip by trigger mode non timer");
			return;
		}else if(flagDay && (state == LIGHT_ON)){
			Log.i(ConfigUtil.TAG_LIGHT,"setLight skip by Day mode");
			return;
		}
		Log.i(ConfigUtil.TAG_LIGHT,"setLight state="+state);
		IO.cmd(state);
  	}

	private void setLight(int state){
		setLight(state,this);
	}

	private static void setLightEnable(boolean enable){
		int state = enable ? 14 : 15;
		Log.i(ConfigUtil.TAG_LIGHT,"setLightEnable state="+state);
		IO.cmd(state);
	}

	private boolean isLightEnable(Context context){
		  String auto_lighte=Utils.getProperty(context, Utils.KEY_FTP_IMG_AUTO_LIGHTE);
		  boolean enable = (!"0".equals(auto_lighte) && !flagDay);
		  Log.i(ConfigUtil.TAG_LIGHT,"isLightEnable enable="+enable);
		  return enable;
	}

	private void DayNightModeInit(){
		flagDay = false;
		DayNightModeSet(true,2);
	}

	private static void DayNightModeSet(boolean isDay, int configExp){
		Log.i(ConfigUtil.TAG_LIGHT,"DayNightModeSet isDay="+isDay);
		if(isDay == flagDay){
			Log.i(ConfigUtil.TAG_LIGHT,"DayNightModeSet skip by same mode");
			return;
		}
		flagDay = isDay;
		if(isDay){
			setLightEnable(false);
			setDev(2);
		}else{
			if(!ConfigUtil.FEATURE_DISABLE_TAKEPIC_ON_NIGHT){
				setLightEnable(true);
			}else{
				Log.i(ConfigUtil.TAG_LIGHT,"DayNightModeSet night but take picture disabled");
			}
			setDev(configExp);
		}
	}

	private static void DayNightModeCheck(Context context, int isoValue, int configExp, int realExp){
		boolean isDay = true;
		
		if(configExp != 2 && configExp < realExp){
			Log.i(ConfigUtil.TAG_LIGHT,"Night mode by configExp != 2 && configExp< realExp");
			isDay = false;
		}else if(configExp == 2){
			isDay = flagDay;
		}else if(isoValue <= 200){
			if(configExp < realExp){
				Log.i(ConfigUtil.TAG_LIGHT,"Night mode by iso_value <= 200 && configExp < realExp");
				isDay = false;
			}else{
				Log.i(ConfigUtil.TAG_LIGHT,"Day mode by iso_value <= 200 && configExp>=realExp");
				isDay = true;
			}
		}else{
			Log.i(ConfigUtil.TAG_LIGHT,"Day mode by iso_value > 200");//+ by hcj
			isDay = true;
		}
		DayNightModeSet(isDay,configExp);
	}
  	//+ by hcj @}
}
