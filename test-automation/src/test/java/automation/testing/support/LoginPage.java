package automation.testing.support;

import org.apache.logging.log4j.LogManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

public class LoginPage {
	
	private boolean isSuccessfulLogin = false;

	@Test
	public void execute(WebDriver webDriver, String user, String pass) throws Exception {
		
		WebDriverWait wait = new WebDriverWait(webDriver, 5);
		wait.until(ExpectedConditions.elementToBeClickable((webDriver.findElement(By.id("UserName")))));
		WebElement userName = webDriver.findElement(By.id("UserName"));
		userName.sendKeys(Keys.CONTROL+"a");
		Thread.sleep(100);
		userName.sendKeys(Keys.DELETE);
		Thread.sleep(100);
		userName.sendKeys(user);
		
		WebElement password = webDriver.findElement(By.id("Password"));
		password.sendKeys(Keys.CONTROL+"a");
		Thread.sleep(100);
		password.sendKeys(Keys.DELETE);
		Thread.sleep(100);
		password.sendKeys(pass);
		
		WebElement submitBtn = webDriver.findElement(By.xpath(("//input[@type='submit']")));
		Actions actions = new Actions(webDriver);
		actions.moveToElement(submitBtn).click().perform();
		
		// Checking if Login successfully
		WebDriverWait waitForSuccess = new WebDriverWait(webDriver, 5);
		try {
//			waitForSuccess.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("systenInfoDisplay"))));
			waitForSuccess.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.xpath("//div[@id='header']//div[@id='mainMenu']"))));
		} catch (Exception e) {
			LogManager.getLogger().error(e);
			setSuccessfulLogin(false);
			throw new RuntimeException("Couldn't login to the system with user '"+user+"' using password '"+pass+"'");
		}
		setSuccessfulLogin(true);		
	}

	public boolean isSuccessfulLogin() {
		return isSuccessfulLogin;
	}

	public void setSuccessfulLogin(boolean isSuccessfulLogin) {
		this.isSuccessfulLogin = isSuccessfulLogin;
	}

}
