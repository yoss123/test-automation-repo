package automation.testing.report;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;

import automation.testing.support.ScreenRecorderUtil;

// http://toolsqa.com/selenium-webdriver/testng-listeners/

public class ExtentReportSuiteListener implements ISuiteListener, ITestListener, IInvokedMethodListener {
	
	public static final String SYSTEM_PROPERTY_REPORT_FILE_PATH = "reportFile";
	
//	public static final String TFS_TEST_CASE_URL_PREFIX = "http://obg-tfs1:8080/tfs/HRCM_Collection/DataLifting/_workitems?id=";
	public static final String TEST_CASE_ID_ATTRIBUTE_NAME = "testCaseId=";
	public static final String ASSERT_TRUE_ERROR_MSG_TO_FILTER = "expected [true] but found [false]";
	public static final String ASSERT_NOT_NULL_ERROR_MSG_TO_FILTER = "expected object to not be null";
	public static final int TEST_RESULT_FAIL_STATUS = 2; 
	
	private static ExtentReports reports;
	private static ExtentTest test;
	private String reportsDirectoryName = null;
	private String tfsTestCasePrefix = null;
	private static final Logger logger = LogManager.getLogger();
	
	/**
	 * Invoked before running a TestNG Suite
	 */
	public void onStart(ISuite suite) {
		logger.info("on suite start");
		reports = new ExtentReports();
		String reportFileName = System.getProperty(SYSTEM_PROPERTY_REPORT_FILE_PATH); // When running the test from Jenkins then the report file name and path defined there because this flexibility is needed. Otherwise, take it from the test suite xml file
		if(reportFileName == null) { // If the reportFileName wasn't passed as parameter (for example, when running in Jenkins) then take it from the test suite xml file
			reportFileName = suite.getParameter("reportsDirectoryName")+"/"+suite.getParameter("reportFileName");
		}
		reportsDirectoryName = reportFileName.substring(0, reportFileName.lastIndexOf('/'));
		ExtentHtmlReporter reporter = new ExtentHtmlReporter(reportFileName);
		reports.attachReporter(reporter);

		tfsTestCasePrefix = suite.getParameter("tfsTestCasePrefix");
	}

	/**
	 * Invoked after running a TestNG Suite
	 */
	public void onFinish(ISuite suite) {
		logger.info("on suite finish");
//		reports.removeTest(test);
		reports.flush();
	}	
	
	/**
	 * Invoked after the test class is instantiated and before any configuration method is called
	 */
	public void onTestStart(ITestResult result) {
		try {
			ScreenRecorderUtil.startRecord(result.getMethod().getMethodName());
		} catch(Exception e) {
			e.printStackTrace();
		}			
	}

