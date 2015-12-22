package org.openmrs.uitestframework.page;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.uitestframework.test.TestBase;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * A superclass for "real" pages. Has lots of handy methods for accessing
 * elements, clicking, filling fields. etc.
 */
public abstract class Page {

    public final String URL_ROOT;

    protected TestProperties properties = TestProperties.instance();
    protected WebDriver driver;
    private String serverURL;
    protected WebDriverWait waiter;

    private final ExpectedCondition<Boolean> pageReady = new ExpectedCondition<Boolean>() {
        public Boolean apply(WebDriver driver) {
        	Object readyState = executeScript("return document.readyState;");

        	if (hasPageReadyIndicator()) {
        		return "complete".equals(readyState) && Boolean.TRUE.equals(executeScript("return (typeof pageReady !== 'undefined') ? pageReady : null;"));
        	} else {
        		return "complete".equals(readyState);
        	}

        }
    };

    public Page(WebDriver driver) {
        this.driver = driver;
        serverURL = properties.getWebAppUrl();
        URL_ROOT = "/" + StringUtils.substringAfterLast(serverURL, "/");
        waiter = new WebDriverWait(driver, TestBase.MAX_WAIT_SECONDS);
    }

    /**
     * Override to return true, if a page has the 'pageReady' JavaScript variable.
     *
     * It is called by {@link #loadPage()} to determine, if it should wait for the variable to return true.
     *
     * @return true if the page has pageReady indicator, false by default
     */
    public boolean hasPageReadyIndicator() {
    	return false;
    }

    public Object executeScript(String script) {
    	return ((JavascriptExecutor) driver).executeScript(script);
    }

    public void waitForPage() {
    	waiter.until(pageReady);
	}

    public void goToPage(String address) {
        driver.get(serverURL + address);

        waitForPage();
    }

    public void go() {
        driver.get(StringUtils.removeEnd(serverURL, URL_ROOT) + expectedUrlPath());

        waitForPage();
    }

    public WebElement findElement(By by) {
    	waitForPage();
        waiter.until(ExpectedConditions.visibilityOfElementLocated(by));

        return driver.findElement(by);
    }

    public WebElement findElementById(String id) {
        return findElement(By.id(id));
    }

    public String getText(By by) {
        return findElement(by).getText();
    }

    public void setText(By by, String text) {
        setText(findElement(by), text);
    }

    public void setText(String id, String text) {
        setText(findElement(By.id(id)), text);
    }

    public void setTextToFieldNoEnter(By by, String text) {
        setTextNoEnter(findElement(by), text);
    }

    public void setTextToFieldInsideSpan(String spanId, String text) {
        setText(findTextFieldInsideSpan(spanId), text);
    }

    private void setText(WebElement element, String text) {
        setTextNoEnter(element, text);
        element.sendKeys(Keys.RETURN);
    }

    private void setTextNoEnter(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }

    public void clickOn(By by) {
        findElement(by).click();
    }

    public void selectFrom(By by, String value){
        Select droplist = new Select(findElement(by));
        droplist.selectByVisibleText(value);
    }

    public void hoverOn(By by) {
        Actions builder = new Actions(driver);
        Actions hover = builder.moveToElement(findElement(by));
        hover.perform();
    }

    private WebElement findTextFieldInsideSpan(String spanId) {
        return findElementById(spanId).findElement(By.tagName("input"));
    }

    public String title() {
        return getText(By.tagName("title"));
    }

    public String urlPath() {
        try {
            return new URL(driver.getCurrentUrl()).getPath();
        }
        catch (MalformedURLException e) {
            return null;
        }
    }

    public List<WebElement> findElements(By by) {
    	waitForPage();
        waiter.until(ExpectedConditions.presenceOfElementLocated(by));

        return driver.findElements(by);
    }

    /**
     * Real pages supply their expected URL path.
     *
     * @return The path portion of the url of the page.
     */
    public abstract String expectedUrlPath();

    public void clickOnLinkFromHref(String href) throws InterruptedException{
        // We allow use of xpath here because href's tend to be quite stable.
        clickOn(byFromHref(href));
    }

    public By byFromHref(String href) {
        return By.xpath("//a[@href='" + href + "']");
    }

    public void waitForFocusById(final String id) {
    	waitForPage();

        waiter.until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return hasFocus(id);
            }
        });
    }

    public void waitForFocusByCss(final String tag, final String attr, final String value) {
    	waitForPage();

        waiter.until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return hasFocus(tag, attr, value);
            }
        });
    }

    public void waitForJsVariable(final String varName) {
    	waitForPage();

        waiter.until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return ((JavascriptExecutor)driver).executeScript("return (typeof " + varName + "  !== 'undefined') ? " + varName + " : null") != null;
            }
        });
    }

    public void waitForElementToBeHidden(By by) {
    	waitForPage();

        waiter.until(ExpectedConditions.invisibilityOfElementLocated(by));
    }

    public void waitForElementToBeEnabled(By by) {
    	waitForPage();

        waiter.until(ExpectedConditions.elementToBeClickable(by));
    }

    boolean hasFocus(String id) {
    	waitForPage();

        return (Boolean) ((JavascriptExecutor)driver).executeScript("return jQuery('#" + id +  "').is(':focus')", new Object[] {});
    }

    boolean hasFocus(String tag, String attr, String value) {
    	waitForPage();

        return (Boolean) ((JavascriptExecutor)driver).executeScript("return jQuery('" + tag + "[" + attr + "=" + value + "]').is(':focus')", new Object[] {});
    }

    public void waitForElement(By by) {
    	waitForPage();

        waiter.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    public void waitForTextToBePresentInElement(By by, String text) {
    	waitForPage();

        waiter.until(ExpectedConditions.textToBePresentInElementLocated(by, text));
    }

    public Boolean containsText(String text) {
    	waitForPage();

        return driver.getPageSource().contains(text);
    }

}
