package com.cj;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

public class NetworkUtils{
	public static void setNetworkOn(Context context){
		if(ConfigUtil.DBG_NETWORK_WIFI){
			return;
		}
		TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		boolean enabled = getDataEnabled(telephonyManager);
		//Log.i(TAG, "--data enabled = "+enabled);
		if(!enabled){
			//Log.i(TAG, "--open data enabled ");
			setDataEnabled(telephonyManager);
		}
	}

	public static boolean getDataEnabled(TelephonyManager telephonyManager){
		if(ConfigUtil.DBG_NETWORK_WIFI){
			return true;
		}
		return telephonyManager.getDataEnabled();
	}

	public static void setDataEnabled(TelephonyManager telephonyManager){
		if(ConfigUtil.DBG_NETWORK_WIFI){
			return;
		}
		telephonyManager.setDataEnabled(true);
	}
}