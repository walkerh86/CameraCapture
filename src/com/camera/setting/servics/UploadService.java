package com.camera.setting.servics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.net.ftp.FTPFile;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.camera.setting.ftp.FTP;
import com.camera.setting.ftp.FTP.DownLoadProgressListener;
import com.camera.setting.ftp.FTP.UploadProgressListener;
import com.camera.setting.receiver.BootBroadcastReceiver;
import com.camera.setting.utils.Utils;

//+ by hcj @{
import com.cj.NetworkUtils;
import com.cj.ConfigUtil;
//+ by hcj @}

public class UploadService extends Service{
	private static final String TAG = UploadService.class.getSimpleName();
	//private static final String TAG = "FTP";
	private TelephonyManager telephonyManager ;
	private ExecutorService uploadExecutor = Executors.newFixedThreadPool(1);//同时允许有三帧在保存
	private ExecutorService uploadFileExecutor = Executors.newFixedThreadPool(1);//同时允许有三帧在保存
	private FTP ftpClient;
	private static boolean isCreateFloat = true;
	private String imgName;
	private String imgPaht;
    private static IntentFilter myFilter = null;
    
    public static boolean updataApkFlag=false;
    
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "--onCreate");
		if(isCreateFloat){
			ftpClient = new FTP(UploadService.this);
    		isCreateFloat = false;
    	}
		// 注册这两个广�?	
		if(myFilter == null){
			myFilter = new IntentFilter();
			myFilter.addAction(Utils.CAMERA_UPLOAD_SERVICE_ACTION);
			myFilter.addAction(Utils.CAMERA_UPLOAD_FILE_SERVICE_ACTION);
			myFilter.addAction("com.gs.camera");
			myFilter.addAction("com.gs.updateftpXml");
			myFilter.addAction("android.intent.action.updatexml");
			this.registerReceiver(myBroadCast, myFilter);
		}
	}

	/* 创建广播接收�?*/
	public BroadcastReceiver myBroadCast = new BroadcastReceiver()
	{
		public void onReceive(Context context, Intent intent)
		{

			String action = intent.getAction();
			/*
			 * 如果捕捉到的action是ACTION_BATTERY_CHANGED�?就运行onBatteryInfoReceiver()
			 */
			System.out.println("this action++++++++++++++++" + action);
			telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			//boolean enabled = telephonyManager.getDataEnabled();//- by hcj
			boolean enabled = NetworkUtils.getDataEnabled(telephonyManager);//+ by hcj
			Log.i(TAG, "--data enabled = "+enabled);
			final String deivcesId = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
			if(null !=intent && intent.getAction().equals(Utils.CAMERA_UPLOAD_SERVICE_ACTION)){
				final String imgname=intent.getStringExtra(Utils.INTENT_CAMERA_NAME_JPG);
				final String imgpath=intent.getStringExtra(Utils.INTENT_CAMERA_PATH);
				final boolean uploadXml = intent.getBooleanExtra(Utils.INTENT_UPLOAD_XML,false);
				Log.i(TAG, "--path:"+getImgPaht()+"\n name:"+getImgName());
				if(!enabled){
					Log.i(TAG, "--open data enabled ");
					//telephonyManager.setDataEnabled(true);//- by hcj
					NetworkUtils.setDataEnabled(telephonyManager);//+ by hcj
					Log.i(TAG, "--start sleep for open network");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.e(TAG, "sleep 10s failed" ,e);
					} 
				}
				
				new Thread(){
					public void run() {
						startUpload(deivcesId,uploadXml,imgname,imgpath);
					};
				}.start();
				if(!"state.xml".equals(imgname))
				upLoadOther();
				Log.i(TAG,action+"/upLoadOther\n");
			}
			else if(null !=intent && intent.getAction().equals(Utils.CAMERA_UPLOAD_FILE_SERVICE_ACTION)){
				if(!enabled){
					Log.i(TAG, "--open data enabled ");
					//telephonyManager.setDataEnabled(true);//- by hcj
					NetworkUtils.setDataEnabled(telephonyManager);//+ by hcj
					Log.i(TAG, "--start sleep for open network");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.e(TAG, "sleep 10s failed" ,e);
					} 
				}
				startFileUpload(deivcesId);
				Log.i(TAG,action+"/startFileUpload\n");
			}else if("com.gs.camera".equals(intent.getAction())){

				new Thread(){
					public void run() {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						getmTimer();
						upLoadOther();
					};
				}.start();
				Log.i(TAG,action+"/upLoadOther\n");
				
			}else if("com.gs.updateftpXml".equals(intent.getAction())){
								
				new Thread(){
					public void run() {
						ftpClient = new FTP(UploadService.this);
						startUpload(deivcesId,true,"state.xml",Utils.XML_PATH);
					};
				}.start();
				Log.i(TAG,action+"/startUpload\n");
			}
			else if("android.intent.action.updatexml".equals(action)){
				Log.i(TAG,action+"\n");
			}
		}
	};
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "--onStartCommand");
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		//boolean enabled = telephonyManager.getDataEnabled();//- by hcj
		boolean enabled = NetworkUtils.getDataEnabled(telephonyManager);//+ by hcj
		Log.i(TAG, "--data enabled = "+enabled);
		final String deivcesId = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
		if(null !=intent && intent.getAction().equals(Utils.CAMERA_UPLOAD_SERVICE_ACTION)){
			final String imgname=intent.getStringExtra(Utils.INTENT_CAMERA_NAME_JPG);
			final String imgpath=intent.getStringExtra(Utils.INTENT_CAMERA_PATH);
			final boolean uploadXml = intent.getBooleanExtra(Utils.INTENT_UPLOAD_XML,false);
			Log.i(TAG, "--path:"+getImgPaht()+"\n name:"+getImgName());
			if(!enabled){
				Log.i(TAG, "--open data enabled ");
				//telephonyManager.setDataEnabled(true);//- by hcj
				NetworkUtils.setDataEnabled(telephonyManager);//+ by hcj
				Log.i(TAG, "--start sleep for open network");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e(TAG, "sleep 10s failed" ,e);
				} 
			}
			new Thread(){
				public void run() {
					startUpload(deivcesId,uploadXml,imgname,imgpath);
				};
			}.start();
			if(!"state.xml".equals(imgname))
			upLoadOther();
		}
		if(null !=intent && intent.getAction().equals(Utils.CAMERA_UPLOAD_FILE_SERVICE_ACTION)){
			if(!enabled){
				Log.i(TAG, "--open data enabled ");
				//telephonyManager.setDataEnabled(true);//- by hcj
				NetworkUtils.setDataEnabled(telephonyManager);//+ by hcj
				Log.i(TAG, "--start sleep for open network");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e(TAG, "sleep 10s failed" ,e);
				} 
			}
			startFileUpload(deivcesId);
		}
		return super.onStartCommand(intent, flags, startId);
	}
	private void startUpload(final String deviceID,final boolean uploadXml,final String name,final String path) {
		
		uploadExecutor.execute(new Runnable() {	
			@Override
			public void run() {
				// 检查FTP服务是否连接
				if (ftpClient == null) {
					ftpClient = new FTP(UploadService.this);
				} 
				boolean mkdir = ftpClient.mkdir(deviceID);
				boolean mkupdtedir = ftpClient.mkdir(deviceID+"/updata");
				Log.i(TAG, "--mkdir "+ deviceID + " :"+ mkdir+":mkupdtedir="+mkupdtedir);
				ftpClient.uploadingSingle(deviceID+"/"+name,path, new UploadProgressListener() {
					@Override
					public void onUploadProgress(String currentStep,long downProcess,File file) {
						Log.i(TAG, "--"+ currentStep);
						if(uploadXml){
							if(currentStep.equals(Utils.FTP_UPLOAD_FAIL)){
								Intent uploadIntent = new Intent(); 
								uploadIntent.setAction(Utils.CAMERA_UPLOAD_SERVICE_ACTION);
								uploadIntent.putExtra(Utils.INTENT_CAMERA_NAME_JPG, "state.xml");
								uploadIntent.putExtra(Utils.INTENT_CAMERA_PATH, Utils.XML_PATH);
								uploadIntent.putExtra(Utils.INTENT_UPLOAD_XML, true);
								uploadIntent.setClass(UploadService.this, UploadService.class);
								startService(uploadIntent);
							}
						}
					}
				});
			}
		});
	
	}
	
	private void startUploadList(final String filePath,final String fileName) {
		final String deviceID=SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
		Log.i(TAG, "---startUpload deviceID: "+deviceID);

		uploadExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				// 检查FTP服务是否连接
				if (ftpClient == null) {
					ftpClient = new FTP(UploadService.this);
				} 
				boolean mkdir = ftpClient.mkdir(deviceID);
				Log.i(TAG, "---mkdir "+ deviceID + " :"+ mkdir+"name:"+fileName+"filePath"+filePath);
				ftpClient.uploadingSingle(deviceID+"/"+fileName,filePath, new UploadProgressListener() {
					@Override
					public void onUploadProgress(String currentStep,long downProcess,File file) {
						try{
						Log.i(TAG, "--"+ currentStep);
							if(currentStep.equals(Utils.FTP_UPLOAD_SUCCESS)){
								Utils.deleteFile(new File(filePath));
							} 								
						}catch(Exception e){
							e.printStackTrace();
							Utils.deleteFile(new File(filePath));
						}
					}
				});
			}
		});
		
	}
	long lastClickTime=0;
	public synchronized  boolean isFastClick() {
        long time = System.currentTimeMillis();   
        if ( time - lastClickTime < 500) { 
        	mTimer.cancel();
        	getmTimer();
            return true;   
        }   
        lastClickTime = time;   
        return false;   
    }
	Timer mTimer;
	public void getmTimer()
	{
		if (mTimer != null || "".equals(mTimer))
		{
			return;
		}
		mTimer = new Timer();
		int ms = 3000;
		mTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				upLoadOther();
			}
		}, ms, ms);
	}
	public void upLoadOther(){
		if (isFastClick()) {return;}
		final List<String> list=getPictures(Utils.IMG_PATH);
		int m = 0;
		for(int i=0;i<list.size();i++){
			String name=list.get(i).replace("/mnt/sdcard/DCIM/Camera/", "");
			System.out.println(imgName+"na---------me"+name);
			Log.i(TAG,"upLoadOther\n");
			if(!name.equals(imgName)){
				startUploadList(list.get(i),name);

				m++;
				if(m >= 10){
					Log.i(TAG,"upLoadOther m >= 10\n");
					break;
				}

			}
		}
		mTimer.cancel();
		mTimer = null;
	}
	public List<String> getPictures(final String strPath) { 
	    List<String> list = new ArrayList<String>(); 
	    File file = new File(strPath); 
	    File[] allfiles = file.listFiles(); 
	    if (allfiles == null) { 
	      return null; 
	    } 
	    for(int k = 0; k < allfiles.length; k++) { 
	      final File fi = allfiles[k]; 
	      if(fi.isFile()) { 
	              int idx = fi.getPath().lastIndexOf("."); 
	              if (idx <= 0) { 
	                  continue; 
	              } 
	              String suffix = fi.getPath().substring(idx); 
	              if (suffix.toLowerCase().equals(".jpg") || 
	                  suffix.toLowerCase().equals(".jpeg") || 
	                  suffix.toLowerCase().equals(".bmp") || 
	                  suffix.toLowerCase().equals(".png") || 
	                  suffix.toLowerCase().equals(".gif") ) { 
	                  list.add(fi.getPath()); 
	              } 
	      } 
	   } 
	   return list; 
	 }
	private void startFileUpload(final String deviceID) {
		uploadFileExecutor.execute(new Runnable() {
			@Override
			public void run() {
				// 检查FTP服务是否连接
				if (ftpClient == null) {
					ftpClient = new FTP(UploadService.this);
				} 
				boolean mkdir = ftpClient.mkdir(deviceID);
				Log.i(TAG, "--mkdir "+ deviceID + " :"+ mkdir);
				File file =new File(Utils.IMG_PATH);
				ArrayList<File> deleteFile = new ArrayList<File>();
		    	File []files = file.listFiles();
		    	//Log.i(TAG, "--uploadingSingle  files.length :"+files.length);
		    	if(files != null && files.length > 0){
		    		for (int i = 0; i < files.length; i++) {
		    			File child = files[i];
		    			try {
							ftpClient.uploadingSingle(child,deviceID+"/", null);
							deleteFile.add(child);
							Log.i(TAG, "--uploadingSingle:"+child.getName());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
		    	}
		    	for(File f:deleteFile){
		    		f.delete();
		    	}
		    	try {
					ftpClient.uploadAfterOperate(new UploadProgressListener() {
						public void onUploadProgress(String currentStep, long uploadSize, File file) {
							Log.i(TAG, "--onUploadProgress:"+currentStep);
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
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
