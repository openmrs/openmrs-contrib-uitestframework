package org.openmrs.uitestframework.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openmrs.uitestframework.page.GenericPage;
import org.openmrs.uitestframework.page.LoginPage;
import org.openmrs.uitestframework.page.Page;
import org.openmrs.uitestframework.page.TestProperties;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;

public class TestBase {
	
    protected static WebDriver driver;

    @BeforeClass
    public static void startWebDriver() {
        final TestProperties properties = TestProperties.instance();
        final TestProperties.WebDriverType webDriverType = properties.getWebDriver();
        switch (webDriverType) {
			case chrome:
				driver = setupChromeDriver();
				break;
			case firefox:
				driver = setupFirefoxDriver();
				break;
			default:
				// shrug, choose chrome as default
				driver = setupChromeDriver();
				break;
		}
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        goToLoginPage();	// TODO is this right? do we always want to go to the start page?
    }

    @AfterClass
    public static void stopWebDriver() {
        driver.quit();
    }
    
    public static void goToLoginPage() {
    	currentPage().gotoPage(LoginPage.LOGIN_PATH);
    }
    
    // This takes a screen (well, browser) snapshot whenever there's a failure
    // and stores it in a "screenshots" directory.
    @Rule
    public TestRule testWatcher = new TestWatcher() {
        @Override
        public void failed(Throwable t, Description test) {
            takeScreenshot(test.getDisplayName());
        }
    };
    
    static WebDriver setupFirefoxDriver() {
    	driver = new FirefoxDriver();
    	return driver;
    }

    static WebDriver setupChromeDriver() {
        URL chromedriverExecutable = null;
        ClassLoader classLoader = TestBase.class.getClassLoader();

        String chromedriverExecutableFilename = null;
        if(SystemUtils.IS_OS_MAC_OSX) {
        	chromedriverExecutableFilename = "chromedriver";
            chromedriverExecutable = classLoader.getResource("chromedriver/mac/chromedriver");
        } else if(SystemUtils.IS_OS_LINUX) {
        	chromedriverExecutableFilename = "chromedriver";
            chromedriverExecutable = classLoader.getResource("chromedriver/linux/chromedriver");
        } else if(SystemUtils.IS_OS_WINDOWS) {
        	chromedriverExecutableFilename = "chromedriver.exe";
            chromedriverExecutable = classLoader.getResource("chromedriver/windows/chromedriver.exe");
        }
        String errmsg = "cannot find chromedriver executable";
        String chromedriverExecutablePath = null;
		if (chromedriverExecutable == null) {
			System.err.println(errmsg);
        	Assert.fail(errmsg);
        } else {
        	chromedriverExecutablePath = chromedriverExecutable.getPath();
        	// This ugly bit checks to see if the chromedriver file is inside a jar, and if so
        	// uses VFS to extract it to a temp directory. 
        	if (chromedriverExecutablePath.contains(".jar!")) {
                FileObject chromedriver_vfs;
                try {
	                chromedriver_vfs = VFS.getManager().resolveFile(chromedriverExecutable.toExternalForm());
	                File chromedriver_fs = new File(FileUtils.getTempDirectory(), chromedriverExecutableFilename);
					FileObject chromedriverUnzipped = VFS.getManager().toFileObject(chromedriver_fs);
					chromedriverUnzipped.delete();
					chromedriverUnzipped.copyFrom(chromedriver_vfs, new AllFileSelector());
					chromedriverExecutablePath = chromedriver_fs.getPath();
					if (! SystemUtils.IS_OS_WINDOWS) {
						chromedriver_fs.setExecutable(true);
					}
                }
                catch (FileSystemException e) {
                	System.err.println(errmsg + ": " + e);
                	e.printStackTrace();
                	Assert.fail(errmsg + ": " + e);
                }
        	}
        }
        System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, chromedriverExecutablePath);
        String chromedriverFilesDir = "target/chromedriverlogs";
        try {
	        FileUtils.forceMkdir(new File(chromedriverFilesDir));
        }
        catch (IOException e) {
        	e.printStackTrace();
        }
        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, chromedriverFilesDir + "/chromedriver-" + TestClassName.name + ".log");
        driver = new ChromeDriver();
        return driver;
    }

    /**
     * Return a Page that represents the current page, so that all the convenient
     * methods in Page can be used.
     * 
     * @return a Page
     */
    public static Page currentPage() {
    	return new GenericPage(driver);
    }

    /**
     * Assert we're on the expected page. For now, it just checks the <title> tag.
     * 
     * @param expected page
     */
	public void assertPage(Page expected) {
	    assertEquals(expected.expectedUrlPath(), currentPage().urlPath());
    }

	public void takeScreenshot(String filename) {
	    File tempFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
	    try {
	        FileUtils.copyFile(tempFile, new File("target/screenshots/" + filename + ".png"));
	    } catch (IOException e) {
	    }
    }

	// This junit cleverness picks up the name of the test class, to be used in the chromedriver log file name.
	@ClassRule 
	public static TestClassName TestClassName = new TestClassName();
	static class TestClassName implements TestRule {
		public String name;
		@Override
	    public Statement apply(Statement statement, Description description) {
			name = description.getTestClass().getSimpleName();
		    return statement;
	    }
	}

}
