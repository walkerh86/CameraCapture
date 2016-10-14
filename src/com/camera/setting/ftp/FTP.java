package com.camera.setting.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import com.camera.setting.utils.Utils;
import android.content.Context;
import android.util.Log;

public class FTP {
	public static final String TAG = "FTP";
	/**
	 * 服务器名.
	 */
	private String hostName;

	/**
	 * 端口号
	 */
	private int serverPort;

	/**
	 * 用户名.
	 */
	private String userName;

	/**
	 * 密码.
	 */
	private String password;

	/**
	 * FTP连接.
	 */
	private FTPClient ftpClient;

	public FTP(Context context){
		this.hostName=Utils.getProperty(context, Utils.KEY_FTP_URL);
		this.userName=Utils.getProperty(context, Utils.KEY_FTP_USERNAME);
		this.password=Utils.getProperty(context, Utils.KEY_FTP_PASSWORD);
//		Log.i(TAG, "--host:"+hostName + " userName:"+ userName+" password:"+password);
		this.serverPort = 21;
		this.ftpClient = new FTPClient();
		
	}

	// -------------------------------------------------------文件上传方法------------------------------------------------

	/**
	 * 上传单个文件.
	 * 
	 * @param localFile
	 *            本地文件
	 * @return true上传成功, false上传失败
	 * @throws IOException
	 */
	public boolean uploadingSingle(File localFile,String remotePath,
			UploadProgressListener listener) throws IOException {
		Log.i(TAG, "uploadingSingle:"+localFile.getName());
		boolean flag = true;
		// 上传之前初始化
		this.uploadBeforeOperate(remotePath, listener);
		// 带有进度的方式
		FileInputStream buffIn = new FileInputStream(localFile);
		flag = ftpClient.storeFile(localFile.getName(), buffIn);
		buffIn.close();
		return flag;
	}
	
