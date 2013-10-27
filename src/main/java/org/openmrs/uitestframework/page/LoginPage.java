package org.openmrs.uitestframework.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LoginPage extends AbstractBasePage {
	
	static final String USERNAME_TEXTBOX_ID = "username";
	static final String PASSWORD_TEXTBOX_ID = "password";
	static final String LOGIN_BUTTON_ID = "login-button";
	public static final String LOGIN_PATH = "/login.htm";
    public static final String LOCATIONS_ID = "sessionLocation";
	static final String LOGOUT_PATH = "/logout";
	static final String CLERK_USERNAME = "clerk";
	static final String CLERK_PASSWORD = "Clerk123";
	static final String NURSE_USERNAME = "nurse";
	static final String NURSE_PASSWORD = "Nurse123";
	static final String DOCTOR_USERNAME = "doctor";
	static final String DOCTOR_PASSWORD = "Doctor123";
	
	private String UserName;
	
	private String Password;
	
	public LoginPage(WebDriver driver) {
		super(driver);
		UserName = properties.getUserName();
		Password = properties.getPassword();
	}
	
	public void login(String user, String password) {
		setTextToFieldNoEnter(By.id(USERNAME_TEXTBOX_ID), user);
		setTextToFieldNoEnter(By.id(PASSWORD_TEXTBOX_ID), password);
		WebElement locations = findElement(By.id(LOCATIONS_ID));
		WebElement firstLocation = locations.findElement(By.tagName("li"));	// choose the first location in the list
		firstLocation.click();
		clickOn(By.id(LOGIN_BUTTON_ID));
		findElement(byFromHref(URL_ROOT + LOGOUT_PATH));	// this waits until the Logoff link is present
	}
	
	public void loginAsAdmin() {
		login(UserName, Password);
	}
	
	@Override
	public String expectedUrlPath() {
		return URL_ROOT + LOGIN_PATH;
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
}
