package org.openmrs.uitestframework.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openmrs.uitestframework.test.TestData.checkIfPatientExists;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openmrs.uitestframework.page.LoginPage;
import org.openmrs.uitestframework.page.Page;
import org.openmrs.uitestframework.page.TestProperties;
import org.openmrs.uitestframework.page.exception.PageRejectedException;
import org.openmrs.uitestframework.test.TestData.EncounterInfo;
import org.openmrs.uitestframework.test.TestData.PatientInfo;
import org.openmrs.uitestframework.test.TestData.RoleInfo;
import org.openmrs.uitestframework.test.TestData.TestPatient;
import org.openmrs.uitestframework.test.TestData.UserInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.junit.SauceOnDemandTestWatcher;

/**
 * Superclass for all UI Tests. Contains lots of handy "utilities" needed to setup and tear down
 * tests as well as handy methods needed during tests, such as:
 * <ul>
 *     <li>initialize Selenium WebDriver</li>
 *     <li>create (and delete) test patient, @see {@link #createTestPatient()}</li>
 *     <li>@see {@link #goToLoginPage()}</li>
 *     <li>@see {@link #login()}</li>
 *     <li>@see {@link #assertPage(Page)} - @see {@link #pageContent()}</li>
 * </ul>
 */
@RunWith(Parameterized.class)
public class TestBase implements SauceOnDemandSessionIdProvider {

	public static final int MAX_WAIT_IN_SECONDS = 120;
	public static final int MAX_PAGE_LOAD_IN_SECONDS = 120;
	public static final int MAX_SERVER_STARTUP_IN_MILLISECONDS = 10 * 60 * 1000;
	public static final int MAX_SAUCELAB_COMMAND_TIMEOUT_IN_SECONDS = 600;

	public String sessionId;

	public SauceOnDemandAuthentication sauceLabsAuthentication;

	public String sauceLabsHubUrl;

	@Rule
	public SauceOnDemandTestWatcher sauceLabsResultReportingTestWatcher;

	@Rule
	public TestRule testWatcher = new TestWatcher() {

		@Override
		public void failed(Throwable t, Description test) {
			takeScreenshot(test.getDisplayName().replaceAll("[()]", ""));
		}
	};

	@Rule
	public TestName testName = new TestName();

	protected WebDriver driver;

	protected Page page;

	@Parameterized.Parameter
	public String platform;

	@Parameterized.Parameter(value = 1)
	public String browser;

	@Parameterized.Parameter(value = 2)
	public String browserVersion;

	@Parameterized.Parameters(name = "{index}: {0} {1} {2}")
	public static Collection<Object[]> getBrowsers() {
		return Arrays.asList(new Object[][] {
				{"Linux", "Firefox", "42"},
				{"Linux", "Chrome", "48.0"}
		});
	}

	private static transient boolean serverFailure = false;