	/**
	 * dahai.zhou
	 * 上传文件
	 * @param ftp
	 * @param remoteFileName 远程文件名称
	 * @param locaFileName 本地文件名称
	 */
	public boolean uploadingSingle(String remoteFileName, String locaFileName,UploadProgressListener listener) {
		boolean isSuccess = false;
		File file = new File(locaFileName);
		try {
			// 上传之前初始化
			this.uploadBeforeOperate(remoteFileName, listener);
			InputStream input = new FileInputStream(locaFileName);
			isSuccess = ftpClient.storeFile(remoteFileName, input);
			input.close();
			if(isSuccess){
				listener.onUploadProgress(Utils.FTP_UPLOAD_SUCCESS, 0L, file);
			}else{
				listener.onUploadProgress(Utils.FTP_UPLOAD_FAIL, 0L, file);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			isSuccess = false;
			listener.onUploadProgress(Utils.FTP_UPLOAD_FAIL, 0L, file);
		} catch (IOException e) {
			e.printStackTrace();
			isSuccess = false;
			listener.onUploadProgress(Utils.FTP_UPLOAD_FAIL, 0L, file);
		}
		// 上传完成之后关闭连接
		try {
			uploadAfterOperate(listener);
		} catch (IOException e) {
			e.printStackTrace();
		}
		 return isSuccess;
	}
	
	public boolean mkdir(String pathname){
		try {
			// 打开FTP服务
			Log.i(TAG, "try mkdir:"+pathname);
			try {
				this.openConnect();
			} catch (IOException e) {
				Log.e(TAG, "--open connect error",e);
				return false;
			}
			// 先判断服务器文件是否存在
			FTPFile[] files = ftpClient.listFiles(pathname);
			if (files.length == 0) {
				// 设置模式
				ftpClient.setFileTransferMode(org.apache.commons.net.ftp.FTP.STREAM_TRANSFER_MODE);
				// FTP下创建文件夹
				ftpClient.makeDirectory(pathname);
			}
			Log.i(TAG, "--send mkdir");
			this.closeConnect();
			return true;
		} catch (IOException e) {
			Log.e(TAG, "--mkdir error",e);
			return false;
		}
	}
	
	
	/**
	 * 上传文件之前初始化相关参数
	 * 
	 * @param remotePath
	 *            FTP目录
	 * @param listener
	 *            监听器
	 * @throws IOException
	 */
	public void uploadBeforeOperate(String remotePath,
			UploadProgressListener listener) throws IOException {

		// 打开FTP服务
		try {
			this.openConnect();
			if(listener!=null)
				listener.onUploadProgress(Utils.FTP_CONNECT_SUCCESSS, 0,
						null);
		} catch (IOException e1) {
			Log.e(TAG, "uploadBeforeOperate error",e1);
			if(listener!=null)
				listener.onUploadProgress(Utils.FTP_CONNECT_FAIL, 0, null);
			return;
		}

		// 设置模式
		ftpClient.setFileTransferMode(org.apache.commons.net.ftp.FTP.STREAM_TRANSFER_MODE);
		// FTP下创建文件夹
//		ftpClient.makeDirectory(remotePath);
		// 改变FTP目录
		ftpClient.changeWorkingDirectory(remotePath);
		// 上传单个文件
	}

	/**
	 * 上传完成之后关闭连接
	 * 
	 * @param listener
	 * @throws IOException
	 */
	public void uploadAfterOperate(UploadProgressListener listener)
			throws IOException {
		this.closeConnect();
		listener.onUploadProgress(Utils.FTP_DISCONNECT_SUCCESS, 0, null);
	}
	

	
	
	

	// -------------------------------------------------------文件下载方法------------------------------------------------

	/**
	 * 下载单个文件，可实现断点下载.
	 * 
	 * @param serverPath
	 *            Ftp目录及文件路径
	 * @param localPath
	 *            本地目录
	 * @param fileName       
	 *            下载之后的文件名称
	 * @param listener
	 *            监听器
	 * @throws IOException
	 */
	public void downloadSingleFile(String serverPath, String localPath, String fileName, DownLoadProgressListener listener)
			throws Exception {
		if (this.hostName != null || !this.hostName.equals("")
				&& this.userName != null || !this.userName.equals("")
				&& this.password != null || !this.password.equals("")) {
			 Utils.deleteFile(new File(localPath+fileName));
		}
		// 打开FTP服务
		try {
			this.openConnect();
			listener.onDownLoadProgress(Utils.FTP_CONNECT_SUCCESSS, 0, null);
			Log.i(TAG, "downloadSingleFile openConnect");
		} catch (IOException e1) {
			e1.printStackTrace();
			listener.onDownLoadProgress(Utils.FTP_CONNECT_FAIL, 0, null);
			Log.i(TAG, "downloadSingleFile Utils.FTP_CONNECT_FAIL");
			return;
		}
		
		// 先判断服务器文件是否存在
		FTPFile[] files = ftpClient.listFiles(serverPath);
		if (files.length == 0) {
			listener.onDownLoadProgress(Utils.FTP_FILE_NOTEXISTS, 0, null);
			Log.i(TAG, "downloadSingleFile Utils.FTP_FILE_NOTEXISTS");
			return;
		}

		//创建本地文件夹
		File mkFile = new File(localPath);
		if (!mkFile.exists()) {
			mkFile.mkdirs();
		}

		localPath = localPath + fileName;
		// 接着判断下载的文件是否能断点下载
		long serverSize = files[0].getSize(); // 获取远程文件的长度
		File localFile = new File(localPath);
		long localSize = 0;
		if (localFile.exists()) {
			localSize = localFile.length(); // 如果本地文件存在，获取本地文件的长度
			if (localSize >= serverSize) {
				File file = new File(localPath);
				file.delete();
			}
		}
		
		// 进度
		long step = serverSize / 100;
		long process = 0;
		long currentSize = 0;
		// 开始准备下载文件
		OutputStream out = new FileOutputStream(localFile, true);
		ftpClient.setRestartOffset(localSize);
		InputStream input = ftpClient.retrieveFileStream(serverPath);
		byte[] b = new byte[1024];
		int length = 0;
		if(input != null){
			while ((length = input.read(b)) != -1) {
				out.write(b, 0, length);
				currentSize = currentSize + length;
				if (currentSize / step != process) {
					process = currentSize / step;
					if (process % 5 == 0) {  //每隔%5的进度返回一次
						listener.onDownLoadProgress(Utils.FTP_DOWN_LOADING, process, null);
						Log.i(TAG, "downloadSingleFile Utils.FTP_DOWN_LOADING...");
					}
				}
			}
		}else{
			listener.onDownLoadProgress(Utils.FTP_FILE_NOTEXISTS, 0, null);
			Log.i(TAG, "downloadSingleFile Utils.FTP_FILE_NOTEXISTS1");
		}
		out.flush();
		out.close();
		input.close();
		
		// 此方法是来确保流处理完毕，如果没有此方法，可能会造成现程序死掉
		if (ftpClient.completePendingCommand()) {
			listener.onDownLoadProgress(Utils.FTP_DOWN_SUCCESS, 0, new File(localPath));
			Log.i(TAG, "downloadSingleFile Utils.FTP_DOWN_SUCCESS");
		} else {
			listener.onDownLoadProgress(Utils.FTP_DOWN_FAIL, 0, null);
			Log.i(TAG, "downloadSingleFile Utils.FTP_DOWN_FAIL");
		}

		// 下载完成之后关闭连接
		this.closeConnect();
		listener.onDownLoadProgress(Utils.FTP_DISCONNECT_SUCCESS, 0, null);

		return;
	}
	
	public void downloadSingleFile1(String serverDir, String serverPath, String localPath, String fileName, DownLoadProgressListener listener)
			throws Exception {
		if (this.hostName != null || !this.hostName.equals("")
				&& this.userName != null || !this.userName.equals("")
				&& this.password != null || !this.password.equals("")) {
			 Utils.deleteFile(new File(localPath+fileName));
		}
		// 打开FTP服务
		try {
			this.openConnect();
			listener.onDownLoadProgress(Utils.FTP_CONNECT_SUCCESSS, 0, null);
			Log.i(TAG, "downloadSingleFile openConnect");
		} catch (IOException e1) {
			e1.printStackTrace();
			listener.onDownLoadProgress(Utils.FTP_CONNECT_FAIL, 0, null);
			Log.i(TAG, "downloadSingleFile Utils.FTP_CONNECT_FAIL");
			return;
		}
		
		// 先判断服务器文件是否存在
		FTPFile[] files = ftpClient.listFiles(serverDir);
		if (files.length == 0) {
			listener.onDownLoadProgress(Utils.FTP_FILE_NOTEXISTS, 0, null);
			Log.i(TAG, "downloadSingleFile Utils.FTP_FILE_NOTEXISTS0");
			return;
		}

		//创建本地文件夹
		File mkFile = new File(localPath);
		if (!mkFile.exists()) {
			mkFile.mkdirs();
		}

		localPath = localPath + fileName;
		// 接着判断下载的文件是否能断点下载
		long serverSize = files[0].getSize(); // 获取远程文件的长度
		File localFile = new File(localPath);
		long localSize = 0;
		if (localFile.exists()) {
			localSize = localFile.length(); // 如果本地文件存在，获取本地文件的长度
			if (localSize >= serverSize) {
				File file = new File(localPath);
				file.delete();
			}
		}
		
		// 进度
		long step = serverSize / 100;
		long process = 0;
		long currentSize = 0;
		// 开始准备下载文件
		OutputStream out = new FileOutputStream(localFile, true);
		ftpClient.setRestartOffset(localSize);
		InputStream input = ftpClient.retrieveFileStream(serverPath);
		byte[] b = new byte[1024];
		int length = 0;
		if(input != null){
			while ((length = input.read(b)) != -1) {
				out.write(b, 0, length);
				currentSize = currentSize + length;
				if (currentSize / step != process) {
					process = currentSize / step;
					if (process % 5 == 0) {  //每隔%5的进度返回一次
						listener.onDownLoadProgress(Utils.FTP_DOWN_LOADING, process, null);
						Log.i(TAG, "downloadSingleFile Utils.FTP_DOWN_LOADING...");
					}
				}
			}
		}else{
			listener.onDownLoadProgress(Utils.FTP_FILE_NOTEXISTS, 0, null);
			Log.i(TAG, "downloadSingleFile Utils.FTP_FILE_NOTEXISTS1");
		}
		out.flush();
		out.close();
		input.close();
		
		// 此方法是来确保流处理完毕，如果没有此方法，可能会造成现程序死掉
		if (ftpClient.completePendingCommand()) {
			listener.onDownLoadProgress(Utils.FTP_DOWN_SUCCESS, 0, new File(localPath));
			Log.i(TAG, "downloadSingleFile Utils.FTP_DOWN_SUCCESS");
		} else {
			listener.onDownLoadProgress(Utils.FTP_DOWN_FAIL, 0, null);
			Log.i(TAG, "downloadSingleFile Utils.FTP_DOWN_FAIL");
		}

		// 下载完成之后关闭连接
		this.closeConnect();
		listener.onDownLoadProgress(Utils.FTP_DISCONNECT_SUCCESS, 0, null);

		return;
	}

	// -------------------------------------------------------文件删除方法------------------------------------------------

	/**
	 * 删除Ftp下的文件.
	 * 
	 * @param serverPath
	 *            Ftp目录及文件路径
	 * @param listener
	 *            监听器
	 * @throws IOException
	 */
	public void deleteSingleFile(String serverPath, DeleteFileProgressListener listener)
			throws Exception {
		// 先判断服务器文件是否存在
		FTPFile[] files = ftpClient.listFiles(serverPath);
		if (files.length == 0) {
			listener.onDeleteProgress(Utils.FTP_FILE_NOTEXISTS);
			return;
		}
		
		//进行删除操作
		boolean flag = true;
		flag = ftpClient.deleteFile(serverPath);
		if (flag) {
			listener.onDeleteProgress(Utils.FTP_DELETEFILE_SUCCESS);
		} else {
			listener.onDeleteProgress(Utils.FTP_DELETEFILE_FAIL);
		}
		
		// 删除完成之后关闭连接
		this.closeConnect();
		listener.onDeleteProgress(Utils.FTP_DISCONNECT_SUCCESS);
		
		return;
	}

	// -------------------------------------------------------打开关闭连接------------------------------------------------

	
	/**
	 * 打开FTP服务.
	 * 
	 * @throws IOException
	 */
	public void openConnect() throws IOException {
		// 中文转码
		ftpClient.setControlEncoding("UTF-8");
		// 连接至服务器
//		ftpClient.setDefaultTimeout(30000);
//		ftpClient.setConnectTimeout(30000);
//		ftpClient.setDataTimeout(30000);
//		ftpClient.setSoTimeout(30000);
		
		ftpClient.connect(hostName, serverPort);
		// 获取响应值
		int reply = ftpClient.getReplyCode();// 服务器响应值
		if (!FTPReply.isPositiveCompletion(reply)) {
			// 断开连接
			ftpClient.disconnect();
			throw new IOException("connect fail: " + reply);
		}
		// 登录到服务器
		ftpClient.login(userName, password);
		// 获取响应值
		reply = ftpClient.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			// 断开连接
			ftpClient.disconnect();
			throw new IOException("connect fail: " + reply);
		} else {
			// 获取登录信息
			FTPClientConfig config = new FTPClientConfig(ftpClient
					.getSystemType().split(" ")[0]);
			config.setServerLanguageCode("zh");
			ftpClient.configure(config);
			// 使用被动模式设为默认
			ftpClient.enterLocalPassiveMode();
			// 二进制文件支持
			ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
		}
	}


