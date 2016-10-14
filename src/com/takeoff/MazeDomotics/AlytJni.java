package com.takeoff.MazeDomotics;

public class AlytJni {
	public static native int openSpiSrdy(String path);
	public static native int readSpiSrdy(int fd);
	public static native int closeSpiSrdy(int fd);
	static 
    {
        System.loadLibrary("alytjni"); 
    }
}
