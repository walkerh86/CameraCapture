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
import com.camera.setting.ftp.FTP.DeleteFileProgressListener;
import com.camera.setting.ftp.FTP.DownLoadProgressListener;
import com.camera.setting.ftp.FTP.UploadProgressListener;
import com.camera.setting.receiver.BootBroadcastReceiver;
import com.camera.setting.utils.Utils;

//+ by hcj @{
import com.cj.NetworkUtils;
import com.cj.ConfigUtil;
import com.cj.MiscUtils;
import com.cj.FileUtils;
import com.cj.LogManager;
import com.cj.VarCommon;
import com.cj.StateRetManager;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
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
		Log.i(ConfigUtil.TAG, "UploadService onCreate");
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
			//myFilter.addAction("com.cj.state_changed");
			this.registerReceiver(myBroadCast, myFilter);
		}

		//initUploadMode();
		StateRetManager retManager = StateRetManager.getInstance(this);
		//VarCommon var = VarCommon.getInstance();
		//var.setExecutor(uploadExecutor);
		mVarCommon.setOnUploadRequestListener(new VarCommon.OnUploadRequestListener(){
			@Override
			public void onUploadRequest(String localPath, String remoteDir, UploadProgressListener uploadListener){
				Log.i(ConfigUtil.TAG,"onUploadRequest localPath="+localPath+",remoteDir="+remoteDir);
				uploadExecutor.execute(new UploadRunnable(localPath,remoteDir,uploadListener));
			}

			@Override
			public void onDeleteRequest(String remoteDir, DeleteFileProgressListener deleteListener){
				uploadExecutor.execute(new DeleteRunnable(remoteDir,deleteListener));
			}
		});
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
			//System.out.println("this action++++++++++++++++" + action);
			Log.i(ConfigUtil.TAG,"UploadService onReceive action="+action);
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
					/*
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.e(TAG, "sleep 10s failed" ,e);
					} 
					*/
					MiscUtils.threadSleep(1000);
				}
				new Thread(){
					public void run() {
						startUpload(deivcesId,uploadXml,imgname,imgpath);
					};
				}.start();
				if(!"state.xml".equals(imgname))
				upLoadOther();
				Log.i(TAG,action+"/upLoadOther\n");
				//Log.i(ConfigUtil.TAG,"CAMERA_UPLOAD_SERVICE_ACTION mUploadQueue + "+Utils.XML_PATH);
				//mUploadQueue.add(Utils.XML_PATH);
			}
			else if(null !=intent && intent.getAction().equals(Utils.CAMERA_UPLOAD_FILE_SERVICE_ACTION)){
				if(!enabled){
					Log.i(TAG, "--open data enabled ");
					//telephonyManager.setDataEnabled(true);//- by hcj
					NetworkUtils.setDataEnabled(telephonyManager);//+ by hcj
					Log.i(TAG, "--start sleep for open network");
					/*
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.e(TAG, "sleep 10s failed" ,e);
					} 
					*/
					MiscUtils.threadSleep(1000);
				}
				startFileUpload(deivcesId);
				Log.i(TAG,action+"/startFileUpload\n");
			}else if("com.gs.camera".equals(intent.getAction())){
				//upload after take picture
				final String imgPath = (intent.hasExtra("imgPath")) ? intent.getStringExtra("imgPath") : null;
				//new Thread(){
					//public void run() {
						Log.i(ConfigUtil.TAG,"com.gs.camera imgPath="+imgPath+",mUploadMode="+mVarCommon.getUploadMode());
						if(mVarCommon.getUploadMode() == ConfigUtil.UPLOAD_MODE_SAVE || imgPath == null){
							//MiscUtils.threadSleep(1000);
							getmTimer();
							upLoadOther();
						}else{
							uploadImage(imgPath,null);
						}
					//};
				//}.start();
				Log.i(TAG,action+"/upLoadOther\n");
				/*
				if(intent.hasExtra("imgPath")){
					String imgPath = intent.getStringExtra("imgPath");
					Log.i(ConfigUtil.TAG,"com.gs.camera mUploadQueue + "+imgPath);
					mUploadQueue.add(imgPath);
				}else{
					Log.i(ConfigUtil.TAG,"com.gs.camera without imgPath");
				}
				if(mUploadMode == ConfigUtil.UPLOAD_MODE_SAVE){
					upLoadOther();
				}
				*/
			}else if("com.gs.updateftpXml".equals(intent.getAction())){
				new Thread(){
					public void run() {
						ftpClient = new FTP(UploadService.this);
						startUpload(deivcesId,true,"state.xml",Utils.XML_PATH);
					};
				}.start();
				Log.i(TAG,action+"/startUpload\n");
				/*
				ftpClient = new FTP(UploadService.this);//incase ftp url changed
				Log.i(ConfigUtil.TAG,"com.gs.updateftpXml mUploadQueue + "+Utils.XML_PATH);
				mUploadQueue.add(Utils.XML_PATH);
				*/
			}
			else if("android.intent.action.updatexml".equals(action)){
				Log.i(TAG,action+"\n");
			}
		//+ by hcj @{	
			else if("com.cj.state_changed".equals(action)){
				//Log.i(ConfigUtil.TAG, "com.cj.state_changed initUploadMode");
				//initUploadMode();
			}
		//+ by hcj @}	
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
				/*
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e(TAG, "sleep 10s failed" ,e);
				} 
				*/
				MiscUtils.threadSleep(1000);
			}
			new Thread(){
				public void run() {
					startUpload(deivcesId,uploadXml,imgname,imgpath);
				};
			}.start();
			if(!"state.xml".equals(imgname))
			upLoadOther();
			//Log.i(ConfigUtil.TAG,"CAMERA_UPLOAD_SERVICE_ACTION mUploadQueue + "+Utils.XML_PATH);
			//mUploadQueue.add(Utils.XML_PATH);
		}
		if(null !=intent && intent.getAction().equals(Utils.CAMERA_UPLOAD_FILE_SERVICE_ACTION)){
			if(!enabled){
				Log.i(TAG, "--open data enabled ");
				//telephonyManager.setDataEnabled(true);//- by hcj
				NetworkUtils.setDataEnabled(telephonyManager);//+ by hcj
				Log.i(TAG, "--start sleep for open network");
				/*
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e(TAG, "sleep 10s failed" ,e);
				} 
				*/
				MiscUtils.threadSleep(1000);
			}
			startFileUpload(deivcesId);
		}
