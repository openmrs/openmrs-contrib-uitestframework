package org.openmrs.uitestframework.page;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

/**
 * Exposes test properties. This class is typically used like this:
 *   TestProperties properties = TestProperties.instance();
 *   
 * Properties are obtained using the following order of lookup:
 *   1. Java system property (System.getProperty). This allows for command line -D setting.
 *   2. OS environment variable (System.getenv). 
 *   3. test.properties file found on the classpath under org/openmrs/uitestframework/
 *   4. The hard-wired defaults in this class.
 *  Also note that test.properties can be "filled in" with properties from pom.xml
 *  by using ${}, e.g. 
 *     in test.properties:
 *       webapp.url=${webapp.url}
 *     in pom.xml:
 *       <properties>
 *          <webapp.url>http://localhost:8080/openmrs</webapp.url>
 *       </properties>
 */
public class TestProperties {
	
	public static final String WEBDRIVER_PROPERTY = "webdriver";
	
	public static final String DEFAULT_WEBDRIVER = "firefox";
	
	public static final String LOGIN_PASSWORD_PROPERTY = "login.password";
	
	public static final String DEFAULT_PASSWORD = "test";
	
	public static final String LOGIN_USERNAME_PROPERTY = "login.username";
	
	public static final String DEFAULT_LOGIN_USERNAME = "admin";
	
	public static final String WEBAPP_URL_PROPERTY = "webapp.url";
	
	public static final String DEFAULT_WEBAPP_URL = "http://localhost:8080/openmrs";
	
	public static final String DATABASE_DRIVERCLASS_PROPERTY = "jdbc.driverclass";
	
	public static final String DEFAULT_DATABASE_DRIVERCLASS = "com.mysql.jdbc.Driver";
	
	public static final String DATABASE_CONNECTIONURL_PROPERTY = "jdbc.connectionurl";
	
	public static final String DEFAULT_DATABASE_CONNECTIONURL = "jdbc:mysql://localhost:3306/openmrs";
	
	public static final String DATABASE_USERNAME_PROPERTY = "database.username";
	
	public static final String DEFAULT_DATABASE_USERNAME = "test";
	
	public static final String DATABASE_PASSWORD_PROPERTY = "database.password";
	
	public static final String DEFAULT_DATABASE_PASSWORD = "test";
	
	public static final String DATABASE_SCHEMA_PROPERTY = "database.schema";
	
	public static final String DEFAULT_DATABASE_SCHEMA = "openmrs";
	
	private static TestProperties SINGLETON;
	
	private Properties properties;
	
	public static TestProperties instance() {
		if (SINGLETON == null) {
			SINGLETON = new TestProperties();
		}
		return SINGLETON;
	}
	
	public TestProperties() {
		properties = new Properties();
		try {
			URL resource = Thread.currentThread().getContextClassLoader()
			        .getResource("org/openmrs/uitestframework/test.properties");
			if (resource != null) {
				System.out.println("test.properties found: " + resource.toExternalForm());
				InputStream input = resource.openStream();
				properties.load(new InputStreamReader(input, "UTF-8"));
				System.out.println("test.properties:");
				System.out.println(properties);
			}
		}
		catch (IOException ioException) {
			throw new RuntimeException("test.properties not found. Error: ", ioException);
		}
		System.out.println(WEBAPP_URL_PROPERTY + ": " + getWebAppUrl());
		System.out.println(LOGIN_USERNAME_PROPERTY + ": " + getUserName());
		System.out.println(LOGIN_PASSWORD_PROPERTY + ": " + getPassword());
		System.out.println(WEBDRIVER_PROPERTY + ": " + getWebDriver());
		System.out.println(DATABASE_USERNAME_PROPERTY + ": " + getDatabaseUsername());
		System.out.println(DATABASE_PASSWORD_PROPERTY + ": " + getDatabasePassword());
	}
	
	public String getWebAppUrl() {
		return getProperty(WEBAPP_URL_PROPERTY, DEFAULT_WEBAPP_URL);
	}
	
	public String getUserName() {
		return getProperty(LOGIN_USERNAME_PROPERTY, DEFAULT_LOGIN_USERNAME);
	}
	
	public String getPassword() {
		return getProperty(LOGIN_PASSWORD_PROPERTY, DEFAULT_PASSWORD);
	}
	
	public enum WebDriverType {
		chrome, firefox
	}; // only these two for now
	
	public WebDriverType getWebDriver() {
		try {
			return WebDriverType.valueOf(getProperty(WEBDRIVER_PROPERTY, DEFAULT_WEBDRIVER));
		}
		catch (IllegalArgumentException e) {
			return WebDriverType.firefox;
		}
	}
	
	public String getDatabaseUsername() {
		return getProperty(DATABASE_USERNAME_PROPERTY, DEFAULT_DATABASE_USERNAME);
	}
	
	public String getDatabasePassword() {
		return getProperty(DATABASE_PASSWORD_PROPERTY, DEFAULT_DATABASE_PASSWORD);
	}
	
	public String getDatabaseDriverclass() {
		return getProperty(DATABASE_DRIVERCLASS_PROPERTY, DEFAULT_DATABASE_DRIVERCLASS);
	}

	public String getDatabaseConnectionUrl() {
		return getProperty(DATABASE_CONNECTIONURL_PROPERTY, DEFAULT_DATABASE_CONNECTIONURL);
	}
	
	public String getDatabaseSchema() {
		return getProperty(DATABASE_SCHEMA_PROPERTY, DEFAULT_DATABASE_SCHEMA);
	}
	
	public String getProperty(String property, String defaultValue) {
		String value = System.getProperty(property);
		if (value == null) {
			value = System.getenv(property);
		}
		if (value == null) {
			value = properties.getProperty(property);
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}
}
