package com.tkqengineer.http;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author : tengkangquan@jianyi.tech
 * @date : 2018/1/12 23:06
 */
public class HttpUtils {
	private final static Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	
	private HttpUtils() {
	}
	
	private static CloseableHttpClient getCloseableHttpClient() {
		return HttpClients.createDefault();
	}
	
	private static class DefaultRequestConfigBuilder {
		private static final RequestConfig INSTANCE =
				RequestConfig.custom()
						//设置连接超时时间
						.setConnectTimeout(5000)
						// 设置请求超时时间
						.setConnectionRequestTimeout(5000)
						.setSocketTimeout(5000)
						//默认允许自动重定向
						.setRedirectsEnabled(true)
						.build();
	}
	
	private static RequestConfig getDefaultRequestConfig() {
		return DefaultRequestConfigBuilder.INSTANCE;
	}
	
	private static String doGetParams(String url, Object param) {
		if (param == null) {
			return url;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(url);
		if (param instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) param;
			Set<Map.Entry<String, Object>> entries = map.entrySet();
			Iterator<Map.Entry<String, Object>> iterator = entries.iterator();
			
			if (url.contains("?")) {
				while (iterator.hasNext()) {
					Map.Entry<String, Object> next = iterator.next();
					builder.append("&").append(next.getKey()).append("=").append(next.getValue());
				}
			} else {
				int count = 0;
				while (iterator.hasNext()) {
					Map.Entry<String, Object> next = iterator.next();
					if (count == 0) {
						builder.append("?").append(next.getKey()).append("=").append(next.getValue());
						count++;
					} else {
						builder.append("&").append(next.getKey()).append("=").append(next.getValue());
					}
					
				}
			}
		} else {
			
			List<Field> fields = FieldUtils.getAllFieldsList(param.getClass());
			if (url.contains("?")) {
				for (Field field : fields) {
					String fieldName = field.getName();
					try {
						field.setAccessible(true);
						Object value = field.get(param);
						if (value != null) {
							builder.append("&").append(fieldName).append("=").append(value);
						}
					} catch (IllegalAccessException e) {
						logger.info("反射参数失败:参数=" + fieldName, e);
					}
				}
				
			} else {
				int count = 0;
				for (Field field : fields) {
					String fieldName = field.getName();
					try {
						field.setAccessible(true);
						Object value = field.get(param);
						if (value != null) {
							if (count == 0) {
								builder.append("?").append(fieldName).append("=").append(value);
								count++;
							} else {
								builder.append("&").append(fieldName).append("=").append(value);
							}
						}
					} catch (IllegalAccessException e) {
						logger.info("反射参数失败:参数=" + fieldName, e);
					}
				}
			}
		}
		return builder.toString();
	}
	
	private static void closeHttpClient(Closeable httpClient) {
		try {
			if (httpClient != null) {
				httpClient.close();
			}
		} catch (IOException e) {
			logger.info("httpClient关闭失败", e);
		}
		
	}
	
	private static HttpGet getHttpGetMethod(String url) {
		HttpGet httpGet = new HttpGet(url);
		httpGet.setConfig(getDefaultRequestConfig());
		return httpGet;
	}
	
	private static HttpPost getHttpPostMethod(String url) {
		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(getDefaultRequestConfig());
		return httpPost;
	}
	
	public static String httpGet(String url) {
		return httpGet(url, "UTF-8");
	}
	
