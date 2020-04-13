package automation.testing.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.amazonaws.services.s3.model.ListObjectsRequest;
//import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;

public class HttpClientUtil {
	
public static final int NUMBER_OF_CONNECTIONS_TO_UPLOAD_FILES = 5; 
	
	public static final int NUMBER_OF_TRIES_TO_GET_HTTP_OK_RESPONSE = 3;
	public static final int TIME_BETWEEN_TRIES_TO_GET_HTTP_OK_RESPONSE = 3000; // in msec
	
	private static HttpClient httpClient = null;
	
	private static HttpClient getHttpClient(String cookie) throws KeyManagementException, NoSuchAlgorithmException {
		if(httpClient == null) {
			List<Header> headers = new ArrayList<Header>();
			headers.add(new BasicHeader(HttpHeaders.COOKIE, cookie));
			httpClient = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
		            .setCookieSpec(CookieSpecs.STANDARD).build()).setSSLSocketFactory(getLayeredConnectionSocketFactory()).setDefaultHeaders(headers).build();
		}
		return httpClient;
	}
	
	private static String getHttpClientResponse(HttpRequestBase httpRequestBase, String jsonStringRequest, String cookie, Logger logger) throws Exception {
						
		HttpResponse response = null;
		int counterOfTriesToGetHttpOkResponse = 0;
		
		if(httpRequestBase instanceof HttpPost) {
			StringEntity requestEntity = new StringEntity(jsonStringRequest, ContentType.APPLICATION_JSON);		
			((HttpPost) httpRequestBase).setEntity(requestEntity);
		}
		
		while(counterOfTriesToGetHttpOkResponse < NUMBER_OF_TRIES_TO_GET_HTTP_OK_RESPONSE) {
			response = getHttpClient(cookie).execute(httpRequestBase);
			
			if(response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
				HttpEntity entity = response.getEntity();
				String responseString = EntityUtils.toString(entity, "UTF-8");
				return responseString;
			} else if(response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
				counterOfTriesToGetHttpOkResponse++;
				Thread.sleep(TIME_BETWEEN_TRIES_TO_GET_HTTP_OK_RESPONSE);
				logger.info("Try #"+counterOfTriesToGetHttpOkResponse+" while calling GET REST API return response: "+response.getStatusLine());
				continue;
			} else {
				httpClient = null; // So next time it fails we'll create a new connection, otherwise the connection will get stuck in the 3 failure
				throw new RuntimeException("While calling Rest API service ("+httpRequestBase
						+" ; with parameters: "+jsonStringRequest
						+") got error with status : "+response.getStatusLine());
			}
		}
		
		httpClient = null; // So next time it fails we'll create a new connection, otherwise the connection will get stuck in the 3 failure
		throw new RuntimeException("While calling Rest API service ("+httpRequestBase
				+" ; with parameters: "+jsonStringRequest
				+") got error with status : "+response.getStatusLine());
	}
	
	public static String getPostResponseString(String url, String jsonStringRequest, String cookie, Logger logger) throws Exception {		
		HttpPost request = new HttpPost(url);
		return getPostResponseString(request, jsonStringRequest, cookie, logger);
	}
	
	private static String getPostResponseString(HttpPost request, String jsonStringRequest, String cookie, Logger logger) throws Exception {		
		return getHttpClientResponse(request, jsonStringRequest, cookie, logger);
	}
	
	public static String getGettResponseString(String url, String cookie, Logger logger) throws Exception {		
		HttpGet request = new HttpGet(url);		
		return getHttpClientResponse(request, null, cookie, logger);
	}
	
	public static List<Map<String, Object>> getGettResponseAsList(String url, String cookie, Logger logger) throws Exception {		
		String responseString = getGettResponseString(url, cookie, logger);
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String,Object>> list = mapper.readValue(responseString, List.class);
		return list;
	}
	
	public static File downloadFile(String url, String cookie, String filePathToDownload) throws Exception {
		
		HttpGet request = new HttpGet(url);
		HttpEntity entity = null;			
		InputStream inputStream = null;
        OutputStream outputStream = null;
        File downloaddedFile = null;
		
		try {
			HttpResponse response = getHttpClient(cookie).execute(request);
			
			if(response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
				entity = response.getEntity();			
				inputStream = entity.getContent();
		        outputStream = new FileOutputStream(filePathToDownload);
		        IOUtils.copy(inputStream, outputStream);		        
		        
		        downloaddedFile = new File(filePathToDownload);
		        return downloaddedFile;
			} else {
				return null;
			}
		} catch(Exception e) {
			e.printStackTrace();
			return downloaddedFile;
		} finally {
			if(inputStream != null) {
				inputStream.close();
			}
			if(outputStream != null) {
				outputStream.close();
			}
		}
	}
	
	public static String getPostResponseString(String url, Map<String, String> jsonPostRequestParams, String cookie, Logger logger) throws Exception {		
		return getPostResponseString(url, getJsonMapRequestAsString(jsonPostRequestParams), cookie, logger);
	}
	
	public static Object getValueFromJsonResponse(String jsonResposeString, String jsonKeyName) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue(jsonResposeString, Map.class);
		
		ArrayList<Object> dataArray = (ArrayList<Object>) map.get("data");
		LinkedHashMap<String, Object> mappedJSON = null;
		if(dataArray.size()>0) {
			mappedJSON = (LinkedHashMap<String, Object>) dataArray.get(0); // refer only for the first result got from HttpResponse
		} else {
			return null;
		}
		
		Set<String> keys = mappedJSON.keySet();
		Iterator<String> iterator = keys.iterator();
		String currentKey = null;
		while(iterator.hasNext()) {
			currentKey = (String) iterator.next();
			if(currentKey.equalsIgnoreCase(jsonKeyName))
				return mappedJSON.get(currentKey);
		}
		
		throw new RuntimeException("Couldn't find key named "+jsonKeyName+" in JSON response string: "+jsonResposeString);
	}
	
	private static String getJsonMapRequestAsString(Map<String, String> jsonPostRequestParams) {
		String jsonStringRequest = "{";
		
		// Building the jsonStringRequest with the jsonPostRequestParams
		Set<String> keySet = jsonPostRequestParams.keySet();
		ArrayList<String> listOfKeys = new ArrayList<String>(keySet);
		String currentParamName = null;
		String currentParamValue = null;
		for(int paramsCounter=0; paramsCounter<listOfKeys.size() ; paramsCounter++) {
			currentParamName = listOfKeys.get(paramsCounter);
			jsonStringRequest = jsonStringRequest.concat("\""+currentParamName+"\":");
//				System.out.println("currentParamName: "+currentParamName);
			currentParamValue = jsonPostRequestParams.get(currentParamName);
			jsonStringRequest = jsonStringRequest.concat(currentParamValue+",");
//				System.out.println("currentParamValue: "+currentParamValue);
		}
		jsonStringRequest = jsonStringRequest.substring(0, jsonStringRequest.length()-1); // remove the last ','
		jsonStringRequest = jsonStringRequest.concat("}");
//		System.out.println("jsonStringRequest: "+jsonStringRequest);
		
		return jsonStringRequest;
	}
	
	private static LayeredConnectionSocketFactory getLayeredConnectionSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, new TrustManager[]{new TrustAnyTrustManager()}, new java.security.SecureRandom());
		return new SSLConnectionSocketFactory(sslContext);
	}
	
	public static boolean uploadFile(String url, String fileToUploadPath, 
			String eTag, String dlType, String cookie, int maxNumberOftriesToUploadItem, Logger logger) 
					throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException {

		HttpResponse response = null;
		HttpPost post = null;
		File file = null;
		
		try {
			file = new File(fileToUploadPath);
			
			for(int counter=0 ; counter < maxNumberOftriesToUploadItem ; counter++) {
				post = new HttpPost(url);
				HttpEntity entity = null;
				if(dlType == null) {
					entity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532)
							.addBinaryBody("file", file).addTextBody("eTags", eTag).build();
				} else {
					entity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532)
							.addBinaryBody("file", file).addTextBody("eTags", eTag).addTextBody("dlType", dlType).build();
				}
				
				post.setEntity(entity);
				response = getHttpClient(cookie).execute(post);
				
				if(response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
					logger.info("Try #"+(counter+1)+" to upload item "+fileToUploadPath+" was successful");
					return true;
				} else {
					logger.warn("Try #"+(counter+1)+" to upload item "+fileToUploadPath+" has failed - Server response: "+response.getStatusLine().toString());
				}
			}
			
			return false;
		} finally {
			if(file != null) {
				file.delete();
			}
		}
	}
	
	public static void uploadFiles(String url, String directoryToUploadFilesFrom, 
			String eTag, String dlType, String cookie, int maxNumberOftriesToUploadItem, Logger logger) 
					throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException {
		
		File folder = new File(directoryToUploadFilesFrom);
		if(!folder.exists()) {
			throw new RuntimeException("Failed to find directory named "+ directoryToUploadFilesFrom +" to upload the sample items from");
		}
		
		uploadFiles(url, directoryToUploadFilesFrom, eTag, dlType, cookie, maxNumberOftriesToUploadItem, logger, 0, folder.listFiles().length);
	}
	
	public static void uploadItemsToSystemFromS3(String url, String s3AccessKeyId, String s3SecretKeyId, String s3ClientRegion, String s3BucketName,
			String eTag, String dlType, String lastStation, String cookie, int maxNumberOftriesToUploadItem, Logger logger) 
					throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException {
		
		String currentFullPathObjectKey = null;
		String currentObjectKey = null;
        S3Object currentFullObject = null;
        InputStream currentS3ObjectInputStream = null;
		
		HttpResponse response = null;
		HttpPost post = new HttpPost(url);
		HttpEntity entity = null;
		File tempFile = null;
		MultipartEntityBuilder multipartEntityBuilder = null;
		       
        AmazonS3 s3Client = AmazonS3Util.getS3Client(s3AccessKeyId, s3SecretKeyId, s3ClientRegion);
		
        List<S3ObjectSummary> listOfObjectSummary = AmazonS3Util.getObjectsKeyList(s3Client, s3BucketName, logger);
    	
    	logger.info("Uploading "+listOfObjectSummary.size()+" items from S3 directory with bucket "+s3BucketName);
        
        for(int objectsCounter=0 ; objectsCounter < listOfObjectSummary.size() ; objectsCounter++) {
            	
        	try {
            	currentFullPathObjectKey = listOfObjectSummary.get(objectsCounter).getKey();
            	if(currentFullPathObjectKey.equals(s3BucketName.substring(s3BucketName.indexOf("/")+1)+"/")) {
            		continue;
            	} else {
            		currentObjectKey = currentFullPathObjectKey.substring(currentFullPathObjectKey.lastIndexOf("/")+1);
            	}
            	currentFullObject = s3Client.getObject(new GetObjectRequest(s3BucketName, currentObjectKey));
            	currentS3ObjectInputStream = currentFullObject.getObjectContent();
            	
            	tempFile = new File(currentObjectKey);
        		FileUtils.copyInputStreamToFile(currentS3ObjectInputStream, tempFile);
        		
        		if(tempFile.isDirectory()) {
        			logger.info("Item "+currentObjectKey+" wasn't loaded since it's a directory");
        			continue;
        		}
        	} catch(Exception e) {
        		e.printStackTrace();
				logger.error("Failed to download object "+currentObjectKey+" from S3 region "+s3ClientRegion+" in bucket "+s3BucketName+". Continue to the next object to upload from S3");
				if(currentFullObject != null) {
	            	currentFullObject.close();
	            }
				continue;
        	}
        	
        	try {
            	for(int counterOfTriesToUploadItem=0 ; counterOfTriesToUploadItem < maxNumberOftriesToUploadItem ; counterOfTriesToUploadItem++) {     		
            		
            		multipartEntityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532)
					.addBinaryBody("file", tempFile).addTextBody("eTags", eTag);
            		
            		if((dlType != null) && (dlType.trim().length() > 0)) {
            			multipartEntityBuilder = multipartEntityBuilder.addTextBody("dlType", dlType);
            		}
            		
            		if((lastStation != null) && (lastStation.trim().length() > 0)) {
            			multipartEntityBuilder = multipartEntityBuilder.addTextBody("lastStation", lastStation);
            		}
            		
            		entity = multipartEntityBuilder.build();
            		
					post.setEntity(entity);
					response = getHttpClient(cookie).execute(post);
					
					if(response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
						logger.info("Try #"+(counterOfTriesToUploadItem+1)+" to upload item #"+objectsCounter+" named "+currentObjectKey+" was successful (dlType="+dlType+" & lastStation="+lastStation+" & tagName="+eTag+")");
						tempFile.delete();
						break;
					} else {
						logger.warn("Try #"+(counterOfTriesToUploadItem+1)+" to upload item #"+objectsCounter+" named "+currentObjectKey+" has failed - Server response: "+response.getStatusLine().toString());
					}
				}
        	} catch (Exception e) {
        		e.printStackTrace();
				logger.error("Failed to upload object "+currentS3ObjectInputStream+" from S3 region "+s3ClientRegion+" in bucket "+s3BucketName+" to system. Continue to the next item.");
				if(currentFullObject != null) {
	            	currentFullObject.close();
	            }
				continue;
        	}
        }
	}
	
	public static void uploadFiles(String url, String directoryToUploadFilesFrom, 
			String eTag, String dlType, String cookie, int maxNumberOftriesToUploadItem, Logger logger,
			int fileIndexInDirectoryToStartFrom, int fileIndexInDirectoryToEndWith) 
					throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException {
		
		File folder = new File(directoryToUploadFilesFrom);
		if(!folder.exists()) {
			throw new RuntimeException("Failed to find directory named "+ directoryToUploadFilesFrom +" to upload the sample items from");
		}	
		
		File[] listOfFiles = folder.listFiles();
		HttpResponse response = null;
		HttpPost post = new HttpPost(url);
		HttpEntity entity = null;
		
		for (int i = fileIndexInDirectoryToStartFrom; i < fileIndexInDirectoryToEndWith; i++) {
			String currentFileNameAndPathToUpload = directoryToUploadFilesFrom+"/"+listOfFiles[i].getName();
			try {
				if (listOfFiles[i].isFile()) {
					  for(int counter=0 ; counter < maxNumberOftriesToUploadItem ; counter++) {
							if(dlType == null) {
								entity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532)
										.addBinaryBody("file", listOfFiles[i]).addTextBody("eTags", eTag).build();
							} else {
								entity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532)
										.addBinaryBody("file", listOfFiles[i]).addTextBody("eTags", eTag).addTextBody("dlType", dlType).build();
							}
							
							post.setEntity(entity);
							response = getHttpClient(cookie).execute(post);
							
							if(response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
								logger.info("Try #"+(counter+1)+" to upload item #"+i+" named "+listOfFiles[i]+" was successful");
								break;
							} else {
								logger.warn("Try #"+(counter+1)+" to upload item #"+i+" named "+listOfFiles[i]+" has failed - Server response: "+response.getStatusLine().toString());
							}
						}
				}
			} catch(Exception e) {
				e.printStackTrace();
				logger.error("Failed to upload item "+currentFileNameAndPathToUpload);
				continue;
			}
		}
	}
	
	public static String getCookie() throws Exception {
		WebDriver webDriver = DriverFactory.invokeBrowser("chrome", "browser-drivers", false, "qa/Actual", false, "http://localhost:4444/wd/hub");
		webDriver.get("https://www.amazon.com");
		LoginPage loginPage = null;
		loginPage = PageFactory.initElements(webDriver, LoginPage.class);
		loginPage.execute(webDriver, "name", "pass");
		String cookie = "";
		
		Object[] setOfCookies = webDriver.manage().getCookies().toArray();
		for(int cookiesCounter =0 ; cookiesCounter < setOfCookies.length ; cookiesCounter++) {
			cookie = cookie.concat(((Cookie) setOfCookies[cookiesCounter]).getName()).concat("=");
			cookie = cookie.concat(((Cookie) setOfCookies[cookiesCounter]).getValue()).concat("; ");
		}
		return cookie;
	}
	
}
