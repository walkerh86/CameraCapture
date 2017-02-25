package com.cj;

public class ConfigUtil{
	public static final boolean DBG = false;
	public static final boolean DBG_NO_NET = DBG && false;
	public static final boolean DBG_NETWORK_WIFI = DBG && false;
	public static final boolean DBG_BROADCAST_TAKE_PIC = DBG && false;
	public static final boolean DBG_LIGHT = DBG && false;
	public static final boolean DBG_TIMER = DBG && false;
	public static final boolean DBG_ISO = DBG && false;
	public static final boolean DBG_DAYNIGHT = DBG && true;
	public static final boolean DBG_DAYNIGHT_ALARM = DBG && false;
	public static final boolean DBG_UPGRADE = DBG && true;

	public static final String TAG = "hcj";
	public static final String TAG_UPGRADE = "hcj.upgrade";
	public static final String TAG_TAKEPIC = "hcj.takepic";
	public static final String TAG_LIGHT = "hcj.light";
	public static final String TAG_TIMER = "hcj.timer";
	public static final String TAG_CONTINUES = "hcj.continues";
	public static final String TAG_DAYNIGHT = "hcj.daynight";
	public static final String TAG_POWER = "hcj.power";
	public static final String TAG_SMS = "hcj.sms";

	public static final int CAMERA_SLEEP_ALWAYS = 0;
	public static final int CAMERA_SLEEP_NEVER = 1;
	public static final int CAMERA_SLEEP_NIGHT_ONLY = 2;
	public static final int FEATURE_CAMERA_SLEEP_POLICY = CAMERA_SLEEP_NEVER;

	public static final int DAY_NIGHT_CHECK_ONLY_AUTOLIGHT_ON = 0;
	public static final int DAY_NIGHT_CHECK_ALWAYS = 1;
	public static final int FEATURE_DAY_NIGHT_CHECK_POLICY = DAY_NIGHT_CHECK_ALWAYS;
	
	public static final int FLASHLIGHT_CTRL_BY_DRV = 0;
	public static final int FLASHLIGHT_CTRL_BY_APP = 1;
	public static final int FEATURE_FLASHLIGHT_CTRL_POLICY = FLASHLIGHT_CTRL_BY_APP;

	public static final int UPLOAD_MODE_SAVE = 0;
	public static final int UPLOAD_MODE_REALTIME =1;
	public static final int UPLOAD_MODE_FULL = 2;

	public static final int DAY_NIGHT_ISO_THRESHOLD = 350;

	public static final boolean FEATURE_DISABLE_TAKEPIC_ON_NIGHT = false;
	public static final boolean FEATURE_SMS_PIC_SIZE_FIX = false;

	public static final int SRV_CONNECT_TIMEOUT =6000;//ms

	//delay time after set light on
	public static final int WORK_AFTER_LIGHT_ON_DELAY_MS = 15;
	//
	public static final int CAM_PARAM_PREVIEW_FPS = 15;

	//
	public static final String RET_STATE_REMOTE_DIR = "log";

	//upgrade
	public static final int UPGRADE_CHECK_PERIOD_MS = 1000*60*15;

	public static final int ISO_CHECK_PERIOD_MIN = 10;//10;

	public static final String BIN_PREFIX = "CaptureCamera_";
	public static final String BIN_SUFFIX = ".bin";
}
