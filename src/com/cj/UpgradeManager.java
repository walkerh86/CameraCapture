package com.cj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPFile;

import com.camera.setting.ftp.FTP;
import com.camera.setting.ftp.FTP.DownLoadProgressListener;
import com.camera.setting.ftp.FTP.UploadProgressListener;
import com.camera.setting.servics.BootCameraService;
import com.camera.setting.utils.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;
import android.content.pm.IPackageInstallObserver;
import java.security.MessageDigest;

public class UpgradeManager implements Runnable{
	private int mLocalVerCode = 0;
	private FTP mFtpClient;
	private String mDeviceId;
	private PackageManager mPackageManager;
	private boolean mIsUpgrading;
	private int mTargetVerCode;
	private File mUpgradeFile;
	private Context mContext;
	private String mUpgradeMd5Value;
	private String mUpgradeApkName;
	private static final int UPGRADE_RET_OK = 0;
	private static final int UPGRADE_RET_FAIL_MD5 = 1;
	private static final int UPGRADE_RET_FAIL_INSTALL = 2;
	private boolean mApkUpgradeChecked;
	private static final String BIN_PREFIX = "CaptureCamera_";
	private boolean mUpgradeRetSuccess;

	@Override
	public void run(){
		while (true) {
			if(!mApkUpgradeChecked){
				mApkUpgradeChecked = true;
				apkCheckUpgradeIsOk();
			}
			apkCheckUpgrade();
			try {
				Thread.sleep(1000*60*5);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public UpgradeManager(Context context){
		mContext = context;
		mPackageManager = context.getPackageManager();
		try {
			PackageInfo pi = mPackageManager.getPackageInfo("com.camera.setting", PackageManager.GET_UNINSTALLED_PACKAGES);
			if (pi != null) {
				mLocalVerCode = pi.versionCode;
			}
		} catch (NameNotFoundException e) {
			Log.i(ConfigUtil.TAG_UPGRADE,"UpgradeManager e="+e);
		}

		mFtpClient = new FTP(context);
		mFtpClient.ftpClient.setConnectTimeout(ConfigUtil.SRV_CONNECT_TIMEOUT);
		mDeviceId = SystemProperties.get(Utils.SYSTEM_KEY_DEIVCES_ID);
		Log.i(ConfigUtil.TAG_UPGRADE,"UpgradeManager mLocalVerCode="+mLocalVerCode+",mDeviceId="+mDeviceId);

		checkAndDelApk();
	}

	private void apkCheckUpgradeIsOk(){
		SharedPreferences preference = mContext.getSharedPreferences(Utils.XML_INFO, Context.MODE_PRIVATE);
		int installVerCode = preference.getInt("key_apk_install_vercode",-1);
		Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgradeIsOk installVerCode="+installVerCode);
		if(installVerCode == -1){
			return;
		}
		if(installVerCode == mLocalVerCode){
			apkSetResult(mLocalVerCode,UPGRADE_RET_OK, 0);
		}
		Editor editor = preference.edit();
		editor.putInt("key_apk_install_vercode",-1);
		editor.apply();
	}

	private void checkAndDelApk(){
		File updateDir = new File("//sdcard/Download/");
		if(updateDir.exists()){
			File[] subFiles = updateDir.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir,String name){
					Log.i(ConfigUtil.TAG_UPGRADE, "checkAndDelApk name="+name);
					return name.endsWith("apk");
				}
			});
			if(subFiles != null && subFiles.length > 0){
				for(File subFile : subFiles){
					subFile.delete();
				}
			}
		}
	}

