package com.camera.setting.utils;

import java.io.Serializable;

public class CameraStateItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private String xmlVersion;
	private String rebootTiem;
	private String triggerModel;
	private String takingModel;
	private String fileSize;
	private String imgTiem;// 曝光时间
	private String imgBrightness;// 亮度
	private String imgWhiteValance;// 相片白平衡
	private String autoLight;// 自动补光
	private String ftpUrl;
	private String ftpUser;
	private String ftpPassword;
	private String tiem1;
	private String tiem2;
	private String tiem3;
	private String tiem4;
	private String tiem5;
	private String tiem6;

	public String getXmlVersion() {
		return xmlVersion;
	}

	public void setXmlVersion(String xmlVersion) {
		this.xmlVersion = xmlVersion;
	}

	public String getRebootTiem() {
		return rebootTiem;
	}

	public void setRebootTiem(String rebootTiem) {
		this.rebootTiem = rebootTiem;
	}

	public String getTriggerModel() {
		return triggerModel;
	}

	public void setTriggerModel(String triggerModel) {
		this.triggerModel = triggerModel;
	}

	public String getTakingModel() {
		return takingModel;
	}

	public void setTakingModel(String takingModel) {
		this.takingModel = takingModel;
	}

	public String getImgTiem() {
		return imgTiem;
	}

	public void setImgTiem(String imgTiem) {
		this.imgTiem = imgTiem;
	}

	public String getImgBrightness() {
		return imgBrightness;
	}

	public void setImgBrightness(String imgBrightness) {
		this.imgBrightness = imgBrightness;
	}

	public String getImgWhiteValance() {
		return imgWhiteValance;
	}

	public void setImgWhiteValance(String imgWhiteValance) {
		this.imgWhiteValance = imgWhiteValance;
	}
	

	public String getAutoLight() {
		return autoLight;
	}

	public void setAutoLight(String autoLight) {
		this.autoLight = autoLight;
	}

	public String getFileSize() {
		return fileSize;
	}

	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}

	public String getFtpUrl() {
		return ftpUrl;
	}

	public void setFtpUrl(String ftpUrl) {
		this.ftpUrl = ftpUrl;
	}

	public String getFtpUser() {
		return ftpUser;
	}

	public void setFtpUser(String ftpUser) {
		this.ftpUser = ftpUser;
	}

	public String getFtpPassword() {
		return ftpPassword;
	}

	public void setFtpPassword(String ftpPassword) {
		this.ftpPassword = ftpPassword;
	}

	public String getTiem1() {
		return tiem1;
	}

	public void setTiem1(String tiem1) {
		this.tiem1 = tiem1;
	}

	public String getTiem2() {
		return tiem2;
	}

	public void setTiem2(String tiem2) {
		this.tiem2 = tiem2;
	}

	public String getTiem3() {
		return tiem3;
	}

	public void setTiem3(String tiem3) {
		this.tiem3 = tiem3;
	}

	public String getTiem4() {
		return tiem4;
	}

	public void setTiem4(String tiem4) {
		this.tiem4 = tiem4;
	}

	public String getTiem5() {
		return tiem5;
	}

	public void setTiem5(String tiem5) {
		this.tiem5 = tiem5;
	}

	public String getTiem6() {
		return tiem6;
	}

	public void setTiem6(String tiem6) {
		this.tiem6 = tiem6;
	}
//+ by hcj @{
	public boolean isValid(){
		if(getFtpUrl() == null || getFtpUser() == null || getFtpPassword() == null
			|| getTiem6() == null){
			return false;
		}
		return true;
	}
//+ by hcj @}
}
