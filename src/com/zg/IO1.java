package com.zg; 
public class IO1 { 
    public static native int mode(int mode);
    
    static 
    {
        System.loadLibrary("zgio_jni1"); 
    }
}