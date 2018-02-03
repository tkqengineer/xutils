package com.tkqengineer.date;

import com.tkqengineer.http.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author : tengkangquan@jianyi.tech
 * @date : 2018/2/3 17:41
 */
public class DateUtils {
	private final static Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	
	public static String dateFormat(Date date, String pattern) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		return simpleDateFormat.format(date);
	}
	
	public static Date parseDate(String date, String pattern) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		try {
			return simpleDateFormat.parse(date);
		} catch (ParseException e) {
			logger.error("时间解析错误", e);
		}
		return null;
	}
	
	
}
