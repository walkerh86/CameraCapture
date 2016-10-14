package com.camera.setting.utils;
public class ImgSize {
	    // size: height:240 width:320
		// size: height:480 width:640
		// size: height:768 width:1024
		// size: height:720 width:1280
		// size: height:768 width:1280
		// size: height:960 width:1280
		// size: height:1200 width:1600
		// size: height:1088 width:1920
		// size: height:1536 width:2048
		// size: height:1440 width:2560
		// size: height:1920 width:2560
		// size: height:2448 width:3264

		public static final String[] SIZE_240x320 =  { "240", "320" };
		public static final String[] SIZE_480x640 = { "480", "640" };
		public static final String[] SIZE_768x1024 = { "768", "1024" };
		public static final String[] SIZE_720x1280 = { "720", "1280" };
		public static final String[] SIZE_768x1280 = { "768", "1280" };
		public static final String[] SIZE_960x1280 = { "960", "1280" };
		public static final String[] SIZE_1200x1600 = { "1200", "1600" };
		public static final String[] SIZE_1088x1920 = { "1088", "1920" };
		public static final String[] SIZE_1536x2048 = { "1536", "2048" };
		public static final String[] SIZE_1440x2560 = { "1440", "2560" };
		public static final String[] SIZE_1920x2560 = { "1920", "2560" };
		public static final String[] SIZE_2448x3264 = { "2448", "3264" };


		public static String[] getImgSize(int size) {
			String[] value = new String[2];
			switch (size) {
			case 1:
				value = SIZE_240x320;
				break;
			case 2:
				value = SIZE_480x640;
				break;
			case 3:
				value = SIZE_768x1024;
				break;
			case 4:
				value = SIZE_720x1280;
				break;
			case 5:
				value = SIZE_768x1280;
				break;
			case 6:
				value = SIZE_960x1280;
				break;
			case 7:
				value = SIZE_1200x1600;
				break;
			case 8:
				value = SIZE_1088x1920;
				break;
			case 9:
				value = SIZE_1536x2048;
				break;
			case 10:
				value =  SIZE_1440x2560;
				break;
			case 11:
				value = SIZE_1920x2560;
				break;
			case 12:
				value = SIZE_2448x3264;
				break;
			default:
				break;
			}
			return value;
		}
		public static String getWhiteBalance(int size){
			String value="auto";
			switch (size) {
			case 1:
				value = "auto";
				break;
			case 2:
				value = "incandescent";
				break;
			case 3:
				value = "fluorescent";
				break;
			case 4:
				value = "warm-fluorescent";
				break;
			case 5:
				value = "daylight";
				break;
			case 6:
				value = "cloudy-daylight";
				break;
			case 7:
				value = "twilight";
				break;
			case 8:
			case 9:
			case 10:
				value = "shade";
				break;
			default:
				break;
			}
			return value;
		}
		
		public static int getExposure(int size){
			int value=2;
			switch (size) {
			case 1:
				value = 2;
				break;
			case 2:
				value = 98;
				break;
			case 3:
				value = 196;
				break;
			case 4:
				value = 406;
				break;
			case 5:
				value = 812;
				break;
			case 6:
				value = 1008;
				break;
			case 7:
				value = 2016;
				break;
			case 8:
				value = 6608;
				break;
			case 9:
				value = 16660;
				break;
			case 10:
				value = 49994;
				break;
			default:
				break;
			}
			return value;
		}
   
}
