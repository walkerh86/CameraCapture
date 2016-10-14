package com.zg; 
public class IO { 
    public static native int cmd(int bcmd);
    public static native int spiwrite(int reg_addr,int write_val);
    public static native int spiread(int reg_addr,byte[] buf,int len);
    static 
    {
        System.loadLibrary("zgio_jni"); 
    }
}