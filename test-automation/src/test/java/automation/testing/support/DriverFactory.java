package automation.testing.support;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.wdm.WebDriverManager;

public class DriverFactory {
	
	
	
	public static final int RECOMMENDED_SCREEN_HEIGHT = 1080;
	public static final int RECOMMENDED_SCREEN_WIDTH = 1920;
	
	public static WebDriver getDriver(String browserType, String webDriverDirectoryPath, boolean isBrowserHeadless, 
			String actualFilesDirectoryToDownloadTo, boolean isRunningRemotely, String seleniumGridHubUrl) 
			throws NoSuchDriverException, MalformedURLException {
		
		WebDriver driver = null;
		
		if(browserType.equalsIgnoreCase(BrowserType.CHROME)) {
			// Replacing the next line chromeDriver set property with dynamic cromeDriver setup using the WebDriverManager
			// System.setProperty("webdriver.chrome.driver", webDriverDirectoryPath+"\\chromedriver.exe");
			
			// The next code is to enable file download in Chrome without the message 'This type of file can harm your computer'
			HashMap<String, Object> driverPrefs = new HashMap<String, Object>();
			driverPrefs.put("profile.default_content_settings.popups", 0);
			driverPrefs.put("safebrowsing.enabled", "true");
			if(actualFilesDirectoryToDownloadTo != null) {
				File fileToDownloadDir = new File(actualFilesDirectoryToDownloadTo);
				String downloadFilepath = fileToDownloadDir.getAbsolutePath();
				driverPrefs.put("download.default_directory", downloadFilepath);
			}
	        ChromeOptions options = new ChromeOptions();
	        
	        if(isBrowserHeadless) {
	        	options.addArguments("headless");
	        }
	        
	        options.setExperimentalOption("prefs", driverPrefs);
	        options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
	        options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
	        options.setCapability(CapabilityType.BROWSER_NAME, BrowserType.CHROME);
	        
	        if(isRunningRemotely) {
	        	driver = new RemoteWebDriver(new URL(seleniumGridHubUrl), options);
	        } else {
	        	WebDriverManager.chromedriver().setup();
	        	driver = new ChromeDriver(options);
	        }
	        
		} else if(browserType.equalsIgnoreCase(BrowserType.FIREFOX)) {
			WebDriverManager.firefoxdriver().setup();
			return new FirefoxDriver();
		} else if(browserType.equalsIgnoreCase(BrowserType.IEXPLORE)) { 
			WebDriverManager.iedriver().setup();
			return new InternetExplorerDriver();
		} else {
			throw new NoSuchDriverException("Driver for browser type "+browserType+" is not available");
		}
		
		driver.manage().window().maximize();	        
        setScreenResolution(driver);            
        return driver;
	}
	
	private static void setScreenResolution(WebDriver webDriver) {

		int originalWindowWidth = webDriver.manage().window().getSize().getWidth();
		int originalWindowHeight = webDriver.manage().window().getSize().getHeight();
		
		if((originalWindowWidth != DriverFactory.RECOMMENDED_SCREEN_WIDTH) || (originalWindowHeight != DriverFactory.RECOMMENDED_SCREEN_HEIGHT)) {
//			System.out.println("Changing screen resolution from "+originalWindowWidth+"*"+originalWindowHeight+" to "
//					+DriverFactory.RECOMMENDED_SCREEN_WIDTH+"*"+DriverFactory.RECOMMENDED_SCREEN_HEIGHT);
			
			webDriver.manage().window().setSize(new Dimension(1920, 1080));
			int changedWindowWidth = webDriver.manage().window().getSize().getWidth();
			int changedWindowHeight = webDriver.manage().window().getSize().getHeight();			
			
			if((changedWindowWidth != DriverFactory.RECOMMENDED_SCREEN_WIDTH) || (changedWindowHeight != DriverFactory.RECOMMENDED_SCREEN_HEIGHT)) {
				// If after changing resolution the resolution is still the same then print a warning
				LogManager.getLogger().warn("Screen resolution is "+changedWindowWidth+"*"+changedWindowHeight+" which is different than the recommended resolution "
						+DriverFactory.RECOMMENDED_SCREEN_WIDTH+"*"+DriverFactory.RECOMMENDED_SCREEN_HEIGHT+". Note that some of the tests may not work properly");
			} else {
				LogManager.getLogger().info("Screen resolution has been changed from "+originalWindowWidth+"*"+originalWindowHeight+" to "
						+DriverFactory.RECOMMENDED_SCREEN_WIDTH+"*"+DriverFactory.RECOMMENDED_SCREEN_HEIGHT);
			}
		}
	}

	public static WebDriver invokeBrowser(String browserType, String webDriverDirectoryPath, boolean isBrowserHeadless, String url, 
			String actualFilesDirectoryToDownloadTo, boolean isRunningRemotely, String seleniumGridHubUrl) throws Exception {
		WebDriver webDriver = DriverFactory.invokeBrowser(browserType, webDriverDirectoryPath, isBrowserHeadless, 
				actualFilesDirectoryToDownloadTo, isRunningRemotely, seleniumGridHubUrl);		
		webDriver.get(url);
		return webDriver;
	}

	public static WebDriver invokeBrowser(String browserType, String webDriverDirectoryPath, boolean isBrowserHeadless,
			String actualFilesDirectoryToDownloadTo, boolean isRunningRemotely, String seleniumGridHubUrl) throws Exception {
		WebDriver webDriver = DriverFactory.getDriver(browserType, webDriverDirectoryPath, isBrowserHeadless, 
				actualFilesDirectoryToDownloadTo, isRunningRemotely, seleniumGridHubUrl);
		webDriver.manage().window().maximize();
		webDriver.manage().deleteAllCookies();
		webDriver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		webDriver.manage().timeouts().pageLoadTimeout(30,TimeUnit.SECONDS);
		return webDriver;
	}
	
	public static void main(String[] args) {
		try {
			WebDriver webDriver = DriverFactory.invokeBrowser("chrome", "browser-drivers", false, "qa/Actual", false, "http://localhost:4444/wd/hub");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}