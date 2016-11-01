package com.cj;

public class ConfigUtil{
	public static final boolean DBG = true;
	public static final boolean DBG_NO_NET = DBG && false;
	public static final boolean DBG_NETWORK_WIFI = DBG && false;
	public static final boolean DBG_BROADCAST_TAKE_PIC = DBG && false;
	public static final boolean DBG_LIGHT = DBG && false;
	public static final boolean DBG_TIMER = DBG && false;
	public static final boolean DBG_ISO = DBG && true;

	public static final String TAG = "hcj";
	public static final String TAG_UPGRADE = "hcj.upgrade";
	public static final String TAG_TAKEPIC = "hcj.takepic";
	public static final String TAG_LIGHT = "hcj.light";
	public static final String TAG_TIMER = "hcj.timer";
	public static final String TAG_CONTINUES = "hcj.continues";

	public static final int CAMERA_SLEEP_ALWAYS = 0;
	public static final int CAMERA_SLEEP_NEVER = 1;
	public static final int CAMERA_SLEEP_NIGHT_ONLY = 2;
	public static final int FEATURE_CAMERA_SLEEP_POLICY = CAMERA_SLEEP_NIGHT_ONLY;

	public static final int DAY_NIGHT_CHECK_ONLY_AUTOLIGHT_ON = 0;
	public static final int DAY_NIGHT_CHECK_ALWAYS = 1;
	public static final int FEATURE_DAY_NIGHT_CHECK_POLICY = DAY_NIGHT_CHECK_ALWAYS;

	public static final boolean FEATURE_DISABLE_TAKEPIC_ON_NIGHT = false;

	public static final String CJ_ACTION_BOOT_COMPLETED = "com.cj.action.BOOT_COMPLETED";

	public static final int SRV_CONNECT_TIMEOUT = 3000;//ms
}
