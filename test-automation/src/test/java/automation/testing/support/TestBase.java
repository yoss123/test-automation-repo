package automation.testing.support;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

public abstract class TestBase {
	
	public static final String PING_TO_SYSTEM_REST_SERVICE_URL = "/ping";
	
	public static final String IMAGES_REPOSITORY_PATH = "images_repository";
	
	public static final int DEFAULT_NUMBER_OF_ITEM_REPLICATIONS = 1; // namely, no replications for this item
	
	public static final int TIMEOUT_PINGING_TO_SYSTEM_TO_GET_UP = 1000 * 60 * 10; // 10 minutes
	public static final int INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP = 1000 * 20; // 20 seconds
			
	protected static WebDriver webDriver = null;
	protected String restAPICookie = null;
	protected String referencesFilesDirectory = null;
	
	protected String systemName = null;
	protected String restApiUrl = null;
	protected String loginUrl = null;
	
	protected String testerUsername = null;
	protected String testerPassword = null;
	
	protected boolean isLogPerformanceData = false;
	
	// Amazon S3 parameters
	protected String amazonS3clientRegion = null;
	protected String amazonS3accessKeyId = null;
	protected String amazonS3secretKeyId = null;
	protected String amazonS3bucketName = null;	
	
	protected static final Logger logger = LogManager.getLogger();
	
	public static WebDriver getWebDriver() {
		return webDriver;
	}
	
	public static void setWebDriver(WebDriver webDriver) {
		TestBase.webDriver = webDriver;
	}
	
	@BeforeMethod
	protected void beforeTestMethodExecuted(Method method) {
		logger.info("Executing "+method.getName()+" test method");
	}
	
	@Parameters({"browserType","webDriverDirectoryPath","isBrowserHeadless","isRunningRemotely","seleniumGridHubUrl",
		"actualFilesDirectoryToDownloadTo","amazonS3clientRegion","amazonS3accessKeyId","amazonS3secretKeyId","amazonS3bucketName",
		"testerUsername","testerPassword","optionalParam"})
	@BeforeClass
	public void init(String browserType, String webDriverDirectoryPath, boolean isBrowserHeadless, boolean isRunningRemotely, String seleniumGridHubUrl, 
			String actualFilesDirectoryToDownloadTo, String amazonS3clientRegion, String amazonS3accessKeyId, String amazonS3secretKeyId, String amazonS3bucketName,
			String testerUsername, String testerPassword, @Optional String optionalParam) throws Exception {
				
		// Initializing the webDriver for the test
		webDriver = DriverFactory.invokeBrowser(browserType, webDriverDirectoryPath, isBrowserHeadless, actualFilesDirectoryToDownloadTo, isRunningRemotely, seleniumGridHubUrl);		
		
		this.amazonS3clientRegion = amazonS3clientRegion;
		this.amazonS3accessKeyId = amazonS3accessKeyId;
		this.amazonS3secretKeyId = amazonS3secretKeyId;
		this.amazonS3bucketName = amazonS3bucketName;
		
		logger.info("Starting to execute test class "+getClass().getName()+" by user "+testerUsername);		
		
		Assert.assertTrue(loginToSystemAndSetRestAPICookie(loginUrl, testerUsername, testerPassword), "Failed to login with user '"+testerUsername+"' and password '"+testerPassword);
		
	}
	
	/** Login to the system and validate that the DLS server is up. If not then try again after wait time of 1 minute. Timeout for tries is 10 minutes. */
	protected boolean loginToSystemAndSetRestAPICookie(String loginUrl, String testerUsername, String testerPassword) {
		
		LoginPage loginPage = null;
		boolean isSystemUp = false;
		
		for(int currentTimeCount = 0 ; (!isSystemUp) && (currentTimeCount <= TIMEOUT_PINGING_TO_SYSTEM_TO_GET_UP) ;
				currentTimeCount+=INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP) {
			
			// Try to connect to the login page
			try {
				webDriver.get(loginUrl);
			} catch(TimeoutException e) {
				logWarningAndSleepIntervalTime("Failed connecting to login page "+loginUrl+". Will try again in "
								+(INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP/1000)+" sec.", INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP);
				continue;
			}
			
			try {
				loginPage = PageFactory.initElements(webDriver, LoginPage.class);
				loginPage.execute(webDriver, testerUsername, testerPassword);			
				if(!loginPage.isSuccessfulLogin()) {
					logger.warn("Failed to login to system "+this.systemName+" with user name '"+testerUsername+"' and password '"+testerPassword+"' because of wrong user name or password. Failing the test.");
					return false;
				}
			} catch (TimeoutException timeoutException) {
				logWarningAndSleepIntervalTime("Failed to login to system "+this.systemName+" because it wasn't responsive. Will try again in "
						+(INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP/1000)+" sec", INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP);
				continue;
			} catch(Exception e) {
				logWarningAndSleepIntervalTime("Failed to login to system "+this.systemName+" with user name '"
								+testerUsername+"' and password '"+testerPassword+"'",
								INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP);
				continue;
			}
			
			setRestApiCookie(webDriver);
			
			isSystemUp = isSystemUp();
			if(!isSystemUp) {
				logWarningAndSleepIntervalTime("System is down. Will try to ping it again in "+(INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP/1000)
						+" sec. Current time waiting to system to be up is "+(currentTimeCount/1000)+" sec while timeout is "
						+(TIMEOUT_PINGING_TO_SYSTEM_TO_GET_UP/1000)+" sec", INTERVAL_BETWEEN_PINGING_TO_SYSTEM_TO_GET_UP);
				continue;
			} else {
				logger.info("System "+this.systemName+" is up & running !!!");
				return true;
			}			
		}
		
		return false;
	}
	
	private void logWarningAndSleepIntervalTime(String msg2log, int sleepTime) {
		logger.warn(msg2log);
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private boolean isSystemUp() {
		String ping2systemUrl = this.restApiUrl + PING_TO_SYSTEM_REST_SERVICE_URL;
		String responseString = null;
		try {
			responseString = HttpClientUtil.getGettResponseString(ping2systemUrl, this.restAPICookie, logger);
			return ((responseString != null) && responseString.contains("Ping result"));
		} catch (Exception e) {
			logger.info("Ping to system "+this.systemName+" has failed");
			return false;
		}
	}
	
	private void setRestApiCookie(WebDriver webDriver) {
		restAPICookie = "";
		Object[] setOfCookies = webDriver.manage().getCookies().toArray();
		for(int cookiesCounter =0 ; cookiesCounter < setOfCookies.length ; cookiesCounter++) {
			restAPICookie = restAPICookie.concat(((Cookie) setOfCookies[cookiesCounter]).getName()).concat("=");
			restAPICookie = restAPICookie.concat(((Cookie) setOfCookies[cookiesCounter]).getValue()).concat("; ");
		}
	}
			
	@AfterClass
	public void tearDown() {
		
		logger.info("Finished executing test suite");
		
		webDriver.quit();
	}
	
	public static String getStringValueOfToolParameter(String parameterValueFromTestNgXml, String systemPropertyName) {
		String systemPropertyValueAsString = System.getProperty(systemPropertyName); // When running the test from Jenkins then the report file name and path defined there because this flexibility is needed. Otherwise, take it from the test suite xml file
		if(systemPropertyValueAsString != null) {
			logger.info("Set "+systemPropertyName+" as "+systemPropertyValueAsString+" from System.property");
			return systemPropertyValueAsString;
		} else {
			logger.info("Set "+systemPropertyName+" as "+parameterValueFromTestNgXml+" from XML file");
			return parameterValueFromTestNgXml;
		}
	}
	
}