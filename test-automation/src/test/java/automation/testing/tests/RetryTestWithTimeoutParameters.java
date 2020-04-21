package automation.testing.tests;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RetryTestWithTimeoutParameters {
	int timeout() default 0; // in sec
	int intervalBetweenRetries() default 0; // In sec
}