	/**
	 * 关闭FTP服务.
	 * 
	 * @throws IOException
	 */
	public void closeConnect() throws IOException {
		if (ftpClient != null) {
			// 退出FTP
			ftpClient.logout();
			// 断开连接
			ftpClient.disconnect();
		}
	}
	public FTPFile[] listFile() throws IOException{
		if (ftpClient != null) {
			return ftpClient.listFiles();
		}else
			return null;
	}

	// ---------------------------------------------------上传、下载、删除监听---------------------------------------------
	
	/*
	 * 上传进度监听
	 */
	public interface UploadProgressListener {
		public void onUploadProgress(String currentStep, long uploadSize, File file);
	}

	/*
	 * 下载进度监听
	 */
	public interface DownLoadProgressListener {
		public void onDownLoadProgress(String currentStep, long downProcess, File file);
	}

	/*
	 * 文件删除监听
	 */
	public interface DeleteFileProgressListener {
		public void onDeleteProgress(String currentStep);
	}

	public void setFileTransferMode(int streamTransferMode) throws IOException {
		ftpClient.setFileTransferMode(streamTransferMode);
		
	}

	public void makeDirectory(String idName) throws IOException {
		ftpClient.makeDirectory(idName);
	}

	public void changeWorkingDirectory(String idName) throws IOException {
		ftpClient.changeWorkingDirectory(idName);
	}

	public FTPFile[] listFiles(String dirPath) {
		FTPFile[] files=null;
		try {
			files = ftpClient.listFiles(dirPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return files;
	}
	
	public FTPFile[] getDir(String pathname){
		try {
			// 打开FTP服务
			Log.i(TAG, "try mkdir:"+pathname);
			try {
				this.openConnect();
			} catch (IOException e) {
				Log.e(TAG, "--open connect error",e);
				return null;
			}
			// 先判断服务器文件是否存在
			FTPFile[] files = ftpClient.listFiles(pathname);
			System.out.println("guos listFile=="+files.length);
			if (files.length == 0) {
				// 设置模式
				ftpClient.setFileTransferMode(org.apache.commons.net.ftp.FTP.STREAM_TRANSFER_MODE);
				// FTP下创建文件夹
				ftpClient.makeDirectory(pathname);
			}
			Log.i(TAG, "--send mkdir");
			this.closeConnect();
			return files;
		} catch (IOException e) {
			Log.e(TAG, "--mkdir error",e);
			return null;
		}
	}
}