	public void apkCheckUpgrade(){
		Log.i(ConfigUtil.TAG_UPGRADE, "apkCheckUpgrade mIsUpgrading="+mIsUpgrading);
		if(mIsUpgrading) return;

		mUpgradeMd5Value = null;
		mUpgradeApkName = null;
		
		String remotePath = mDeviceId+"/updata";
		FTPFile[] files = null;
		try{
			mFtpClient.openConnect();
			files = mFtpClient.listFiles(remotePath);
			mFtpClient.closeConnect();
		}catch(IOException e){
			Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade e="+e);
		}
		if(files != null && files.length > 0){
			for(FTPFile file : files){
				Log.i(ConfigUtil.TAG_UPGRADE,"update dir file:"+file.getName());
				String name = file.getName();
				if(name.startsWith(BIN_PREFIX) && name.endsWith(".bin")){
					mUpgradeApkName = name;
					break;
				}
			}
		}
		Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade apkName="+mUpgradeApkName);
		boolean doUpgrade = false;
		if(mUpgradeApkName != null){
			//get md5 value
			/*
			String md5Prefix = mUpgradeApkName.replace("apk","md5")+".";
			String md5Name = null;
			for(FTPFile file : files){
				String name = file.getName();
				if(name.startsWith(md5Prefix)){
					md5Name = name;
					mUpgradeMd5Value = name.substring(md5Prefix.length());
					break;
				}
			}
			*/
			String nameNoPrefix = mUpgradeApkName.substring(BIN_PREFIX.length());
			int seperatorIdx = nameNoPrefix.indexOf("_");
			int md5BeginIdx = seperatorIdx+1;
			int md5EndIdx = nameNoPrefix.lastIndexOf(".");
			if(md5EndIdx > md5BeginIdx){
				mUpgradeMd5Value = nameNoPrefix.substring(md5BeginIdx,md5EndIdx);
			}
			if(mUpgradeMd5Value == null || mUpgradeMd5Value.length() < 1){
				Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade remote md5 invalid:"+mUpgradeMd5Value);
				return;
			}
			//get apk version
			/*
			int verCodeBeginIdx = mUpgradeApkName.lastIndexOf("_");
			if(verCodeBeginIdx > -1){
				verCodeBeginIdx++;
			}
			int verCodeEndIdx = mUpgradeApkName.lastIndexOf(".");
			*/
			int verCodeBeginIdx = 0;
			int verCodeEndIdx = seperatorIdx;
			if(verCodeEndIdx > verCodeBeginIdx){
				//should catch exception here
				try{
					int remoteVerCode = Integer.valueOf(nameNoPrefix.substring(verCodeBeginIdx,verCodeEndIdx));
					Log.i(ConfigUtil.TAG_UPGRADE,"remoteVerCode="+remoteVerCode);
					if(remoteVerCode > mLocalVerCode){
						doUpgrade = true;
						mTargetVerCode = remoteVerCode;
					}
				}catch(Exception e){
					Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade e="+e);
				}
			}else{
				Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade remote apk name invalid:"+mUpgradeApkName);
			}
		}
		Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade doUpgrade="+doUpgrade);
		if(doUpgrade){
			mIsUpgrading =  true;
			Log.i(ConfigUtil.TAG_UPGRADE, "apkCheckUpgrade mIsUpgrading true");
			try{
				String localApkName = mUpgradeApkName.replace(".bin",".apk");
				mFtpClient.downloadSingleFile(remotePath+"/"+mUpgradeApkName,"//sdcard/Download/",localApkName,new DownLoadProgressListener(){
					@Override
					public void onDownLoadProgress(String currentStep,long downProcess, File file) {
						if (currentStep.equals(Utils.FTP_DOWN_SUCCESS)) {
							if(apkCheckMd5(file)){
								apkInstall(file);
							}else{
								mIsUpgrading =  false;
								apkSetResult(UPGRADE_RET_FAIL_MD5,0);
								Log.i(ConfigUtil.TAG_UPGRADE, "apkCheckUpgrade mIsUpgrading false by check md5 fail");
							}
						}else if(currentStep.equals(Utils.FTP_DOWN_FAIL)){
							mIsUpgrading =  false;
							Log.i(ConfigUtil.TAG_UPGRADE, "apkCheckUpgrade mIsUpgrading false by onDownLoadProgress FTP_DOWN_FAIL");
						}else if(currentStep.equals(Utils.FTP_FILE_NOTEXISTS)){
							mIsUpgrading =  false;
							Log.i(ConfigUtil.TAG_UPGRADE, "apkCheckUpgrade mIsUpgrading false by onDownLoadProgress FTP_FILE_NOTEXISTS");
						}
					}
				});
			}catch(Exception e){
				Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade e="+e);
				mIsUpgrading =  false;
				Log.i(ConfigUtil.TAG_UPGRADE, "apkCheckUpgrade mIsUpgrading false by onDownLoadProgress Exception");
			}
		}
	}

	private void apkDownload(){
	}

