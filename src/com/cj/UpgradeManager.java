package com.cj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigInteger;

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

	@Override
	public void run(){
		while (true) {
			if(!mApkUpgradeChecked){
				mApkUpgradeChecked = true;
				apkCheckUpgradeIsOk();
			}
			apkCheckUpgrade();
			try {
				Thread.sleep(1000*20);
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
				if(name.endsWith("apk")){
					mUpgradeApkName = name;
					break;
				}
			}
		}
		Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade apkName="+mUpgradeApkName);
		boolean doUpgrade = false;
		if(mUpgradeApkName != null){
			//get md5 value
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
			if(mUpgradeMd5Value == null || mUpgradeMd5Value.length() < 1){
				Log.i(ConfigUtil.TAG_UPGRADE,"apkCheckUpgrade remote md5 invalid:"+md5Name);
				return;
			}
			//get apk version
			int verCodeBeginIdx = mUpgradeApkName.lastIndexOf("_");
			if(verCodeBeginIdx > -1){
				verCodeBeginIdx++;
			}
			int verCodeEndIdx = mUpgradeApkName.lastIndexOf(".");
			if(verCodeEndIdx > verCodeBeginIdx){
				//should catch exception here
				int remoteVerCode = Integer.valueOf(mUpgradeApkName.substring(verCodeBeginIdx,verCodeEndIdx));
				Log.i(ConfigUtil.TAG_UPGRADE,"remoteVerCode="+remoteVerCode);
				if(remoteVerCode > mLocalVerCode){
					doUpgrade = true;
					mTargetVerCode = remoteVerCode;
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
				mFtpClient.downloadSingleFile(remotePath+"/"+mUpgradeApkName,"//sdcard/Download/",mUpgradeApkName,new DownLoadProgressListener(){
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
							Log.i(ConfigUtil.TAG_UPGRADE, "apkCheckUpgrade mIsUpgrading false by onDownLoadProgress fail");
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
  		BigInteger bigInt = new BigInteger(1, digest.digest());
		String md5Value = bigInt.toString(16);
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
			retName = "ver_"+verCode+".ret.ok";
		}else{
			retName = "ver_"+verCode+".ret."+mainCode+"."+subCode;
		}
		String localRetName = "//sdcard/Download/"+retName;
		String remoteRetName = mDeviceId+"/updata/"+retName;
		
		try {
			File localRetFile = new File(localRetName);
			localRetFile.createNewFile();
			
			mFtpClient.uploadingSingle(remoteRetName,localRetName, new UploadProgressListener(){
				@Override
				public void onUploadProgress(String currentStep, long uploadSize, File file){
					if(currentStep.equals(Utils.FTP_UPLOAD_SUCCESS)){
						file.delete();
					}
				}
			});
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
				apkSetResult(UPGRADE_RET_FAIL_MD5,UPGRADE_RET_FAIL_INSTALL);
			}
		}
	}
}
