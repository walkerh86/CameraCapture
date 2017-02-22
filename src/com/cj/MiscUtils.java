package com.cj;

import android.util.Log;
import java.io.File;

public class MiscUtils{
	public static void threadSleep(int sleepMs){
		Log.i(ConfigUtil.TAG,"thread "+Thread.currentThread().getId()+" sleep "+sleepMs);
		try {
			Thread.sleep(sleepMs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static int strToInt(String str, int defValue){
		int ret = defValue;
		try{
			ret = Integer.parseInt(str);
		}catch(Exception e){
			Log.i(ConfigUtil.TAG,"strToInt e="+e);
		}
		return ret;
	}

	public static void deleteSubFiles(String dirPath){
		File file = new File(dirPath);
		File[] files = file.listFiles();
		if(files != null){
			for (File f : files) {
				f.delete();
			}
		}
	}
}
