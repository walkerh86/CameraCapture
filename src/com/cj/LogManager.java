package com.cj;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import android.util.Log;

public class LogManager{
	private static final String TAG = "LogManager";
	private static LogManager mLogManager;
	private static final String LOG_DIR = "//sdcard/log_work/";
	public static final SimpleDateFormat mSdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	static{
		mSdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
	}
	private String mLogFilePath;
	
	private LogManager(){
		//mSdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		//mSdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
		File logDir = new File(LOG_DIR);
		if(!logDir.exists()){
			logDir.mkdirs();
		}
		mLogFilePath = LOG_DIR + mSdf.format(new Date())+".txt";
	}

	public static LogManager getInstance(){
		if(mLogManager == null){
			mLogManager = new LogManager();
		}
		return mLogManager;
	}

	public void Log(String log){
		try {   
			FileWriter writer = new FileWriter(mLogFilePath, true);
			String tag = mSdf.format(new Date())+" ";
			writer.write(tag);
			writer.write(log);
			writer.write("\n");
			writer.close();   
		} catch (IOException e) {   
			Log.i(TAG,"Log e="+e);   
		}   
	}
}
