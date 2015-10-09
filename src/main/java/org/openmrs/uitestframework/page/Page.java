package org.openmrs.uitestframework.page;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * A web page.
 */
public interface Page {

	void goToPage(String address);

	WebElement findElement(By by);
	
	WebElement findElementById(String id);
	
	List<WebElement> findElements(By by);
	
	String getText(By by);
	
	void setText(By by, String text);
	
	void setText(String id, String text);
	
	void setTextToFieldInsideSpan(String spanId, String text);
	
	void clickOn(By by);
	
	void hoverOn(By by);

	/**
	 * Select an item from a drop down list.
	 * 
	 * @param by The drop down list.
	 * @param value The text of the desired item from the list.
	 */
    void selectFrom(By by, String value);

	/**
	 * Return the actual title of the page.
	 * 
	 * @return the page title.
	 */
	String title();

	/**
	 * Return the path portion of the page's current URL.
	 * 
	 * @return the path portion of the URL.
	 */
	String urlPath();
	
	/**
	 * Real pages supply their expected URL path.
	 * 
	 * @return The path portion of the url of the page.
	 */
	String expectedUrlPath();

	/**
	 * Same as setText, but does not send an ENTER key after.
	 */
	void setTextToFieldNoEnter(By textFieldId, String text);
	
	/**
	 * Wait for the given element to be visible. The default wait time is 5 seconds.
	 * 
	 * @param by
	 */
	void waitForElement(By by);
	
	/**
	 * Wait for text to be present in the given element. The default wait time is 5 seconds.
	 * 
	 * @param by
	 */
	void waitForTextToBePresentInElement(By by, String text);
	
	/**
	 * Return true if the page contains the given text anywhere on the page.
	 * 
	 * @param text
	 * @return true if the page contains the given text anywhere on the page.
	 */
	Boolean containsText(String text);

	void waitForPageToBeReady(boolean domOnly);
}