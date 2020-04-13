package automation.testing.support;

import org.apache.logging.log4j.LogManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class WaitUntilExpectedConditionOccured {
	
	public static final long TIME_INTERVAL_TO_VALIDATE_TIMEOUT = 500; // in milliseconds
	
	public static boolean isElementLoadedByXpath(WebDriver webDriver, String xPath, long timeout) {
		long timer = 0;
		String jsToExecute = "return document.evaluate(\""+xPath+"\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue";
		while(timer<timeout) {
			
			if((((JavascriptExecutor) webDriver).executeScript(jsToExecute)) != null) {
				return true;
			} else {
				try {
					Thread.sleep(TIME_INTERVAL_TO_VALIDATE_TIMEOUT);
					timer += TIME_INTERVAL_TO_VALIDATE_TIMEOUT;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		LogManager.getLogger().warn("Wait condition failed. Element with xPath '"+xPath+"' is not loaded (null) by the timeout of "+timeout+" milliseconds");
		return false;
	}
	
	public static boolean isElementLoadedUsingJavaScript(WebDriver webDriver, String jsToExecute, long timeout) {
		long timer = 0;
		while(timer<timeout) {
			
			if((((JavascriptExecutor) webDriver).executeScript(jsToExecute)) != null) {
				return true;
			} else {
				try {
					Thread.sleep(TIME_INTERVAL_TO_VALIDATE_TIMEOUT);
					timer += TIME_INTERVAL_TO_VALIDATE_TIMEOUT;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		LogManager.getLogger().warn("Wait condition failed. Couldn't load element using Java Script '"+jsToExecute+"' is not loaded (null) by the timeout of "+timeout+" milliseconds");
		return false;
	}
	
	public static boolean elementIsLoaded(WebDriver webDriver, long timeout, String elementId) {
		long timer = 0;
		while(timer<timeout) {
			if((((JavascriptExecutor) webDriver).executeScript("return document.getElementById('"+elementId+"')")) != null) {
				return true;
			} else {
				try {
					Thread.sleep(TIME_INTERVAL_TO_VALIDATE_TIMEOUT);
					timer += TIME_INTERVAL_TO_VALIDATE_TIMEOUT;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		LogManager.getLogger().warn("Wait condition failed in "
				+Thread.currentThread().getStackTrace()[2].getClassName() + "." + Thread.currentThread().getStackTrace()[2].getMethodName()
				+". Element with id '"+elementId+"' is not loaded (null) by the timeout of "+timeout+" milliseconds");
		return false;
	}
	
	public static boolean isButtonEnabled(WebDriver webDriver, long timeout, String elementId) throws InterruptedException {
		
		long timer = 0;
		boolean isSaveButtonEnabled = false;
		while(timer<timeout) {
			
			isSaveButtonEnabled = (!(Boolean) ((JavascriptExecutor)webDriver).executeScript("return document.getElementById('"+elementId+"').disabled"));
			if(isSaveButtonEnabled) {
				return true;
			} else {
				Thread.sleep(WaitUntilExpectedConditionOccured.TIME_INTERVAL_TO_VALIDATE_TIMEOUT);
				timer += WaitUntilExpectedConditionOccured.TIME_INTERVAL_TO_VALIDATE_TIMEOUT;
			}
		}
		LogManager.getLogger().warn("Wait condition failed in "
				+Thread.currentThread().getStackTrace()[2].getClassName() + "." + Thread.currentThread().getStackTrace()[2].getMethodName()
				+". Save button with id '"+elementId+"' by the timeout of "+timeout+" milliseconds");
		return false;
	}

}
