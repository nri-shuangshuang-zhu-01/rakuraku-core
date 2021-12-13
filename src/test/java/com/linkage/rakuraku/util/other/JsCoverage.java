package com.linkage.rakuraku.util.other;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.linkage.rakuraku.util.RakurakuWebUtils;

public class JsCoverage {

    public static final String HOME_PAGE = "/jscoverage.html";
    public static final String DELETE_LOCAL_STORAGE = "/jscoverage-clear-local-storage.html";
    public static final String SAVE_LOCAL_STORAGE = "/jscoverage-save-local-storage.html";
    public static final String REMOTE_HOST_DOMAIN = "http://localhost-proxy:8080";

    public static String getCoverageURL() {
        return REMOTE_HOST_DOMAIN + "/xxx/app/AAAAA0000";
    }

    /**
     * invalid method
     * 
     * @param driver
     */
    @SuppressWarnings("unused")
    @Deprecated
    private static void clearLocalStorage(WebDriver driver) {
        String deleteUrl = REMOTE_HOST_DOMAIN + DELETE_LOCAL_STORAGE;
        driver.get(deleteUrl);
        RakurakuWebUtils.waitForLoad(driver);
        sleep(1000);
        saveLocalStorage(driver);
    }

    public static void saveLocalStorage(WebDriver driver) {
        String saveUrl = REMOTE_HOST_DOMAIN + SAVE_LOCAL_STORAGE;
        driver.get(saveUrl);
        RakurakuWebUtils.waitForLoad(driver);
    }

    public static void saveLocalStorageOld(WebDriver driver) {
        String homeUrl = REMOTE_HOST_DOMAIN + HOME_PAGE;
        driver.get(homeUrl);
        RakurakuWebUtils.waitForLoad(driver);
        driver.findElement(By.id("storeTab")).click();
        sleep(500);
        driver.findElement(By.id("storeButton")).click();
    }

    public static void sleep(long millseconds) {
        try {
            Thread.sleep(millseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