	public void onTestSuccess(ITestResult result) {		
		test.log(Status.PASS, getTestDescription(result) + getHtmlLinkToTestPlayback());
		try {
			ScreenRecorderUtil.stopRecord();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getHtmlLinkToTestPlayback() {
		return " <a href=\""+ScreenRecorderUtil.getMovieFileName()
				+"\" target=\"_blank\"> (watch playback)</a>";
	}
	
	// the testDesc is in the format of 'test description' + 'testCaseId=XXX'
	// The returned string should be like this: 
	// "<a href=\"http://obg-tfs1:8080/tfs/HRCM_Collection/DataLifting/_workitems?id=7903\">test name assert true</a>"
	private String getTestDescription(ITestResult result) {
		
		String testDescription = result.getMethod().getDescription(); 
		
		if(testDescription == null) {
			return result.getMethod().getMethodName();
		} else {
			if(testDescription.contains(TEST_CASE_ID_ATTRIBUTE_NAME)) {
				String testCaseId = testDescription.substring(testDescription.indexOf(TEST_CASE_ID_ATTRIBUTE_NAME)+TEST_CASE_ID_ATTRIBUTE_NAME.length());
				String testName = testDescription.substring(0, testDescription.indexOf(TEST_CASE_ID_ATTRIBUTE_NAME));
//				logger.info("<a href=\""+TFS_TEST_CASE_URL_PREFIX+testCaseId+"\">"+testName+"</a>");
				return "<a href=\""+tfsTestCasePrefix+testCaseId+"\" target=\"_blank\">"+testName+"</a>";
			} else {
				return testDescription;
			}
		}
	}

	/**
	 * Invoked each time a test fails
	 */
	public void onTestFailure(ITestResult result) {		
		addTestFailureToReport(result);
		try {
			ScreenRecorderUtil.stopRecord();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addTestFailureToReport(ITestResult result) {
		
		String messageToLogOnReport = getTestDescription(result);
		
		logger.warn("on test failure");
						
		// Adding screenshot of the failure
		// replace null with a static method who gets the WebDriver instance
		addScreenshot(null, reportsDirectoryName, result.getMethod().getMethodName() + new SimpleDateFormat("_dd-MM-yyyy_HH-mm-ss").format(new Date()) + ".png");
//		try {			
//			TakesScreenshot ts = (TakesScreenshot) TestBase.getWebDriver();
//			if(ts!=null) {
//				File src = ts.getScreenshotAs(OutputType.FILE);
//				String snapshotFilePath = result.getMethod().getMethodName() + new SimpleDateFormat("_dd-MM-yyyy_HH-mm-ss").format(new Date()) + ".png";
//				FileUtils.copyFile(src, new File(reportsDirectoryName+ "/" + snapshotFilePath));
//				test.addScreenCaptureFromPath(snapshotFilePath);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		// Adding the Stack Trace of the error in case of a failure due to Exception in the code
		if(!(result.getThrowable() instanceof AssertionError)) {
//			test.log(Status.ERROR, result.getThrowable()); // Show the Exception's error on the report
			
			if(messageToLogOnReport.length()>0) {
				messageToLogOnReport += "<br>";
			}
			messageToLogOnReport += "<font color=\"red\">"+result.getThrowable().getMessage()+". For more information view the logs.</font>";
			
			// Also presenting the Exception in the Eclipse console for debuging purposes
			logger.error(result.getThrowable());
		} else if(!result.getThrowable().getMessage().equals(ASSERT_TRUE_ERROR_MSG_TO_FILTER)){
			if(messageToLogOnReport.length()>0) {
				messageToLogOnReport += "<br>";
			}
			messageToLogOnReport += "<font color=\"red\">";
			if(result.getThrowable().getMessage().contains(ASSERT_TRUE_ERROR_MSG_TO_FILTER)) {
				messageToLogOnReport += result.getThrowable().getMessage().substring(0, result.getThrowable().getMessage().indexOf(ASSERT_TRUE_ERROR_MSG_TO_FILTER)); 
			} else if(result.getThrowable().getMessage().contains(ASSERT_NOT_NULL_ERROR_MSG_TO_FILTER)) {
				messageToLogOnReport += result.getThrowable().getMessage().substring(0, result.getThrowable().getMessage().indexOf(ASSERT_NOT_NULL_ERROR_MSG_TO_FILTER)); 
			} else {
				messageToLogOnReport += result.getThrowable().getMessage();
			}
			messageToLogOnReport += ".</font>";
			messageToLogOnReport += "<font color=\"red\"><b> For more information view the logs.</b></font>";
			
//			logger.warn("No Assertion error");			
		}
		
		test.log(Status.FAIL, messageToLogOnReport + getHtmlLinkToTestPlayback());		
	}

	public void onTestSkipped(ITestResult result) {
		try {
			ScreenRecorderUtil.stopRecord();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		try {
			ScreenRecorderUtil.stopRecord();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onStart(ITestContext context) {
		test = reports.createTest(context.getName());
	}

	public void onFinish(ITestContext context) {
		// TODO Auto-generated method stub
	}

	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		// TODO Auto-generated method stub
	}

	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
		if((testResult.getStatus() == TEST_RESULT_FAIL_STATUS) && method.getTestMethod().getMethodName().equals("init")) {
			addTestFailureToReport(testResult);
		}
	}
	
	public static void addScreenshot(WebDriver webDriver, String reportsDirectoryName, String snapshotFilePath) {
		try {
			TakesScreenshot ts = (TakesScreenshot) webDriver;
			if(ts!=null) {
				File src = ts.getScreenshotAs(OutputType.FILE);
				FileUtils.copyFile(src, new File(reportsDirectoryName+ "/" + snapshotFilePath));
				test.addScreenCaptureFromPath(snapshotFilePath);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
