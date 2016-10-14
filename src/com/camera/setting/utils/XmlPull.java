package com.camera.setting.utils;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlPull {
	/**
	 * 解析xml配制文件
	 * 
	 * @param config
	 */
	public  CameraStateItem readXml(InputStream inputStream) {
		CameraStateItem item = null;
		// 构建XmlPullParserFactory
		try {

			XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
			// 获取XmlPullParser的实例
			XmlPullParser xmlPullParser = pullParserFactory.newPullParser();
			// 设置输入流 xml文件
			xmlPullParser.setInput(inputStream, "UTF-8");
			// 开始
			int eventType = xmlPullParser.getEventType();
			try {
				while (eventType != XmlPullParser.END_DOCUMENT) {
					String nodeName = xmlPullParser.getName();
					switch (eventType) {
					// 文档开始
					case XmlPullParser.START_DOCUMENT:
						item = new CameraStateItem();
						break;
					// 开始节点
					case XmlPullParser.START_TAG:
						if ("version".equals(nodeName)) {
//							int version = Integer.parseInt(xmlPullParser.nextText());
							item.setXmlVersion(xmlPullParser.nextText());
						} else if ("rebooTime".equals(nodeName)) {
							item.setRebootTiem(xmlPullParser.nextText());
						} else if ("triggerModel".equals(nodeName)) {//触发模式
							item.setTriggerModel(xmlPullParser.nextText());
						} else if ("takingModel".equals(nodeName)) {//拍照模式
							item.setTakingModel(xmlPullParser.nextText());
						} else if ("fileSize".equals(nodeName)) {//分辨率设置  
							item.setFileSize(xmlPullParser.nextText());
						} else if ("img_time".equals(nodeName)) {
							item.setImgTiem(xmlPullParser.nextText());
						}  else if ("img_brightness".equals(nodeName)) {
							item.setImgBrightness(xmlPullParser.nextText());
						}else if ("img_white_balance".equals(nodeName)) {
							item.setImgWhiteValance(xmlPullParser.nextText());
						}else if ("auto_light".equals(nodeName)) {
							item.setAutoLight(xmlPullParser.nextText());
						}
						break;
					// 结束节点
					case XmlPullParser.END_TAG:
						break;
					default:
						break;
					}
					switch (eventType) {
					case XmlPullParser.START_TAG:
						if ("ftpUrl".equals(nodeName)) {
							item.setFtpUrl(xmlPullParser.nextText());
						} else if ("ftpUser".equals(nodeName)) {
							item.setFtpUser(xmlPullParser.nextText());
						} else if ("ftpPassword".equals(nodeName)) {
							item.setFtpPassword(xmlPullParser.nextText());
						} 
						break;
					case XmlPullParser.END_TAG:
						break;
					default:
						break;
					}
					switch (eventType) {
					case XmlPullParser.START_TAG:
						if ("time1".equals(nodeName)) {
							item.setTiem1(xmlPullParser.nextText());
						}else if ("time2".equals(nodeName)) {
							item.setTiem2(xmlPullParser.nextText());
						}else if ("time3".equals(nodeName)) {
							item.setTiem3(xmlPullParser.nextText());
						}else if ("time4".equals(nodeName)) {
							item.setTiem4(xmlPullParser.nextText());
						}else if ("time5".equals(nodeName)) {
							item.setTiem5(xmlPullParser.nextText());
						}else if ("time6".equals(nodeName)) {
							item.setTiem6(xmlPullParser.nextText());
						} 
						break;
					case XmlPullParser.END_TAG:
						break;
					default:
						break;
					}
					eventType = xmlPullParser.next();
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return item;
	}
	
}