	private boolean apkCheckMd5(File file){
		Log.i(ConfigUtil.TAG_UPGRADE, "apkCheckMd5...");
		MessageDigest digest = null;
 		FileInputStream in = null;
		byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			in.close();
		} catch (Exception e) {
			Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckMd5 e="+e);
			return false;
		}
		String md5Value = md5Hex(digest.digest());
		Log.i(ConfigUtil.TAG_UPGRADE, "md5Value="+md5Value);
		Log.i(ConfigUtil.TAG_UPGRADE, "mUpgradeMd5Value="+mUpgradeMd5Value);
		if(mUpgradeMd5Value.equalsIgnoreCase(md5Value)){
			return true;
		}
  		return false;
	}

	private void apkInstall(File file){
		Log.i(ConfigUtil.TAG_UPGRADE, "apkInstall...");
		SharedPreferences preference = mContext.getSharedPreferences(Utils.XML_INFO, Context.MODE_PRIVATE);
		Editor editor = preference.edit();
		editor.putInt("key_apk_install_vercode",mTargetVerCode);
		editor.apply();
		
		mUpgradeFile = file;
		Uri packageUri = Uri.fromFile(file);
		MyPackageInstallObserver observer = new MyPackageInstallObserver();  
		mPackageManager.installPackage(packageUri, observer, PackageManager.INSTALL_REPLACE_EXISTING, "com.camera.setting");
	}

	private void apkSetResult(int mainCode, int subCode){
		apkSetResult(mTargetVerCode,mainCode,subCode);
	}

	private void apkSetResult(int verCode, int mainCode, int subCode){
		Log.i(ConfigUtil.TAG_UPGRADE, "apkSetResult mainCode="+mainCode+",subCode="+subCode);
		String retName = null;
		if(mainCode == 0){
			retName = BIN_PREFIX+verCode+".ret.ok";
		}else{
			retName = BIN_PREFIX+verCode+".ret."+mainCode+"."+subCode;
		}
		String localRetName = "//sdcard/Download/"+retName;
		String remoteRetName = mDeviceId+"/updata/"+retName;
		
		try {
			File localRetFile = new File(localRetName);
			localRetFile.createNewFile();
			/*
			if(mLoopHander == null){
				mLoopHander = new LoopHander();
			}
			final UpgradeResultProcess resultProcess = new UpgradeResultProcess(5000,remoteRetName,localRetName);
			mLoopHander.addPorcess(resultProcess);
			*/
			mUpgradeRetSuccess = false;
			for(int i=0;i<3;i++){
				mFtpClient.uploadingSingle(remoteRetName,localRetName, new UploadProgressListener(){
					@Override
					public void onUploadProgress(String currentStep, long uploadSize, File file){
						if(currentStep.equals(Utils.FTP_UPLOAD_SUCCESS)){
							file.delete();
							mUpgradeRetSuccess = true;
						}else{
							Log.i(ConfigUtil.TAG_UPGRADE, "apkSetResult onUploadProgress fail reseaon="+currentStep);
						}
					}
				});
				if(mUpgradeRetSuccess){
					break;
				}else{
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			Log.i(ConfigUtil.TAG_UPGRADE, "apkSetResult e="+e);
		}
	}

	private class MyPackageInstallObserver extends IPackageInstallObserver.Stub{  
		@Override  
		public void packageInstalled(String packageName, int returnCode) {
			Log.i(ConfigUtil.TAG_UPGRADE, "packageInstalled code="+returnCode);
			UpgradeManager.this.mIsUpgrading =  false;
			if(UpgradeManager.this.mUpgradeFile != null){
				UpgradeManager.this.mUpgradeFile.delete();
			}
			if(PackageManager.INSTALL_SUCCEEDED == returnCode){
				UpgradeManager.this.mLocalVerCode = UpgradeManager.this.mTargetVerCode;
				apkSetResult(UPGRADE_RET_OK,0);
			}else{
				apkSetResult(UPGRADE_RET_FAIL_INSTALL,returnCode);
			}
		}
	}
	
	//md5 utils
	private static char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static String md5Hex(byte[] data) {
		int l = data.length;
		char[] out = new char[l<<1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i <  l; i++) {
			out[j++] = DIGITS_LOWER[(0xF0 & data[i])>>4];
			out[j++] = DIGITS_LOWER[0x0F & data[i]];
		}
		return new String(out);
	}

	public class UpgradeResultProcess extends LoopProcess{
		String mRemote;
		String mLocal;
		boolean mFinish;
		public UpgradeResultProcess(int period, String remoteRetName,String localRetName){
			super(period);
			mRemote = remoteRetName;
			mLocal = localRetName;
		}
		public boolean LoopRun(){
			mFinish = false;
			mFtpClient.uploadingSingle(mRemote,mLocal, new UploadProgressListener(){
				@Override
				public void onUploadProgress(String currentStep, long uploadSize, File file){
					if(currentStep.equals(Utils.FTP_UPLOAD_SUCCESS)){
						//file.delete();
						//mFinish = true;
					}else{
						Log.i(ConfigUtil.TAG_UPGRADE, "UpgradeResultProcess onUploadProgress step="+currentStep);
					}
				}
			});
			return mFinish;
		}
	}

	public interface LoopCallback{
		void onFinish(LoopProcess process);
		void onNext(LoopProcess process);
	}

	public class LoopProcess implements Runnable{
		private int mPeriod;
		private LoopCallback mCb;
		public LoopProcess(int period){
			mPeriod = period;
		}

		public void setCallback(LoopCallback cb){
			mCb = cb;
		}

		public boolean LoopRun(){
			return false;
		}

		public int getPeriod(){
			return mPeriod;
		}

		@Override
		public void run(){
			Log.i(ConfigUtil.TAG_UPGRADE,"LoopProcess run");
			if(LoopRun()){
				if(mCb != null){
					mCb.onFinish(this);
				}
			}else{
				if(mCb != null){
					mCb.onNext(this);
				}
			}
		}
	}
	private class LoopHander extends Handler implements LoopCallback{
		//SparseArray<LoopProcess> mProcesses = new SparseArray<LoopProcess>(2);
		//private Handler mHandler = new Handler();
		
		public LoopHander(){
		}

		public void addPorcess(LoopProcess process){
			//mProcesses.add(process);
			process.setCallback(this);
			/*mHandler*/this.postDelayed(process,process.getPeriod());
		}

		public void onFinish(LoopProcess process){
			Log.i(ConfigUtil.TAG_UPGRADE,"onFinish");
		}
		
		public void onNext(LoopProcess process){
			Log.i(ConfigUtil.TAG_UPGRADE,"onNext");
			/*mHandler*/this.postDelayed(process,process.getPeriod());
		}
	}
}
