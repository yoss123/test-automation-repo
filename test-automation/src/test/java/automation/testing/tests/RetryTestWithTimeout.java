package automation.testing.tests;

import org.apache.logging.log4j.LogManager;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryTestWithTimeout implements IRetryAnalyzer {
	
	public static final int TIME_ELAPSED_DEFAULT_VALUE = 0;
	public static final int RETRY_COUNTER_DEFAULT_VALUE = 0;
	
	private int timeElapsed = TIME_ELAPSED_DEFAULT_VALUE; // in sec
	private int retryCounter = RETRY_COUNTER_DEFAULT_VALUE;
	private int hashCode = 0;
	
	@Override
	public boolean retry(ITestResult result) {
		
		if(result.getMethod().getInstance().hashCode() != this.hashCode) resetParameters(result.getMethod().getInstance().hashCode());
		
		RetryTestWithTimeoutParameters retryTestWithTimeoutParameters = result.getMethod().getConstructorOrMethod().getMethod()
				 .getAnnotation(RetryTestWithTimeoutParameters.class);
				
		LogManager.getLogger().info("Executing retry #"+this.retryCounter+" of test "
						+result.getMethod().getMethodName()+" ; Time elapsed is "
						+timeElapsed+" sec while timeout is "
						+retryTestWithTimeoutParameters.timeout()+" sec. Next try will be in "
						+retryTestWithTimeoutParameters.intervalBetweenRetries()+" sec ; HashCode="+result.getMethod().getInstance().hashCode());
		
		if((retryTestWithTimeoutParameters != null) && (this.timeElapsed < retryTestWithTimeoutParameters.timeout())) {
			result.getTestContext().getFailedTests().removeResult(result);
			try {
				Thread.sleep(retryTestWithTimeoutParameters.intervalBetweenRetries() * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.timeElapsed += retryTestWithTimeoutParameters.intervalBetweenRetries();
			this.retryCounter++;
			return true;
	    }
	    return false;
	}

	private void resetParameters(int newHashCode) {
		this.timeElapsed = TIME_ELAPSED_DEFAULT_VALUE;
		this.retryCounter = RETRY_COUNTER_DEFAULT_VALUE;
		this.hashCode = newHashCode;
	}
}