//+ by hcj @{
		if(null !=intent && intent.getAction().equals("com.cj.init_upload")){
			Log.i(ConfigUtil.TAG,"com.cj.init_upload");
			uploadImage(null,Utils.IMG_PATH);
		}
//+ by hcj @}		
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
							//e.printStackTrace();
							LogManager.getInstance().Log("startUploadList e="+e);
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
		Log.i(ConfigUtil.TAG,"upLoadOther thread="+Thread.currentThread().getId());
		if (isFastClick()) {return;}
		final List<String> list=getPictures(Utils.IMG_PATH);
		int m = 0;
		for(int i=0;i<list.size();i++){
			String name=list.get(i).replace("/mnt/sdcard/DCIM/Camera/", "");
			System.out.println(imgName+"na---------me"+name);
			Log.i(TAG,"upLoadOther\n");
			if(!name.equals(imgName)){
				startUploadList(list.get(i),name);
				//+ by hcj @{
				//String imgPath = list.get(i);
				//Log.i(ConfigUtil.TAG,"upLoadOther mUploadQueue + "+imgPath);
				//mUploadQueue.add(imgPath);
				//+ by hcj @}

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
		Log.i(ConfigUtil.TAG,"startFileUpload");
		//upLoadOther();
		uploadFileExecutor.execute(new Runnable() {
			@Override
			public void run() {
				// 检查FTP服务是否连接
				if (ftpClient == null) {
					ftpClient = new FTP(UploadService.this);
				} 
				boolean mkdir = ftpClient.mkdir(deviceID);
				Log.i(ConfigUtil.TAG, "--mkdir "+ deviceID + " :"+ mkdir);
				File file =new File(Utils.IMG_PATH);
				ArrayList<File> deleteFile = new ArrayList<File>();
		    	File []files = file.listFiles();
		    	//Log.i(TAG, "--uploadingSingle  files.length :"+files.length);
		    	if(files != null && files.length > 0){
		    		for (int i = 0; i < files.length; i++) {
		    			File child = files[i];
		    			try {
							Log.i(ConfigUtil.TAG, "--uploadingSingle:"+child.getName());
							ftpClient.uploadingSingle(child,deviceID+"/", null);
							deleteFile.add(child);
							//Log.i(ConfigUtil.TAG, "--uploadingSingle:"+child.getName());
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
							Log.i(ConfigUtil.TAG, "--onUploadProgress:"+currentStep);
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

//+ by hcj @{
	private boolean mUploadImgSuccess;
	private void uploadImage(final String filePath, final String fileDir) {
		Log.i(ConfigUtil.TAG, "---uploadImage");
		uploadExecutor.execute(new Runnable() {
			@Override
			public void run() {
				Log.i(ConfigUtil.TAG, "---uploadImage run");
				mVarCommon.setImgUploading(true);

				if(filePath != null){
					uploadSingleImage(filePath);
				}
				if(fileDir != null){
					final List<String> imgList=getPictures(Utils.IMG_PATH);
					if(imgList != null){
						for(int i=0;i<imgList.size();i++){
							if(imgList.get(i).equals(filePath)){
								continue;
							}
							uploadSingleImage(imgList.get(i));
						}
					}
				}
				
				mVarCommon.setImgUploading(false);
				notifyImgUploadDone();
			}
		});
		
	}

	private void uploadSingleImage(String filePath){
		Log.i(ConfigUtil.TAG, "---uploadSingleImage filePath="+filePath);
		final String deviceID=SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
		
		int retryCount = 2;
		mUploadImgSuccess = false;
		String fileName = FileUtils.getFileName(filePath);
		
		if (ftpClient == null) {
			ftpClient = new FTP(UploadService.this);
		} 
		do{
			boolean mkdir = ftpClient.mkdir(deviceID);
			Log.i(ConfigUtil.TAG, "uploadSingleImage mkdir="+ mkdir +",filePath"+filePath);
			if(mkdir){
				ftpClient.uploadingSingle(deviceID+"/"+fileName,filePath, new UploadProgressListener() {
					@Override
					public void onUploadProgress(String currentStep,long downProcess,File file) {
						try{
							Log.i(ConfigUtil.TAG, "--"+ currentStep);
							if(currentStep.equals(Utils.FTP_UPLOAD_SUCCESS)){
								//Utils.deleteFile(new File(filePath));
								mUploadImgSuccess = true;
							} 								
						}catch(Exception e){
							//e.printStackTrace();
							//Utils.deleteFile(new File(filePath));
							Log.i(ConfigUtil.TAG, "uploadSingleImage uploadingSingle e="+ e);
							LogManager.getInstance().Log("uploadSingleImage e="+ e);
						}
					}
				});
			}
			if(mUploadImgSuccess){
				new File(filePath).delete();
				Log.i(ConfigUtil.TAG, "---uploadSingleImage success & delete file");
				break;
			}
			if(mVarCommon.getUploadMode() == ConfigUtil.UPLOAD_MODE_REALTIME){
				retryCount--;
				Log.i(ConfigUtil.TAG, "---uploadSingleImage fail retryCount="+retryCount);
				if(retryCount == 0){
					Log.i(ConfigUtil.TAG, "---uploadSingleImage fail twice & delete file");
					new File(filePath).delete();
					break;
				}
			}
			if(mVarCommon.getUploadMode() == ConfigUtil.UPLOAD_MODE_SAVE){
				Log.i(ConfigUtil.TAG, "---uploadSingleImage interupt by uploadMode change to save");
				break;
			}
			MiscUtils.threadSleep(2000);
		}while(true);
	}

/*
	private int mUploadMode;
	private String mUploadLastMode;
	private void initUploadMode(){
		//String triggerMode = Utils.getProperty(this,Utils.KEY_FTP_TRIGGERMODEL);
		String brightness = Utils.getProperty(this,Utils.KEY_FTP_IMG_BRIGHTNESS);
		int policy = ConfigUtil.UPLOAD_MODE_SAVE;
		boolean clearOlds = false;
		
		if("1".equals(brightness)){
			policy = ConfigUtil.UPLOAD_MODE_SAVE;
		}else if("2".equals(brightness)){
			policy = ConfigUtil.UPLOAD_MODE_REALTIME;
		}else if("3".equals(brightness)){
			policy = ConfigUtil.UPLOAD_MODE_FULL;
		}else if("4".equals(brightness)){
			policy = ConfigUtil.UPLOAD_MODE_SAVE;
			clearOlds = true;
		}else if("5".equals(brightness)){
			policy = ConfigUtil.UPLOAD_MODE_REALTIME;
			clearOlds = true;
		}else if("6".equals(brightness)){
			policy = ConfigUtil.UPLOAD_MODE_FULL;
			clearOlds = true;
		}else{
			Log.i(ConfigUtil.TAG,"initUploadMode policy invalide config compose: brightness="+brightness);
		}
		Log.i(ConfigUtil.TAG,"initUploadMode policy="+policy+",clearOlds="+clearOlds);
		if(clearOlds){
			if(!brightness.equals(mUploadLastMode)){
				MiscUtils.deleteSubFiles(Utils.IMG_PATH);
			}else{
				Log.i(ConfigUtil.TAG,"initUploadMode skip clearOlds by same mode");
			}
		}
		mUploadMode = policy;
		mUploadLastMode = brightness;
	}
*/
	private void notifyImgUploadDone(){
		sendBroadcast(new Intent("com.cj.img_upload.done"));
	}
	
	private VarCommon mVarCommon = VarCommon.getInstance();

	private class UploadRunnable implements Runnable{
		private String mLocalPath;
		private String mRemoteDir;
		private UploadProgressListener mListener;
		
		public UploadRunnable(String localPath, String remoteDir, UploadProgressListener uploadListener){
			mLocalPath = localPath;
			mRemoteDir = remoteDir;
			mListener = uploadListener;
		}

		@Override
		public void run(){
			Log.i(ConfigUtil.TAG, "UploadRunnable run mLocalPath="+mLocalPath);
			
			if (ftpClient == null) {
				ftpClient = new FTP(UploadService.this);
			} 

			//mk root dir
			String remotePath = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
			
			//mk sub dir
			if(mRemoteDir != null && mRemoteDir.length() > 0){
				remotePath += "/"+mRemoteDir;
				boolean mkdir = ftpClient.mkdir(remotePath);
				if(!mkdir){
					Log.i(ConfigUtil.TAG, "UploadRunnable mkdir "+remotePath+" fail!!!");
					return;
				}
			}
			String fileName = FileUtils.getFileName(mLocalPath);
			remotePath += "/"+fileName;
			Log.i(ConfigUtil.TAG, "UploadRunnable remotePath="+remotePath);
			
			ftpClient.uploadingSingle(remotePath, mLocalPath, mListener);
			Log.i(ConfigUtil.TAG, "UploadRunnable done");
		}
	}

	private class DeleteRunnable implements Runnable{
		private String mRemoteDir;
		private DeleteFileProgressListener mListener;
		
		public DeleteRunnable(String remoteDir, DeleteFileProgressListener uploadListener){
			mRemoteDir = remoteDir;
			mListener = uploadListener;
		}

		@Override
		public void run(){
			Log.i(ConfigUtil.TAG, "DeleteRunnable run mRemoteDir="+mRemoteDir);
			
			if (ftpClient == null) {
				ftpClient = new FTP(UploadService.this);
			} 

			//mk root dir
			String remotePath = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
			
			//mk sub dir
			remotePath += mRemoteDir;
			Log.i(ConfigUtil.TAG, "DeleteRunnable remotePath="+remotePath);
			
			ftpClient.deleteUpgradeFiles(remotePath, mListener);
			Log.i(ConfigUtil.TAG, "DeleteRunnable done");
		}
	}
//+ by hcj @}
}
