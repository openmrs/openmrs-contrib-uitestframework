package org.openmrs.uitestdemo.test;

import org.junit.Test;
import org.openmrs.uitestframework.page.LoginPage;
import org.openmrs.uitestframework.test.TestBase;

/**
 * This class demonstrates usage of the UI Test Framework.
 */
public class UiTestFrameworkDemoTest extends TestBase {
	
	@Test
	public void demonstrateUiTest() {
		try {
	        Thread.sleep(2000);
        }
        catch (InterruptedException e) {
        }
		assertPage(new LoginPage(driver));
	}
}
