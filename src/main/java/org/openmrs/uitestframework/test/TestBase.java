package org.openmrs.uitestframework.test;

import static org.dbunit.database.DatabaseConfig.PROPERTY_DATATYPE_FACTORY;
import static org.dbunit.database.DatabaseConfig.PROPERTY_METADATA_HANDLER;
import static org.junit.Assert.assertEquals;
import static org.openmrs.uitestframework.test.TestData.checkIfPatientExists;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.AmbiguousTableNameException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.mysql.MySqlMetadataHandler;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openmrs.uitestframework.page.GenericPage;
import org.openmrs.uitestframework.page.LoginPage;
import org.openmrs.uitestframework.page.Page;
import org.openmrs.uitestframework.page.TestProperties;
import org.openmrs.uitestframework.test.TestData.EncounterInfo;
import org.openmrs.uitestframework.test.TestData.PatientInfo;
import org.openmrs.uitestframework.test.TestData.RoleInfo;
import org.openmrs.uitestframework.test.TestData.TestPatient;
import org.openmrs.uitestframework.test.TestData.TestProvider;
import org.openmrs.uitestframework.test.TestData.UserInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
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
 * tests as well as handy methods needed during tests, such as: - initialize Selenium WebDriver -
 * create (and delete) test patient, @see {@link #createTestPatient()} - @see {@link #currentPage()}
 * - @see {@link #assertPage(Page)} - @see {@link #pageContent()}
 */
public class TestBase implements SauceOnDemandSessionIdProvider {
	
	public String sessionId;
	
	public SauceOnDemandAuthentication sauceLabsAuthentication;
	
	@Rule
	public SauceOnDemandTestWatcher sauceLabsResultReportingTestWatcher;
	
	@ClassRule
	public static TestClassName testClassName = new TestClassName();
	
	@Rule
	public TestName testName = new TestName();
	
	protected LoginPage loginPage;
	
	public TestBase() {
		String sauceLabsUsername = System.getProperty("SAUCELABS_USERNAME");
		String sauceLabsAccessKey = System.getProperty("SAUCELABS_ACCESSKEY");
		
		if (!StringUtils.isBlank(sauceLabsUsername) && !StringUtils.isBlank(sauceLabsAccessKey)) {
			sauceLabsAuthentication = new SauceOnDemandAuthentication(sauceLabsUsername, sauceLabsAccessKey);
			sauceLabsResultReportingTestWatcher = new SauceOnDemandTestWatcher(this, sauceLabsAuthentication);
		}
	}
	
	@Override
	public String getSessionId() {
		return sessionId;
	}
	
	protected WebDriver driver;
	
	protected static IDatabaseTester dbTester;
	
	protected static QueryDataSet deleteDataSet;
	
	protected static QueryDataSet checkDataSet;
	
	public static final String DEFAULT_ROLE = "Privilege Level: Full";
	
	@Before
	public void startWebDriver() throws Exception {
		if (isRunningOnSauceLabs()) {
			System.out.println("Running on SauceLabs...");
			DesiredCapabilities capabilities = new DesiredCapabilities();
			
			capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
			capabilities.setCapability(CapabilityType.VERSION, "45");
			capabilities.setCapability(CapabilityType.PLATFORM, "Linux");
			
			capabilities.setCapability("name", testClassName.getClassName() + "." + testName.getMethodName());
			
			driver = new RemoteWebDriver(new URL("http://" + sauceLabsAuthentication.getUsername() + ":"
			        + sauceLabsAuthentication.getAccessKey() + "@ondemand.saucelabs.com:80/wd/hub"), capabilities);
			this.sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();
			
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
			driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
		}
		
		goToLoginPage();
	}
	
	@After
	public void stopWebDribver() {
		driver.quit();
	}
	
	public void login() {
		loginPage = goToLoginPage();
		loginPage.loginAsAdmin();
	}
	
	public static IDatabaseTester getDbTester() throws Exception {
		if (dbTester == null) {
			initDatabaseConnection();
		}
		return dbTester;
	}
	
	private static void initDatabaseConnection() throws Exception {
		final TestProperties properties = TestProperties.instance();
		dbTester = new JdbcDatabaseTester(
		                                  properties.getDatabaseDriverclass(), properties.getDatabaseConnectionUrl(),
		                                  properties.getDatabaseUsername(), properties.getDatabasePassword(), properties
		                                          .getDatabaseSchema()) {
			
			// A bit of an ugly hack here, due to the fact that DbUnit is really intended for junit3
			// but we're using it in junit4. When you use it with junit3, the getConnection method
			// takes care of the config.setProperty calls for you. (see org.dbunit.DBTestCase.getConnection()
			// and org.dbunit.ext.mysql.MySqlConnection.MySqlConnection(Connection, String).
			@Override
			public IDatabaseConnection getConnection() throws Exception {
				IDatabaseConnection conn = super.getConnection();
				DatabaseConfig config = conn.getConfig();
				config.setProperty(PROPERTY_DATATYPE_FACTORY, new MySqlDataTypeFactory());
				config.setProperty(PROPERTY_METADATA_HANDLER, new MySqlMetadataHandler());
				return conn;
			}
		};
	}
	
	/**
	 * Typically invoked from an @Before method.
	 */
	public void dbUnitSetup() throws Exception {
		getDbTester().setDataSet(dbUnitDataset());
		getDbTester().setSetUpOperation(dbUnitSetUpOperation());
		getDbTester().onSetup();
	}
	
	/**
	 * Override to setup a pre-test dataset.
	 */
	protected IDataSet dbUnitDataset() throws DataSetException {
		// empty dataset
		String inputXml = "<dataset></dataset>";
		IDataSet dataset = new FlatXmlDataSetBuilder().build(new StringReader(inputXml));
		return dataset;
	}
	
	/**
	 * Override to change how DbUnit operates.
	 */
	protected DatabaseOperation dbUnitSetUpOperation() {
		return DatabaseOperation.REFRESH;
	}
	
	/**
	 * Typically invoked from an @After method.
	 */
	
	public void dbUnitTearDown() throws Exception {
		dbUnitTearDownStatic(dbUnitTearDownOperation());
	}
	
	/**
	 * Typically invoked from an @AfterClass method.
	 */
	public static void dbUnitTearDownStatic() throws Exception {
		dbUnitTearDownStatic(DatabaseOperation.DELETE);
	}
	
	public static void dbUnitTearDownStatic(DatabaseOperation op) throws Exception {
		if (deleteDataSet == null) {
			return;
		}
		getDbTester().setDataSet(deleteDataSet);
		//System.out.println("teardown dataset: " + Arrays.asList(getDbTester().getDataSet().getTableNames()));
		getDbTester().setTearDownOperation(op);
		getDbTester().onTearDown();
		deleteDataSet = null;
	}
	
	private boolean isRunningOnSauceLabs() {
		return sauceLabsAuthentication != null;
	}
	
	/**
	 * Override to change how DbUnit operates.
	 */
	protected DatabaseOperation dbUnitTearDownOperation() {
		return DatabaseOperation.DELETE;
	}
	
	protected static QueryDataSet getDeleteDataSet() throws Exception {
		if (deleteDataSet == null) {
			deleteDataSet = newQueryDataSet();
		}
		return deleteDataSet;
	}
	
	protected static QueryDataSet getCheckDataSet() throws Exception {
		if (checkDataSet == null) {
			checkDataSet = newQueryDataSet();
		}
		return checkDataSet;
	}
	
	private static QueryDataSet newQueryDataSet() throws Exception {
		return new QueryDataSet(getDbTester().getConnection());
	}
	
	public LoginPage goToLoginPage() {
		loginPage = new LoginPage(driver);
		loginPage.gotoPage(LoginPage.LOGIN_PATH);
		return loginPage;
	}
	
	// This takes a screen (well, browser) snapshot whenever there's a failure
	// and stores it in a "screenshots" directory.
	@Rule
	public TestRule testWatcher = new TestWatcher() {
		
		@Override
		public void failed(Throwable t, Description test) {
			takeScreenshot(test.getDisplayName().replaceAll("[()]", ""));
		}
	};
	
	WebDriver setupFirefoxDriver() {
		driver = new FirefoxDriver();
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
		        + testClassName.name + ".log");
		driver = new ChromeDriver();
		return driver;
	}
	
	/**
	 * Return a Page that represents the current page, so that all the convenient methods in Page
	 * can be used.
	 * 
	 * @return a Page
	 */
	public Page currentPage() {
		return new GenericPage(driver);
	}
	
	/**
	 * Assert we're on the expected page.
	 * 
	 * @param expected page
	 */
	public void assertPage(Page expected) {
		assertEquals(expected.expectedUrlPath(), currentPage().urlPath());
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
	
	static class TestClassName implements TestRule {
		
		private String name;
		
		@Override
		public Statement apply(Statement statement, Description description) {
			name = description.getTestClass().getSimpleName();
			return statement;
		}
		
        public String getClassName() {
	        return name;
        }
	}
	
	public String patientIdFromUrl() {
		String url = driver.getCurrentUrl();
		return StringUtils.substringAfter(url, "patientId=");
	}
	
	/**
	 * Delete the given patient from the various tables that contain portions of a patient's info.
	 * 
	 * @param uuid The uuid of the patient to delete.
	 */
	public void deletePatient(String uuid) throws NotFoundException {
		RestClient.delete("patient/" + uuid);
	}
	
	/**
	 * Delete the given user from the various tables that contain portions of a user's info.
	 * 
	 * @param user The database user info, especially the user_id and person_id.
	 */
	public static void deleteUser(UserInfo user) throws Exception {
		// See org.openmrs.module.mirebalais.smoke.helper.UserDatabaseHandler.addUserForDelete(String) for more details.
		String userid = user.userId;
		String personid = user.id;
		QueryDataSet dataSet = getDeleteDataSet();
		addSimpleQuery(dataSet, "person", "person_id", personid);
		addSimpleQuery(dataSet, "provider", "person_id", personid);
		addSimpleQuery(dataSet, "person_name", "person_id", personid);
		addSimpleQuery(dataSet, "person_address", "person_id", personid);
		dataSet.addTable(
		    "name_phonetics",
		    formatQuery(
		        "select * from name_phonetics where person_name_id in (select person_name_id from person_name where person_id = %s)",
		        personid));
		addSimpleQuery(dataSet, "person_attribute", "person_id", personid);
		addSimpleQuery(dataSet, "users", "user_id", userid);
		addSimpleQuery(dataSet, "user_role", "user_id", userid);
		addSimpleQuery(dataSet, "user_property", "user_id", userid);
		dbUnitTearDownStatic();
	}
	
	/**
	 * Delete the given role from the role table, if it was created by this framework.
	 * 
	 * @param user The database role info.
	 */
	public static void deleteRole(RoleInfo role) throws Exception {
		if (!role.created) {
			return;
		}
		QueryDataSet dataSet = getDeleteDataSet();
		addSimpleQuery(dataSet, "role", "uuid", '"' + role.uuid + '"');
		dbUnitTearDownStatic();
	}
	
	static void addSimpleQuery(QueryDataSet dataSet, String tableName, String columnName, String id)
	        throws AmbiguousTableNameException {
		String query = formatQuery(simpleQuery(tableName, columnName), id);
		//System.out.println("addSimpleQuery: " + tableName + " " + query);		
		dataSet.addTable(tableName, query);
	}
	
	static String simpleQuery(String tableName, String columnName) {
		return "select * from " + tableName + " where " + columnName + " = %s";
	}
	
	static String formatQuery(String query, String id) {
		return String.format(query, id);
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
	 * Create a Patient in the database and return its Patient Identifier. The Patient Identifier is
	 * obtained from the database.
	 * 
	 * @param personUuid The person
	 * @param patientIdentifierType The type of Patient Identifier to use
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
	 */
	public static UserInfo createUser(String username, RoleInfo role) {
		UserInfo ui = (UserInfo) TestData.generateRandomPerson(new UserInfo());
		TestData.createPerson(ui);
		ui.username = username;
		ui.addRole(role);
		ui.addRole(DEFAULT_ROLE);
		TestData.createUser(ui);
		return ui;
	}
	
	/**
	 * Create a User and Provider in the database with the given role and provider-role and return
	 * its info.
	 */
	public static UserInfo createUser(String username, RoleInfo role, String providerRole) {
		return createUser(username, role, providerRole, null);
	}
	
	/**
	 * Create a User and Provider in the database with the given role and provider-role and return
	 * its info.
	 */
	public static UserInfo createUser(String username, RoleInfo role, String providerRole, String locale) {
		UserInfo ui = (UserInfo) TestData.generateRandomPerson(new UserInfo());
		ui.locale = locale;
		TestData.createPerson(ui);
		ui.username = username;
		ui.addRole(role);
		ui.addRole(DEFAULT_ROLE);
		TestData.createUser(ui);
		// create provider
		String providerUuid = (new TestProvider(ui.uuid, ui.givenName)).create();
		try {
			// Hack/workaround the fact that we cannot use REST to set the provider_role_id in the provider table.
			// This is the only place where we use DbUnit/JDBC directly during test setup, everywhere
			// else we use REST.
			String providerId = TestData.getId("provider", providerUuid);
			String xmlds = "<dataset>" + "<provider " + "provider_id='" + providerId + "' uuid='" + providerUuid
			        + "' provider_role_id='" + getProviderRoleId(providerRole) + "'/>" + "</dataset>";
			FlatXmlDataSet ds = new FlatXmlDataSetBuilder().build(new StringReader(xmlds));
			getDbTester().setDataSet(ds);
			getDbTester().setSetUpOperation(DatabaseOperation.UPDATE);
			getDbTester().onSetup();
		}
		catch (DataSetException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return ui;
	}
	
	// Part of the above hack to workaround lack of REST support for provider role.
	private static Integer getProviderRoleId(String providerRoleName) throws Exception {
		QueryDataSet ds = newQueryDataSet();
		ds.addTable("providermanagement_provider_role", "select * from providermanagement_provider_role where name = '"
		        + providerRoleName + "'");
		ITable providerRole = ds.getTable("providermanagement_provider_role");
		return (Integer) providerRole.getValue(0, "provider_role_id");
	}
	
	public static RoleInfo findOrCreateRole(String name) {
		RoleInfo ri = new RoleInfo(name);
		String uuid = TestData.getRoleUuid(name);
		if (uuid == null) {
			TestData.createRole(ri);
			ri.created = true;
		} else {
			ri.uuid = uuid;
			ri.created = false;
		}
		return ri;
	}
	
	public void login(UserInfo user) {
		LoginPage page = new LoginPage(driver);
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
