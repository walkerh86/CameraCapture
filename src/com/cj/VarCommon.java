package com.cj;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.camera.setting.ftp.FTP.DeleteFileProgressListener;
import com.camera.setting.ftp.FTP.UploadProgressListener;

public class VarCommon{
	private static VarCommon mVar;
	
 	private VarCommon(){
	}

	public static VarCommon getInstance(){
		if(mVar == null){
			mVar = new VarCommon();
		}
		return mVar;
	}

	//for sync uploading
	private boolean mImgUploading;
	public void setImgUploading(boolean isUploading){
		mImgUploading = isUploading;
	}

	public boolean isImgUploading(){
		return mImgUploading;
	}

	//for common executor
	private ExecutorService mExecutor;

	public void setExecutor(ExecutorService executor){
		mExecutor = executor;
	}

	public ExecutorService getExecutor(){
		return mExecutor;
	}

	//
	public interface OnUploadRequestListener{
		void onUploadRequest(String localPath, String remoteDir, UploadProgressListener uploadListener);
		void onDeleteRequest(String remoteDir, DeleteFileProgressListener deleteListener);
	}
	private OnUploadRequestListener mOnUploadRequestListener;
	public void setOnUploadRequestListener(OnUploadRequestListener listener){
		mOnUploadRequestListener = listener;
	}
	
	public void uploadRequest(String localPath, String remoteDir, UploadProgressListener uploadListener){
		if(mOnUploadRequestListener != null){
			mOnUploadRequestListener.onUploadRequest(localPath,remoteDir,uploadListener);
		}
	}

	public void deleteRequest(String remoteDir, DeleteFileProgressListener deleteListener){
		if(mOnUploadRequestListener != null){
			mOnUploadRequestListener.onDeleteRequest(remoteDir,deleteListener);
		}
	}

	//
/*
	private boolean mDayNightCheckByTime;
	public void setDayNightCheckByTime(boolean isTrue){
		mDayNightCheckByTime = isTrue;
	}

	public boolean isDayNightCheckByTime(){
		return mDayNightCheckByTime;
	}
*/
	private boolean mCameraFullSleep;
	public void setCameraFullSleep(boolean isFullSleep){
		mCameraFullSleep = isFullSleep;
	}

	public boolean isCameraFullSleep(){
		return mCameraFullSleep;
	}

	//
	private int mUploadMode = -1;
	public void setUploadMode(int mode){
		mUploadMode = mode;
	}

	public int getUploadMode(){
		return mUploadMode;
	}

	//
	private boolean mTimeSyncDone;
	public void setTimeSyncDone(boolean done){
		mTimeSyncDone = done;
	}
	public boolean isTimeSyncDone(){
		return mTimeSyncDone;
	}
}
