package com.cj;

import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.UserHandle;
import android.telecom.Phone;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.camera.setting.utils.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.File;

public class StateRetManager{
	private static StateRetManager mStateRetManager;
	private Context mContext;
	private String mBootTime;
	private String mBatteryLevel;
	private SimpleDateFormat mSdf;
	//signal strength
	private SubscriptionInfo mSir;
	private Phone mPhone = null;
	private TelephonyManager mTelephonyManager;
	private List<SubscriptionInfo> mSelectableSubInfos;
	//
	private static final String LOG_DIR = "//sdcard/log/";
	private boolean mTimeSyncDone;
	
	private StateRetManager(Context context){
		mContext = context;

		//clear exist txt
		MiscUtils.deleteSubFiles(LOG_DIR);

		//
		mSdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		mSdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
		
		//battery
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		mContext.registerReceiver(mStateReceiver, filter);

		//signal strength
		mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		mTelephonyManager.listen(mPhoneStateListener,  PhoneStateListener.LISTEN_SIGNAL_STRENGTHS); 

		//storage
	}

	public static StateRetManager getInstance(Context context){
		if(mStateRetManager == null){
			Log.i(ConfigUtil.TAG,"StateRetManager getInstance context="+context);  
			mStateRetManager = new StateRetManager(context);
		}
		return mStateRetManager;
	}

	public void onTimeSyncDone(){
		mTimeSyncDone = true;
		//reboot time
		mBootTime = mSdf.format(new Date());
		Log.i(ConfigUtil.TAG,"StateRetManager onTimeSyncDone mBootTime="+mBootTime);  
	}

	public String genRetFile(){
		if(!VarCommon.getInstance().isTimeSyncDone()){
			Log.i(ConfigUtil.TAG,"genRetFile skip by !VarCommon.getInstance().isTimeSyncDone()");  
			return null;
		}
		StringBuilder sb = new StringBuilder();
		//get battery,84%
		sb.append("电量:");
		sb.append(mBatteryLevel);
		sb.append("\n\r");
	
		//get signal strength,-92Dbm8asu
		sb.append("信号:");
		sb.append(mSignalStengthStr);
		sb.append("\n\r");
		//get free storage
		sb.append("存储:");
		sb.append(getStorage());
		sb.append("\n\r");
		//get reboot time
		sb.append("最后一次重启:");
		sb.append(mBootTime);
		sb.append("\n\r");
		//get apk versions
		getApkVersions();
		sb.append("上次版本:");
		sb.append(mApkVerLast);
		sb.append("\n\r");
		sb.append("升级版本:");
		sb.append(mApkVerCurr);
		sb.append("\n\r");
		//get data usage,dummy
		sb.append("流量:");
		sb.append("\n\r");
		//

		File logDir = new File(LOG_DIR);
		if(!logDir.exists()){
			logDir.mkdirs();
		}
		String localPath = LOG_DIR+mSdf.format(new Date())+".txt";
		writeToTxt(localPath,sb.toString());
		return localPath;
	}

	public static boolean writeToTxt(String fileName, String content) {
		Log.i(ConfigUtil.TAG,"writeToTxt content="+content);  
		if(fileName == null || fileName.length() == 0 || content == null){
			return false;
		}
		try {  
			OutputStreamWriter write = null;  
			BufferedWriter out = null; 
			write = new OutputStreamWriter(new FileOutputStream(  fileName),Charset.forName("gbk"));//һ��Ҫʹ��gbk��ʽ  
			out = new BufferedWriter(write, 1024);  
			out.write(content);  
			out.flush();  
			out.close();  
		} catch (Exception e) {  
			Log.i(ConfigUtil.TAG,"writeToTxt e="+e);  
			return false;  
		}
		return true;  
	}  	

	//battery
	private BroadcastReceiver mStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				mBatteryLevel = getBatteryPercentage(intent);
				int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
				Log.i(ConfigUtil.TAG,"voltage="+voltage);
				if(mTimeSyncDone){
					LogManager.getInstance().Log("mBatteryLevel="+mBatteryLevel);
					LogManager.getInstance().Log("voltage="+voltage);
				}
			}
		}
	};

	private static String getBatteryPercentage(Intent batteryChangedIntent) {
		int level = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
		int scale = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
		return String.valueOf(level * 100 / scale) + "%";
	}

	//signal
	private String mSignalStengthStr;
	
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			int signalDbm = signalStrength.getDbm();
			int signalAsu = signalStrength.getAsuLevel();
			mSignalStengthStr = signalDbm+"Dbm "+signalAsu+"asu";
		}
	};

	//storage
	//private StatFs mStatFs;
	private String getStorage(){
		/*
		if(mStatFs == null){
			File extDir = Environment.getExternalStorageDirectory();
			mStatFs = new StatFs(extDir.getAbsolutePath());
		}
		long freeBytes = mStatFs.getFreeBytes();
		return freeBytes/1024f/1024f+"MByte";
		*/
		int imgNum = 0;
		File imgDir = new File(Utils.IMG_PATH);
		File[] imgFiles = imgDir.listFiles();
		if(imgFiles != null){
			imgNum = imgFiles.length;
		}
		return Integer.toString(imgNum);
	}
	
	//apk version
	private String mApkVerLast;
	private String mApkVerCurr;
	
	private void getApkVersions(){
		String ver = Utils.getProperty(mContext, "apk_init_ver");
		String currVer = getApkVerCurr();
		if(ver == null || ver.length() == 0){
			Utils.saveProperty(mContext, "apk_init_ver",currVer);
			
			mApkVerLast = currVer;
			mApkVerCurr = "";
		}else{
			mApkVerLast = ver;
			mApkVerCurr = mApkVerLast.equals(currVer) ? "" : currVer;
		}
	}
	
	private String getApkVerCurr(){
		String version = "";
		PackageManager mPackageManager = mContext.getPackageManager();
		try {
			PackageInfo pi = mPackageManager.getPackageInfo("com.camera.setting", PackageManager.GET_UNINSTALLED_PACKAGES);
			if (pi != null) {
				version = Integer.toString(pi.versionCode);
			}
		} catch (Exception e) {
			Log.i(ConfigUtil.TAG,"UpgradeManager e="+e);
		}
		return version;
	}
}
