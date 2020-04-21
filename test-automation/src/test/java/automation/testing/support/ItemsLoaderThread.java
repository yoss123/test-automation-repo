package automation.testing.support;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import automation.testing.pages.LoginPage;

public class ItemsLoaderThread extends Thread {
	
	int fileIndexInDirectoryToStartFrom;
	int fileIndexInDirectoryToEndWith;
	
	String url;
	String directoryToUploadFilesFrom; 
	String eTag;
	String dlType;
	String restAPICookie;
	int maxNumberOftriesToUploadItem;
	Logger logger;
	
	public ItemsLoaderThread(int fileIndexInDirectoryToStartFrom, int fileIndexInDirectoryToEndWith, String url,
			String directoryToUploadFilesFrom, String eTag, String dlType, String restAPICookie,
			int maxNumberOftriesToUploadItem, Logger logger) {
		super();
		this.fileIndexInDirectoryToStartFrom = fileIndexInDirectoryToStartFrom;
		this.fileIndexInDirectoryToEndWith = fileIndexInDirectoryToEndWith;
		this.url = url;
		this.directoryToUploadFilesFrom = directoryToUploadFilesFrom;
		this.eTag = eTag;
		this.dlType = dlType;
		this.restAPICookie = restAPICookie;
		this.maxNumberOftriesToUploadItem = maxNumberOftriesToUploadItem;
		this.logger = logger;
	}

	public void run(){  
		try {
				HttpClientUtil.uploadFiles(url, directoryToUploadFilesFrom,
						eTag, null, restAPICookie, maxNumberOftriesToUploadItem, logger, fileIndexInDirectoryToStartFrom, fileIndexInDirectoryToEndWith);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		String restAPICookie = "";
		
		try {
			WebDriver webDriver = DriverFactory.invokeBrowser("chrome", "browser-drivers", false, "itemsSampleTest/actual", false ,"http://localhost:4444/wd/hub");
			webDriver.get("https://www.amazon.com");
			LoginPage loginPage = PageFactory.initElements(webDriver, LoginPage.class);
			loginPage.execute(webDriver, "name", "pass");
			
			Object[] setOfCookies = webDriver.manage().getCookies().toArray();
			for(int cookiesCounter =0 ; cookiesCounter < setOfCookies.length ; cookiesCounter++) {
				restAPICookie = restAPICookie.concat(((Cookie) setOfCookies[cookiesCounter]).getName()).concat("=");
				restAPICookie = restAPICookie.concat(((Cookie) setOfCookies[cookiesCounter]).getValue()).concat("; ");
			}
			
			// TODO: do here something
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