	public static String httpGet(String url, String charset) {
		CloseableHttpClient closeableHttpClient = getCloseableHttpClient();
		HttpGet httpGet = getHttpGetMethod(url);
		try {
			CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (IOException e) {
			logger.info("httpGet请求错误:url=" + url, e);
		} finally {
			closeHttpClient(closeableHttpClient);
		}
		return null;
	}
	
	public static String httpGet(String url, Object param) {
		return httpGet(url, param, "UTF-8");
	}
	
	public static String httpGet(String url, Object param, String charset) {
		return httpGet(doGetParams(url, param), charset);
	}
	
	public static String httpPost(String url) {
		return httpPost(url, "UTF-8");
	}
	
	public static String httpPost(String url, String charset) {
		CloseableHttpClient closeableHttpClient = getCloseableHttpClient();
		HttpPost httpPost = getHttpPostMethod(url);
		try {
			CloseableHttpResponse response = closeableHttpClient.execute(httpPost);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (IOException e) {
			logger.info("httpPost请求错误:url=" + url, e);
		} finally {
			closeHttpClient(closeableHttpClient);
		}
		return null;
	}
	
	public static String httpPost(String url, Object param) {
		return httpPost(url, param, "UTF-8");
	}
	
	private static List<NameValuePair> doPostParam(Object param) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		
		if (param instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) param;
			Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> next = iterator.next();
				nvps.add(new BasicNameValuePair(next.getKey(), next.getValue().toString()));
			}
		} else {
			
			List<Field> fields = FieldUtils.getAllFieldsList(param.getClass());
			for (Field field : fields) {
				String fieldName = field.getName();
				try {
					field.setAccessible(true);
					Object value = field.get(param);
					if (value != null) {
						nvps.add(new BasicNameValuePair(fieldName, value.toString()));
					}
				} catch (IllegalAccessException e) {
					logger.info("反射参数失败:参数=" + fieldName, e);
				}
			}
		}
		return nvps;
		
	}
	
	public static String httpPost(String url, Object param, String charset) {
		
		return httpPost(url, param, null, charset);
	}
	
	public static String httpRestPost(String url, Object param) {
		return httpRestPost(url, param, "UTF-8");
	}
	
	public static String httpRestPost(String url, Object param, String charset) {
		return httpRestPost(url, param, null, charset);
	}
	
	private static CloseableHttpClient getSSLClientDefault() {
		try {
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(
					null, new TrustStrategy() {
						//信任所有
						public boolean isTrusted(X509Certificate[] chain, String authType) {
							return true;
						}
					}).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
			return HttpClients.custom().setSSLSocketFactory(sslsf).build();
		} catch (KeyManagementException e) {
			logger.info("ssl客户端创建失败", e);
		} catch (NoSuchAlgorithmException e) {
			logger.info("ssl客户端创建失败", e);
		} catch (KeyStoreException e) {
			logger.info("ssl客户端创建失败", e);
		}
		return null;
	}
	
	public static String httpsGet(String url) {
		return httpsGet(url, "UTF-8");
	}
	
	public static String httpsGet(String url, String charset) {
		return httpsGet(url, null, charset);
	}
	
	public static String httpsGet(String url, Object param) {
		return httpsGet(url, param, "UTF-8");
	}
	
	public static String httpsGet(String url, Object param, String charset) {
		CloseableHttpClient closeableHttpClient = getSSLClientDefault();
		HttpGet getMethod = getHttpGetMethod(doGetParams(url, param));
		try {
			CloseableHttpResponse response = closeableHttpClient.execute(getMethod);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (IOException e) {
			logger.info("httpsGet请求失败", e);
		} finally {
			closeHttpClient(closeableHttpClient);
		}
		return null;
	}
	
	public static String httpsPost(String url) {
		return httpsPost(url, "UTF-8");
	}
	
	public static String httpsPost(String url, String charset) {
		return httpsPost(url, null, charset);
	}
	
	public static String httpsPost(String url, Object param) {
		return httpsPost(url, param, "UTF-8");
	}
	
	public static String httpsPost(String url, Object param, String charset) {
		
		return httpsPost(url, param, null, charset);
	}
	
	public static String httpsRestPost(String url, Object param) {
		return httpsRestPost(url, param, "UTF-8");
	}
	
	public static String httpsRestPost(String url, Object param, String charset) {
		return httpsRestPost(url, param, null, charset);
	}
	
	public static String httpPostUploadFile(String url, String localFilePath, String filedName, String fileOriginName, Object params) {
		return httpPostUploadFile(url, new File(localFilePath), filedName, fileOriginName, params);
	}
	
	public static String httpPostUploadFile(String url, File file, String filedName, String fileOriginName, Object params) {
		
		if (file == null) {
			logger.info("传入的file对象不能为空");
			return null;
		}
		if (file.isDirectory()) {
			logger.info("传入的file对象不能是文件夹,请输入正确的文件路径");
			return null;
		}
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			
			return httpPostUploadFile(url, inputStream, filedName, fileOriginName, params);
			
		} catch (FileNotFoundException e) {
			logger.info("创建文件io失败");
		} finally {
			
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					logger.info("关闭上传文件流失败", e);
				}
			}
			
		}
		return null;
	}
	
	public static String httpPostUploadFile(String url, InputStream inputStream, String filedName, String fileOriginName, Object params) {
		
		return httpPostUploadFile(url, inputStream, filedName, fileOriginName, null, params);
	}
	
	public static String httpPostUploadFile(String url, InputStream inputStream, String filedName, String fileOriginName, Map<String, String> headers, Object params) {
		if (inputStream == null) {
			logger.info("文件输入流为空");
			return null;
		}
		CloseableHttpClient httpClient = getCloseableHttpClient();
		HttpPost postMethod = getHttpPostMethod(url);
		setHeaders(postMethod, headers);
		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
		multipartEntityBuilder.addBinaryBody(filedName, inputStream, ContentType.MULTIPART_FORM_DATA, fileOriginName);
		// 设置上传的其他参数
		setUploadParams(multipartEntityBuilder, params);
		HttpEntity reqEntity = multipartEntityBuilder.build();
		postMethod.setEntity(reqEntity);
		try {
			CloseableHttpResponse response = httpClient.execute(postMethod);
			return EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeHttpClient(httpClient);
		}
		return null;
	}
	
	private static void setUploadParams(MultipartEntityBuilder multipartEntityBuilder, Object param) {
		
		if (param == null) {
			return;
		}
		if (param instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) param;
			Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> next = iterator.next();
				//处理中文乱码问题
				ContentType contentType = ContentType.create("text/plain", "UTF-8");
				StringBody stringBody = new StringBody(next.getValue().toString(), contentType);
				multipartEntityBuilder.addPart(next.getKey(), stringBody);
				
			}
		} else {
			
			List<Field> fields = FieldUtils.getAllFieldsList(param.getClass());
			for (Field field : fields) {
				String fieldName = field.getName();
				try {
					field.setAccessible(true);
					Object value = field.get(param);
					if (value != null) {
						//处理中文乱码问题
						ContentType contentType = ContentType.create("text/plain", "UTF-8");
						StringBody stringBody = new StringBody(value.toString(), contentType);
						multipartEntityBuilder.addPart(fieldName, stringBody);
					}
				} catch (IllegalAccessException e) {
					logger.info("反射参数失败:参数=" + fieldName, e);
				}
			}
		}
	}
	
	private static void setHeaders(HttpRequest request, Map<String, String> headers) {
		if (headers != null && headers.size() > 0) {
			Iterator<Map.Entry<String, String>> iterator = headers.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, String> next = iterator.next();
				request.setHeader(next.getKey(), next.getValue());
			}
		}
	}
	
	public static String httpGet(String url, Object param, Map<String, String> headers, String charset) {
		CloseableHttpClient closeableHttpClient = getCloseableHttpClient();
		url = doGetParams(url, param);
		HttpGet httpGetMethod = getHttpGetMethod(url);
		setHeaders(httpGetMethod, headers);
		try {
			CloseableHttpResponse response = closeableHttpClient.execute(httpGetMethod);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (IOException e) {
			logger.info("httpGet请求错误", e);
		} finally {
			closeHttpClient(closeableHttpClient);
		}
		
		return null;
	}
	
	public static String httpsGet(String url, Object param, Map<String, String> headers, String charset) {
		CloseableHttpClient closeableHttpClient = getSSLClientDefault();
		url = doGetParams(url, param);
		HttpGet httpGetMethod = getHttpGetMethod(url);
		setHeaders(httpGetMethod, headers);
		try {
			CloseableHttpResponse response = closeableHttpClient.execute(httpGetMethod);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (IOException e) {
			logger.info("httpsGet请求错误", e);
		} finally {
			closeHttpClient(closeableHttpClient);
		}
		return null;
	}
	
	public static String httpPost(String url, Object param, Map<String, String> headers, String charset) {
		
		CloseableHttpClient closeableHttpClient = getCloseableHttpClient();
		HttpPost postMethod = getHttpPostMethod(url);
		setHeaders(postMethod, headers);
		List<NameValuePair> nvps = doPostParam(param);
		try {
			HttpEntity httpEntity = new UrlEncodedFormEntity(nvps, charset);
			postMethod.setEntity(httpEntity);
			CloseableHttpResponse response = closeableHttpClient.execute(postMethod);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (UnsupportedEncodingException e) {
			logger.info("httpPost添加表单数据失败", e);
		} catch (ClientProtocolException e) {
			logger.info("httpPost协议失败", e);
		} catch (IOException e) {
			logger.info("httpPost请求失败", e);
		} finally {
			closeHttpClient(closeableHttpClient);
		}
		
		return null;
	}
	
	public static String httpRestPost(String url, Object param, Map<String, String> headers, String charset) {
		
		CloseableHttpClient closeableHttpClient = getCloseableHttpClient();
		HttpPost postMethod = getHttpPostMethod(url);
		setHeaders(postMethod, headers);
		postMethod.addHeader("Content-type", "application/json; charset=" + charset);
		postMethod.setHeader("Accept", "application/json");
		postMethod.setEntity(new StringEntity(JSON.toJSONString(param), Charset.forName(charset)));
		try {
			CloseableHttpResponse response = closeableHttpClient.execute(postMethod);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (IOException e) {
			logger.info("http请求异常", e);
		}
		
		return null;
	}
	
	public static String httpsPost(String url, Object param, Map<String, String> headers, String charset) {
		CloseableHttpClient sslClientDefault = getSSLClientDefault();
		HttpPost postMethod = getHttpPostMethod(url);
		setHeaders(postMethod, headers);
		List<NameValuePair> nvps = doPostParam(param);
		try {
			HttpEntity httpEntity = new UrlEncodedFormEntity(nvps, charset);
			postMethod.setEntity(httpEntity);
			CloseableHttpResponse response = sslClientDefault.execute(postMethod);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (UnsupportedEncodingException e) {
			logger.info("httpsPost添加表单数据失败", e);
		} catch (ClientProtocolException e) {
			logger.info("httpsPost协议失败", e);
		} catch (IOException e) {
			logger.info("httpsPost请求失败", e);
		} finally {
			closeHttpClient(sslClientDefault);
		}
		return null;
	}
	
	public static String httpsRestPost(String url, Object param, Map<String, String> headers, String charset) {
		
		CloseableHttpClient sslClientDefault = getSSLClientDefault();
		HttpPost postMethod = getHttpPostMethod(url);
		setHeaders(postMethod, headers);
		postMethod.addHeader("Content-type", "application/json; charset=" + charset);
		postMethod.setHeader("Accept", "application/json");
		postMethod.setEntity(new StringEntity(JSON.toJSONString(param), Charset.forName(charset)));
		try {
			CloseableHttpResponse response = sslClientDefault.execute(postMethod);
			return EntityUtils.toString(response.getEntity(), charset);
		} catch (IOException e) {
			logger.info("https请求异常", e);
		}
		return null;
	}
	
}

