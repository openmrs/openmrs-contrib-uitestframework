package org.openmrs.uitestframework.page;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class LoginPage extends Page {

	public static final String LOGIN_PATH = "/login.htm";

	static final By USERNAME = By.id("username");
	static final By PASSWORD = By.id("password");
	static final By LOGIN = By.id("loginButton");
	static final By LOCATIONS = By.cssSelector("#sessionLocation li");

	static final String LOGOUT_PATH = "/logout";
	static final String CLERK_USERNAME = "clerk";
	static final String CLERK_PASSWORD = "Clerk123";
	static final String NURSE_USERNAME = "nurse";
	static final String NURSE_PASSWORD = "Nurse123";
	static final String DOCTOR_USERNAME = "doctor";
	static final String DOCTOR_PASSWORD = "Doctor123";
	static final String SYSADMIN_USERNAME = "sysadmin";
	static final String SYSADMIN_PASSWORD = "Sysadmin123";

	private String username;

	private String password;

	public LoginPage(WebDriver driver) {
		super(driver);
		username = properties.getUsername();
		password = properties.getPassword();
	}

	public void login(String user, String password, Integer location) {
		postLoginForm(user, password, location);

		findElement(byFromHref(getServerUrl() + LOGIN_PATH)); // this waits until the log off link is present
	}

	private void postLoginForm(String user, String password, Integer location) {
	    String postJs;
		InputStream in = null;
		try {
			in = getClass().getResourceAsStream("/post.js");
	        postJs = IOUtils.toString(in);
	        in.close();
        } catch (IOException e) {
	        throw new RuntimeException(e);
        } finally {
        	IOUtils.closeQuietly(in);
        }

		String post = postJs + " post('" + getAbsolutePageUrl() +"', {username: '" + user + "', password: '" + password;
		if (location != null) {
			post += "', sessionLocation: " + location + "});";
		} else {
			post += "});";
		}
		((JavascriptExecutor) driver).executeScript(post);
    }

	public void login(String user, String password) {
		login(user, password, 1);
	}

	public void loginAsAdmin() {
		login(username, password);
	}

	@Override
	public String getPageUrl() {
		return LOGIN_PATH;
	}

	public void loginAsClerk() {
	    login(CLERK_USERNAME, CLERK_PASSWORD);
    }

	public void loginAsNurse() {
		login(NURSE_USERNAME, NURSE_PASSWORD);
    }

	public void loginAsDoctor() {
		login(DOCTOR_USERNAME, DOCTOR_PASSWORD);
    }

	public void loginAsSysadmin() {
		login(SYSADMIN_USERNAME, SYSADMIN_PASSWORD);
    }
}