	public TestBase() {
		String sauceLabsUsername = TestProperties.instance().getProperty("SAUCELABS_USERNAME", null);
		String sauceLabsAccessKey = TestProperties.instance().getProperty("SAUCELABS_ACCESSKEY", null);
		sauceLabsHubUrl = TestProperties.instance().getProperty("saucelabs.hub.url", "ondemand.saucelabs.com:80");

		if (!StringUtils.isBlank(sauceLabsUsername) && !StringUtils.isBlank(sauceLabsAccessKey)) {
			sauceLabsAuthentication = new SauceOnDemandAuthentication(sauceLabsUsername, sauceLabsAccessKey);
			sauceLabsResultReportingTestWatcher = new SauceOnDemandTestWatcher(this, sauceLabsAuthentication);
		}

	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Before
	public void startWebDriver() throws Exception {
		if (serverFailure) {
			fail("Test killed due to server failure");
		}

		String testMethod = getClass().getSimpleName() + "." + testName.getMethodName();
		if (isRunningOnSauceLabs()) {
			DesiredCapabilities capabilities = new DesiredCapabilities();

			capabilities.setCapability(CapabilityType.BROWSER_NAME, browser);
			capabilities.setCapability(CapabilityType.VERSION, browserVersion);
			capabilities.setCapability(CapabilityType.PLATFORM, platform);

			capabilities.setCapability("name", testMethod);

			capabilities.setCapability("commandTimeout", MAX_SAUCELAB_COMMAND_TIMEOUT_IN_SECONDS);

			String buildNumber = System.getProperty("buildNumber");
			if (!StringUtils.isBlank(buildNumber)) {
				capabilities.setCapability("build", buildNumber);
			}
			
			String saucelabsTunnel = System.getProperty("saucelabsTunnel");
			if (!StringUtils.isBlank(saucelabsTunnel)) {
				capabilities.setCapability("tunnel-identifier", saucelabsTunnel);
			}
			
			String branch = System.getProperty("branch");
			if (!StringUtils.isBlank(branch)) {
				capabilities.setCapability("tags", branch);
			}

			driver = new RemoteWebDriver(new URL("http://" + sauceLabsAuthentication.getUsername() + ":"
			        + sauceLabsAuthentication.getAccessKey() + "@" + sauceLabsHubUrl +"/wd/hub"), capabilities);

			this.sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();
			System.out.println("Running " + testMethod + " at https://saucelabs.com/tests/" + this.sessionId);
		} else {
			System.out.println("Running locally...");
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
		}

		driver.manage().timeouts().implicitlyWait(MAX_WAIT_IN_SECONDS, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(MAX_PAGE_LOAD_IN_SECONDS, TimeUnit.SECONDS);

		long start = System.currentTimeMillis();
		while(true){
			try{
				page = login();
				//wait for loading a page for MAX_PAGE_LOAD_IN_SECONDS + MAX_WAIT_IN_SECONDS
				//and interpret no exception as successful connection
				return;
			} catch(ServerErrorException e) {
				failTest(testMethod, e);
			} catch (PageRejectedException e) {
				failTest(testMethod, e);
			} catch (ProcessingException e) {
				failTest(testMethod, e);
			} catch(Exception e){
				if(System.currentTimeMillis() > start + MAX_SERVER_STARTUP_IN_MILLISECONDS){
					failTest(testMethod, e);
				} else {
					//log that connection timed out, and try again in next iteration
					System.out.println("Failed to login in " + testMethod + ", trying again...");
				}
			}
		}
	}

	private void failTest(String testMethod, Exception e) {
		serverFailure = true;
		System.out.println("Test killed due to server failure in " + testMethod);
		ExceptionUtils.printRootCauseStackTrace(e);
		fail("Test killed due to server failure");
	}

	@After
	public void stopWebDriver() {
		if (driver != null) {
			driver.quit();
		}
	}

	private boolean isRunningOnSauceLabs() {
		return sauceLabsAuthentication != null;
	}

	public Page login() {
		return goToLoginPage().loginAsAdmin();
	}

	public LoginPage goToLoginPage() {
		LoginPage loginPage = getLoginPage();

		Response response = ClientBuilder.newClient()
				.target(TestProperties.instance().getWebAppUrl())
				.path(loginPage.getPageUrl())
				.request().get();

		int status = response.getStatus();

		if(status >= 400 && status <= 599){
			throw new ServerErrorException(response.getStatusInfo().getReasonPhrase(), status);
		}

		loginPage.go();
		loginPage.waitForPage();

		//refresh, just to be sure all css files and images are loaded properly
		driver.navigate().refresh();
		loginPage.waitForPage();

		return loginPage;
	}

	protected LoginPage getLoginPage() {
		return new LoginPage(driver);
	}

	WebDriver setupFirefoxDriver() {
		DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
		desiredCapabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
		driver = new FirefoxDriver(desiredCapabilities);
		return driver;
	}

	WebDriver setupChromeDriver() {
		URL chromedriverExecutable = null;
		ClassLoader classLoader = TestBase.class.getClassLoader();

		String chromedriverExecutableFilename = null;
		if (SystemUtils.IS_OS_MAC_OSX) {
			chromedriverExecutableFilename = "chromedriver";
			chromedriverExecutable = classLoader.getResource("chromedriver/mac/chromedriver");
		} else if (SystemUtils.IS_OS_LINUX) {
			chromedriverExecutableFilename = "chromedriver";
			chromedriverExecutable = classLoader.getResource("chromedriver/linux/chromedriver");
		} else if (SystemUtils.IS_OS_WINDOWS) {
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
					if (!SystemUtils.IS_OS_WINDOWS) {
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
		System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, chromedriverFilesDir + "/chromedriver-"
		        + getClass().getSimpleName() + ".log");
		driver = new ChromeDriver();
		return driver;
	}

	/**
	 * Assert we're on the expected page.
	 *
	 * @param expected page
	 */
	public void assertPage(Page expected) {
		assertTrue(driver.getCurrentUrl().contains(expected.getPageUrl()));
	}

	public void takeScreenshot(String filename) {
		if (!isRunningOnSauceLabs()) {
			File tempFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			try {
				FileUtils.copyFile(tempFile, new File("target/screenshots/" + filename + ".png"));
			}
			catch (IOException e) {}
		}
	}

	/**
	 * Delete the given patient from the various tables that contain portions of a patient's info.
	 *
	 * @param uuid The uuid of the patient to delete.
	 */
	public void deletePatient(String uuid) throws NotFoundException {
		RestClient.delete("patient/" + uuid);
	}

	public PatientInfo createTestPatient(String patientIdentifierTypeName, String source) {
		PatientInfo pi = TestData.generateRandomPatient();
		String uuid = TestData.createPerson(pi);
		pi.identifier = createPatient(uuid, patientIdentifierTypeName, source);
		return pi;
	}

	public PatientInfo createTestPatient() {
		return createTestPatient(TestData.OPENMRS_PATIENT_IDENTIFIER_TYPE, "1");
	}

	/**
	 * @return uuid of created test location tag
     */
	public String createTestLocationTag() {
		JsonNode responseBody = RestClient.post("/locationtag", new TestData.TestLocationTag("TAG" + TestData.randomSuffix(), "TEST TAG"));
		return responseBody != null ? responseBody.get("uuid").asText() : null;
	}

	/**
	 * Create a Patient in the database and return its Patient Identifier. The Patient Identifier is
	 * obtained from the database.
	 *
	 * @param personUuid The person
	 * @param patientIdentifierType The type of Patient Identifier to use
	 * @param source the idgen source to use to generate an identifier
	 * @return The Patient Identifier for the newly created patient
	 */
	public String createPatient(String personUuid, String patientIdentifierType, String source) {
		String patientIdentifier = generatePatientIdentifier(source);
		RestClient.post("patient", new TestPatient(personUuid, patientIdentifier, patientIdentifierType));
		return patientIdentifier;
	}

	private String generatePatientIdentifier(String source) {
		return RestClient.generatePatientIdentifier(source);
	}

	/**
	 * Returns the entire text of the "content" part of the current page
	 *
	 * @return the entire text of the "content" part of the current page
	 */
	public String pageContent() {
		return driver.findElement(By.id("content")).getText();
	}

	public EncounterInfo createTestEncounter(String encounterType, PatientInfo patient) {
		EncounterInfo ei = new EncounterInfo();
		ei.datetime = "2012-01-04"; // arbitrary
		ei.type = TestData.getEncounterTypeUuid(encounterType);
		ei.patient = patient;
		TestData.createEncounter(ei); // sets the uuid
		return ei;
	}

	/**
	 * Create a User in the database with the given Role and return its info.
	 * @param username the username to create
	 * @param role the roles to grant them
	 * @return the user that was created
	 */
	public static UserInfo createUser(String username, RoleInfo role) {
		UserInfo ui = (UserInfo) TestData.generateRandomPerson(new UserInfo());
		TestData.createPerson(ui);
		ui.username = username;
		ui.addRole(role);
		ui.addRole("Privilege Level: Full");
		TestData.createUser(ui);
		return ui;
	}

	public void login(UserInfo user) {
		LoginPage page = getLoginPage();
		assertPage(page);
		page.login(user.username, user.password);
	}

	protected void waitForPatientDeletion(String uuid) throws Exception {
		Long startTime = System.currentTimeMillis();
		while (checkIfPatientExists(uuid)) {
			Thread.sleep(200);
			if (System.currentTimeMillis() - startTime > 30000) {
				throw new TimeoutException("Patient not deleted in expected time");
			}
		}
	}

}
