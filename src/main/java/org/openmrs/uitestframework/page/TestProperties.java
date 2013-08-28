package org.openmrs.uitestframework.page;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

/**
 * TODO Comment describe priorities...
 */
public class TestProperties {
	
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
			URL resource = Thread.currentThread().getContextClassLoader().getResource("org/openmrs/uitestframework/test.properties");
			if (resource != null) {
				System.out.println("test.properties found: " + resource.toExternalForm());
				InputStream input = resource.openStream();
				properties.load(new InputStreamReader(input, "UTF-8"));
				System.out.println("properties:");
				System.out.println(properties);
			}
		}
		catch (IOException ioException) {
			throw new RuntimeException("test.properties not found. Error: ", ioException);
		}
	}
	
	public String getWebAppUrl() {
		return getProperty("webapp.url", "http://localhost:8080/openmrs");
	}
	
	public String getUserName() {
		return getProperty("login.username", "admin");
	}
	
	public String getPassword() {
		return getProperty("login.password", "test");
	}
	
	public enum WebDriverType {chrome, firefox};	// only these two for now

	public WebDriverType getWebDriver() {
		try {
	        return WebDriverType.valueOf(getProperty("webdriver", "firefox"));
        }
        catch (IllegalArgumentException e) {
        	return WebDriverType.firefox;
        }
	}
	
	String getProperty(String property, String defaultValue) {
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
