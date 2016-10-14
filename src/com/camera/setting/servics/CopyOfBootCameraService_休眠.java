package com.camera.setting.servics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.net.io.Util;

import com.camera.setting.R;
import com.camera.setting.ftp.FTP;
import com.camera.setting.utils.ImgSize;
import com.camera.setting.utils.Utils;
import com.takeoff.MazeDomotics.AlytJni;
import com.zg.IO;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.Theme;
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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

public class CopyOfBootCameraService_休眠 extends Service implements PreviewCallback,SurfaceHolder.Callback{
	private static final String TAG = CopyOfBootCameraService_休眠.class.getSimpleName();
	
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
	
	private static final int START_CAMERA = 10001;
	private static final int SET_GPIO = 10002;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "--onCreate");
		if(isCreateFloat){
			ftpClient = new FTP(CopyOfBootCameraService_休眠.this);
    		createFloatView();
    		isCreateFloat = false;
    	}
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
	                  //  Message msg = new Message();
	                   // handler.sendEmptyMessage(START_CAMERA);
	            		
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
		                    mCamera.setOneShotPreviewCallback(CopyOfBootCameraService_休眠.this);
		                    photonum=photonum+Integer.parseInt(Utils.getProperty(CopyOfBootCameraService_休眠.this,Utils.KEY_FTP_TAKINGMODEL));
		                    getmTimer();
	            		}else{
	            			startCamera1();
	            		}
	                    
	                    System.out.println("------------key-------------------"+photonum);
	            	}
	            }
	        }
	    }.start();
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
		stopCamera();
	}

	@Override
	public void onPreviewFrame(byte[] arg0, Camera camera) {
		System.out.println("-------"+fileSize);
		IO.cmd(3);
		if (flag && photonum>0) {
			photonum--;
			if(photonum==0){
				IO.cmd(3);
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
			registerReceiver(receiver, filter);
			registed = true;
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
					IO.cmd(3);
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
		        		intent.setClass(CopyOfBootCameraService_休眠.this, UploadService.class);
		        		//startService(intent);
		        		sendBroadcast(intent);
		        		System.gc();
		        		System.runFinalization();
			        	
		        	}catch(Exception e){
		        		Log.e(TAG, "--save file exception",e);
		        	}
				}
            });
	        stopCamera();
        }  
    }; 
    
    private BroadcastReceiver receiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "--action:"+intent.getAction());
			if(intent.getAction().equals(Utils.CAMERA_TAKEPICTURE_ACTION)){
				triggerModel = Utils.getProperty(CopyOfBootCameraService_休眠.this,Utils.KEY_FTP_TRIGGERMODEL);
				takingModel = intent.getIntExtra(Utils.INTENT_TAKINGMODEL, 1);
				fileSize = intent.getIntExtra(Utils.INTENT_FILESIZE, 4);
				handler.sendEmptyMessage(START_CAMERA);
			}
		}
	};
	int height = 1088;//Integer.parseInt(size[0]);
    int width = 1920;//Integer.parseInt(size[1]);
	private void startCamera(SurfaceHolder holder,boolean mflag) {
		Log.i(TAG, "--startCamera");
		if(mCamera==null){
			mCamera = Camera.open();
			Parameters params = mCamera.getParameters();
			String[] size = ImgSize.getImgSize(fileSize);
			height = Integer.parseInt(size[0]);
		    width = Integer.parseInt(size[1]);
		    Log.i(TAG, "--width:+"+width+" height:"+height);
			params.setPictureSize(width, height);
			params.setPreviewSize(width, height);
			params.setPreviewFrameRate(30);
			//params.setBrightnessMode(value);
			//params.setb
			params.setExposureCompensation(ImgSize.getExposure(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_TIEM))));
			params.setWhiteBalance(ImgSize.getWhiteBalance(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_WHITE_BALANCE))));
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
				photonum=photonum+Integer.parseInt(Utils.getProperty(CopyOfBootCameraService_休眠.this,Utils.KEY_FTP_TAKINGMODEL));
			}
			getmTimer();
		}
	}
	private void startCamera1() {
		Log.i(TAG, "--startCamera");
		if(mCamera==null){
			mCamera = Camera.open();
			//IO.cmd(2);
			flag=true;
			photonum=photonum+Integer.parseInt(Utils.getProperty(CopyOfBootCameraService_休眠.this,Utils.KEY_FTP_TAKINGMODEL));
			Parameters params = mCamera.getParameters();
			String[] size = ImgSize.getImgSize(fileSize);
		    Log.i(TAG, "--width:+"+width+" height:"+height);
			params.setPictureSize(width, height);
			params.setPreviewSize(width, height);
			params.setPreviewFrameRate(30);
			params.setExposureCompensation(ImgSize.getExposure(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_TIEM))));
			params.setWhiteBalance(ImgSize.getWhiteBalance(Integer.parseInt(Utils.getProperty(this, Utils.KEY_FTP_IMG_WHITE_BALANCE))));
			mCamera.setParameters(params);
			mCamera.startPreview();
			mCamera.setOneShotPreviewCallback(CopyOfBootCameraService_休眠.this);
			preview = true;
			getmTimer();
		}
	}
	Timer mTimer;
	public void getmTimer()
	{
		if (mTimer != null || "".equals(mTimer))
		{
			mTimer.cancel();
			mTimer=null;
		}
		mTimer = new Timer();
		int ms = 15000;
		mTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				IO.cmd(3);
				stopCamera();
				mTimer.cancel();
				mTimer=null;
			}
		}, ms, ms);
	}
	private void stopCamera() {
		Log.i(TAG, "--stopCamera");
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
 
    int photonum=0;
	@SuppressLint("HandlerLeak") 
	@SuppressWarnings("deprecation")
  	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
  			switch (msg.what) {
  			case START_CAMERA:
  				System.out.println("------------key---ddd----------------");
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
  		 						IO.cmd(2);
  		 						photonum++;
  		 						//mCamera.takePicture(null, null, jpegCalback);
  		 						flag=true;
  		 					} catch (Exception e) {
  		 						Log.e(TAG, "--takePicture failed",e);
  		 					}
  		  					handler.sendEmptyMessageDelayed(START_CAMERA,10);
  		  				}else{
  		  					takingNum = 1;
  		  					handler.removeMessages(START_CAMERA);
  		  				}
  					};
  				}.start();
  				
  				break;
  			case SET_GPIO:
  				IO.cmd(3);
	            handler.removeMessages(SET_GPIO);
  				break;
  			case 100:
				final byte[] data = (byte[]) msg.obj;
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
				String imgName = sdf.format(new Date()) +"-"+triggerModel+"-"+takingModel+"-"+index+"-"+trigger584Model;
				final String jpgName = imgName + ".jpg";
				final String path = String.format(Utils.IMG_PATH+"%s.jpg",imgName);
				final File file = new File(path);
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				System.out.println("-----key="+path);
				new Thread(){
	        		public void run() {
	        			try {
	        				pho(data,file,path,jpgName);
	    				} catch (Exception e) {
	    					e.printStackTrace();
	    				}
	        		};
	        	}.start();
				break;
  			case 101:
  				System.out.println("key4================");
  				Intent intent = new Intent();  
	        	intent.setAction("com.gs.camera");
	        	sendBroadcast(intent);
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
		try {
			FileOutputStream outStream = new FileOutputStream(file);
			YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
			if (image != null) {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
				BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
				stream.writeTo(outStream);
				stream.close();
				preview = true;
				if(photonum==0){
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
			}
		} catch (Exception ex) {
			Log.e("Sys", "Error:" + ex.getMessage());
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
  	
  	

}
