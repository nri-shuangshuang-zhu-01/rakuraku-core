package com.linkage.rakuraku.util;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.linkage.rakuraku.core.RakurakuCore;
import com.linkage.rakuraku.exp.RakurakuException;
import com.linkage.rakuraku.util.other.JunitAssert;

public class RakurakuWebUtils {

    public static RakurakuCaptureUtils guiCamera;

    private static Map<String, Integer> handleList = new HashMap<String, Integer>();

    public static Map<String, String> apiMap = new HashMap<String, String>();

    public static String mockHeaders;
    /**
     * IEDriverのインスタンス化
     *
     * @return WebDriver
     * @throws Exception
     */
    public static WebDriver getIEDriver() throws Exception {
        RakurakuFileUtils.createFloder();
        File file = new File(RakurakuDBUtils.getProps().getProperty("DRIVER_IE")); // メニューからエビデンス取得不要
        System.setProperty("webdriver.ie.driver", file.getAbsolutePath());
        DesiredCapabilities ieCapabilities = DesiredCapabilities.internetExplorer();
        ieCapabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
        ieCapabilities.setCapability(InternetExplorerDriver.IGNORE_ZOOM_SETTING, true);
        WebDriver driver = new InternetExplorerDriver(ieCapabilities);
        /* IE Driverタイムアウト時間 */
        driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
        /* IEウインドのサイズ設定 */
        driver = setWindowSize(driver);
        RakurakuDBUtils.getProps().setProperty("DEVICE_TYPE", "Powered by IE.");
        return driver;
    }

    /**
     * FireFoxDriverのインスタンス化
     *
     * @return WebDriver
     * @throws Exception
     * @throws NumberFormatException
     */
    public static WebDriver getFireFoxDriver() throws NumberFormatException, Exception {
        RakurakuFileUtils.createFloder();
        WebDriver driver = new FirefoxDriver();
        /* FireFoxタイムアウト時間 */
        driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
        /* FireFoxウインドのサイズ設定 */
        driver = setWindowSize(driver);
        RakurakuDBUtils.getProps().setProperty("DEVICE_TYPE", "Powered by FireFox.");
        return driver;
    }

    /**
     * ChromeDriverのインスタンス化
     *
     * @return WebDriver
     * @throws Exception
     */
    public static WebDriver getChromeDriver() throws Exception {
        RakurakuFileUtils.createFloder();
        String key = "webdriver.chrome.driver";
        String value = RakurakuDBUtils.getProps().getProperty("DRIVER_CHROME");
        RakurakuDBUtils.getProps().setProperty("IS_CHROME", "YES");
        System.setProperty(key, value);
        DesiredCapabilities chromeCapabilities = DesiredCapabilities.chrome();
        String downloadFilepath = RakurakuCore.eachEviPath.replace("/", "\\") + "\\Rakuraku_Work\\downloads";
        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("credentials_enable_service", true);
        chromePrefs.put("profile.password_manager_enabled", false);
        chromePrefs.put("download.default_directory", downloadFilepath);
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);
        options.addArguments("--test-type");
        options.addArguments("disable-infobars");
        // options.addArguments("start-maximized");

        chromeCapabilities.setCapability("ignoreProtectedModeSettings", true);
        chromeCapabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        chromeCapabilities.setCapability(ChromeOptions.CAPABILITY, options);
        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY,
                System.getProperty("user.dir") + File.separator + "/chromedriver.log");
        ChromeDriverService chromeDriverService = new ChromeDriverService.Builder().usingAnyFreePort().withVerbose(true)
                .build();
        Thread.sleep(200);
        chromeDriverService.start();
        WebDriver driver = new ChromeDriver(chromeDriverService, chromeCapabilities);
        /* Chrome Driverタイムアウト時間 */
        driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
        /* Chromeウインドのサイズ設定 */
        driver = setWindowSize(driver);
        RakurakuDBUtils.getProps().setProperty("DEVICE_TYPE", "Powered by Chrome.");
        return driver;
    }

    /**
     * SPDriverのインスタンス化
     *
     * @return WebDriver
     * @throws Exception
     */
    public static WebDriver getSPDriver(String phoneType) throws Exception {
        RakurakuFileUtils.createFloder();
        String key = "webdriver.chrome.driver";
        String value = RakurakuDBUtils.getProps().getProperty("DRIVER_CHROME");
        RakurakuDBUtils.getProps().setProperty("IS_CHROME", "YES");
        System.setProperty(key, value);
        Map<String, String> mobileEmulation = new HashMap<String, String>();
        mobileEmulation.put("deviceName", phoneType);
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("mobileEmulation", mobileEmulation);
        options.addArguments("disable-infobars");
        Map<String, String> chromePrefs = new HashMap<String, String>();
        String downloadFilepath = RakurakuCore.eachEviPath.replace("/", "\\") + "\\Rakuraku_Work\\downloads";
        chromePrefs.put("download.default_directory", downloadFilepath);
        options.setExperimentalOption("prefs", chromePrefs);
        DesiredCapabilities chromeCapabilities = DesiredCapabilities.chrome();
        chromeCapabilities.setCapability(ChromeOptions.CAPABILITY, options);
        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY,
                System.getProperty("user.dir") + File.separator + "/chromedriver.log");
        ChromeDriverService chromeDriverService = new ChromeDriverService.Builder().usingAnyFreePort().withVerbose(true)
                .build();
        Thread.sleep(200);
        chromeDriverService.start();
        WebDriver driver = new ChromeDriver(chromeDriverService, chromeCapabilities);
        /* Chrome Driverタイムアウト時間 */
        driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
        /* Chromeウインドのサイズ設定 */
        driver = setWindowSize(driver);
        RakurakuDBUtils.getProps().setProperty("DEVICE_TYPE", phoneType + " Powered by Chrome.");
        return driver;
    }

    /**
     * SafariDriverのインスタンス化
     *
     * @return WebDriver
     * @throws Exception
     * @throws NumberFormatException
     */
    public static WebDriver getSafariDriver() throws Exception {
        WebDriver driver = new SafariDriver();
        driver = setWindowSize(driver);
        RakurakuDBUtils.getProps().setProperty("DEVICE_TYPE", "Powered by Safari.");
        return driver;
    }

    /**
     * 画面ロードを待つ
     *
     * @param driver
     */
    public static void waitForLoad(WebDriver driver) {
        try {
            if (isAlertPerform(driver)) {
                return;
            }
            driver = webBeforeOperate(driver);
            ExpectedCondition<Boolean> pageLoad = new ExpectedCondition<Boolean>() {
                public Boolean apply(WebDriver driver) {
                    return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
                }
            };
            WebDriverWait wait = new WebDriverWait(driver, 60);
            wait.until(pageLoad);
            Thread.sleep(500);
        } catch (Exception e) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {

            }
        }
    }

    /**
     * テスト画面を遷移
     *
     * @param driver
     * IE Driver
     * @throws Exception
     */
    public static WebDriver jumpToView(WebDriver driver, String viewID) throws Exception {
        if (driver.getWindowHandles().size() <= 1
                || driver.getCurrentUrl().toLowerCase().contains(viewID.toLowerCase())) {
            try {
                driver.getWindowHandle();
            } catch (Exception e) {
                driver = webBeforeOperate(driver);
            }
            return driver;
        }
        String oriHandle = "";
        long timeStart, timeEnd;
        int timeInterval = 0;
        try {
            oriHandle = driver.getWindowHandle();
        } catch (Exception e) {
            throw new RakurakuException("元ウインドが既に閉まりました。");
        }
        timeStart = new Date().getTime();
        while (true) {
            for (String handle : driver.getWindowHandles()) {
                driver.switchTo().window(handle);
                if (driver.getCurrentUrl().toLowerCase().contains(viewID.toLowerCase())) {
                    return driver;
                }
            }
            timeEnd = new Date().getTime();
            timeInterval = (int) ((timeEnd - timeStart) / 1000);
            if (timeInterval > 15) {
                break;
            }
        }
        if (StringUtils.isNotBlank(oriHandle)) {
            driver.switchTo().window(oriHandle);
        }
        return driver;
    }

    /**
     * バッチ実行
     *
     * @param
     * @param params
     * @return
     * @throws Exception
     */
    public static void runBat(String flg, String params) throws Exception {
        if ("×".equals(params)) {
            return;
        }
<<<<<<< .mine
        String cmd = "";
        //String url = "D:\\pleiades\\workspace\\rakuraku-auto-test\\test.bat";
        String url = guiCamera.getSeqNoPath() + ".bat";
        FileWriter fw = null;
        fw = new FileWriter(url);
        Process process = null;
        String jobId = getRealValueAfterReplace(RakurakuDBUtils.getProps().getProperty("BATCH_PATH"));
||||||| .r12417
        String jobId = getRealValueAfterReplace(RakurakuDBUtils.getProps().getProperty("BATCH_PATH"));
=======
        String cmd = "";
        //String url = "D:\\pleiades\\workspace\\rakuraku-auto-test\\test.bat";
        String url = guiCamera.getSeqNoPath() + ".bat";
        FileWriter fw = null;
        fw = new FileWriter(url);
        Process process = null;
        String jobId = "\"" + getRealValueAfterReplace(RakurakuDBUtils.getProps().getProperty("BATCH_PATH")) + "\"";
>>>>>>> .r18472
        String parameter = "";
        if (!"〇".equals(params) && !"○".equals(params)) {
            String[] paramArr = params.split(",");
            for (String eachParam : paramArr) {
                parameter = parameter + " \"" + getRealValueAfterReplace(eachParam) + "\"";
            }
        }
//        Process pro;
        BufferedWriter bw = null;
//        BufferedWriter bw = new BufferedWriter(
//                new OutputStreamWriter(new FileOutputStream(new File(guiCamera.getSeqNoPath() + ".log")), "UTF-8"));
        // JAVA反射としてバッチを実施する
        if ("1".equals(RakurakuDBUtils.getProps().getProperty("BATCH_RUN_TYPE"))) {
            Class<?> batCls = Class.forName("shipment-control-batch.src.test.java.jp.co.misumi.shipment.control.batch.ArrivalShipmentTotalJobTest");
            Method batMod = batCls.getMethod("testApplicaiton", String[].class);
            String[] args = {"arrivalShipmentTotalJob", "--job-id=12345", "--operation-date=20210709",
                "--subsidiary-code=MJP", "--job-net-id=100"};
            params = Arrays.toString(args);
            batMod.invoke(null, (Object) params);
        }


        // ウインドウズコマンドとしてバッチを実施する
        else if ("2".equals(RakurakuDBUtils.getProps().getProperty("BATCH_RUN_TYPE"))) {
            String[] s1=RakurakuCore.eachEviPath.split("case_");
            if ("〇".equals(flg) || "○".equals(flg)) {
                if (StringUtils.isNotBlank(parameter)) {
<<<<<<< .mine
                    cmd = "set JAVA_HOME=D:\\pleiades\\jdk11.0.3_7\nset PATH=%JAVA_HOME%;%JAVA_HOME%\\bin;\njava -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
                    guiCamera.getSeqNoPath() + ".log";
                    fw.write(cmd);
                    fw.close();
                    process = Runtime.getRuntime().exec(url);
                    InputStream in = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    in.close();
//                    pro = Runtime.getRuntime().exec(
//                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
//                            guiCamera.getSeqNoPath() + ".log");
||||||| .r12417
                    pro = Runtime.getRuntime().exec(
                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
                            guiCamera.getSeqNoPath() + ".log");
=======
                    cmd = "set JAVA_HOME=D:\\pleiades\\jdk11.0.3_7\nset PATH=%JAVA_HOME%;%JAVA_HOME%\\bin;\njava -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
                    guiCamera.getSeqNoPath() + ".log 2>&1";
                    fw.write(cmd);
                    fw.close();
                    process = Runtime.getRuntime().exec("cmd /c " + url);
                    InputStream in = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    in.close();
//                    pro = Runtime.getRuntime().exec(
//                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
//                            guiCamera.getSeqNoPath() + ".log");
>>>>>>> .r18472
                } else {
<<<<<<< .mine
                    cmd = "set JAVA_HOME=D:\\pleiades\\jdk11.0.3_7\nset PATH=%JAVA_HOME%;%JAVA_HOME%\\bin;\njava -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " +
                        guiCamera.getSeqNoPath() + ".log";
                    fw.write(cmd);
                    fw.close();
                    process = Runtime.getRuntime().exec(url);
                    InputStream in = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    in.close();
//                    pro = Runtime.getRuntime().exec(
//                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " + guiCamera.getSeqNoPath() + ".log");
||||||| .r12417
                    pro = Runtime.getRuntime().exec(
                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " + guiCamera.getSeqNoPath() + ".log");
=======
                    cmd = "set JAVA_HOME=D:\\pleiades\\jdk11.0.3_7\nset PATH=%JAVA_HOME%;%JAVA_HOME%\\bin;\njava -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " +
                        guiCamera.getSeqNoPath() + ".log 2>&1";
                    fw.write(cmd);
                    fw.close();
                    process = Runtime.getRuntime().exec(url);
                    InputStream in = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    in.close();
//                    pro = Runtime.getRuntime().exec(
//                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " + guiCamera.getSeqNoPath() + ".log");
>>>>>>> .r18472
                }

            }else{
                if (StringUtils.isNotBlank(parameter)) {
<<<<<<< .mine
                    cmd = "set JAVA_HOME=D:\\pleiades\\jdk11.0.3_7\nset PATH=%JAVA_HOME%;%JAVA_HOME%\\bin;\njava -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
                        "E:\\case" + ".log";
                    fw.write(cmd);
                    fw.close();
                    process = Runtime.getRuntime().exec(url);
                    InputStream in = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    in.close();
//                    pro = Runtime.getRuntime().exec(
//                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
//                            "E:\\case" + ".log");
||||||| .r12417
                    pro = Runtime.getRuntime().exec(
                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
                            "E:\\case" + ".log");
=======
                    cmd = "set JAVA_HOME=D:\\pleiades\\jdk11.0.3_7\nset PATH=%JAVA_HOME%;%JAVA_HOME%\\bin;\njava -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
                        "E:\\case" + ".log 2>&1";
                    fw.write(cmd);
                    fw.close();
                    process = Runtime.getRuntime().exec(url);
                    InputStream in = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    in.close();
//                    pro = Runtime.getRuntime().exec(
//                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " " + parameter + " > " +
//                            "E:\\case" + ".log");
>>>>>>> .r18472
                } else {
<<<<<<< .mine
                    cmd = "set JAVA_HOME=D:\\pleiades\\jdk11.0.3_7\nset PATH=%JAVA_HOME%;%JAVA_HOME%\\bin;\njava -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " +
                        "E:\\case" + ".log";
                    fw.write(cmd);
                    fw.close();
                    process = Runtime.getRuntime().exec(url);
                    InputStream in = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    in.close();
//                    pro = Runtime.getRuntime().exec(
//                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " + "E:\\case" + ".log");
||||||| .r12417
                    pro = Runtime.getRuntime().exec(
                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " + "E:\\case" + ".log");
=======
                    cmd = "set JAVA_HOME=D:\\pleiades\\jdk11.0.3_7\nset PATH=%JAVA_HOME%;%JAVA_HOME%\\bin;\njava -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " +
                        "E:\\case" + ".log 2>&1";
                    fw.write(cmd);
                    fw.close();
                    process = Runtime.getRuntime().exec(url);
                    InputStream in = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    in.close();
//                    pro = Runtime.getRuntime().exec(
//                        "cmd /c java -javaagent:D:\\pleiades\\workspace\\rakuraku-core\\src\\test\\resources\\selenium-2\\lib\\org.jacoco.agent-0.8.5-runtime.jar=includes=*,destfile=" + s1[0] +"jacoco.exec,append=true -jar " + jobId + " > " + "E:\\case" + ".log");
>>>>>>> .r18472
                }
            }
<<<<<<< .mine
            process.waitFor();
//            pro.waitFor();
//            if (pro.exitValue() == 0) {
//
//                if (bw != null) {
//                    bw.close();
//                }
//                pro.destroy();
//                pro = null;
//            } else {
//
//                bw.close();
//                pro.destroy();
//                pro = null;
||||||| .r12417
            pro.waitFor();
            if (pro.exitValue() == 0) {

                if (bw != null) {
                    bw.close();
                }
                pro.destroy();
                pro = null;
            } else {

                bw.close();
                pro.destroy();
                pro = null;
=======
            while(process.getInputStream() == null){
                new DealProcessSream(process.getInputStream()).start();
            }
            new DealProcessSream(process.getErrorStream()).start();
            process.waitFor(50,TimeUnit.SECONDS);
            process.destroy();
            Thread.sleep(3000);
//            pro.waitFor();
//            if (pro.exitValue() == 0) {
//
//                if (bw != null) {
//                    bw.close();
//                }
//                pro.destroy();
//                pro = null;
//            } else {
//
//                bw.close();
//                pro.destroy();
//                pro = null;
>>>>>>> .r18472
                //throw new RakurakuException("バッチ実行コマンドが正しくない。");
//            }
        }

        // Linuxバッチサーバーへ接続して、ユニックスコマンドとしてバッチを実施する
        else if ("3".equals(RakurakuDBUtils.getProps().getProperty("BATCH_RUN_TYPE"))) {
            JSch jsch = new JSch();
            Session sessionBL = jsch.getSession(RakurakuDBUtils.getProps().getProperty("SERVER_USERNAME"),
                    RakurakuDBUtils.getProps().getProperty("SERVER_HOST"), 22);
            sessionBL.setPassword(RakurakuDBUtils.getProps().getProperty("SERVER_PASSWORD"));
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            sessionBL.setConfig(config);
            sessionBL.setTimeout(30000);
            sessionBL.connect();
            ChannelExec ChannelExecBL = (ChannelExec) sessionBL.openChannel("exec");
            ChannelExecBL.setCommand(
                    RakurakuDBUtils.getProps().getProperty("SERVER_COMMAND_PREFIX") + jobId + " " + parameter);
            ChannelExecBL.connect();
            ChannelExecBL.setInputStream(null);
            ChannelExecBL.setErrStream(System.err);
            BufferedReader reader = null;
            reader = new BufferedReader(new InputStreamReader(ChannelExecBL.getInputStream(),
                    RakurakuDBUtils.getProps().getProperty("SERVER_CHARSET")));
            String line;
            while ((line = reader.readLine()) != null) {
                bw.write(line + "\r\n");
            }
            ChannelExecBL.disconnect();
            sessionBL.disconnect();
        }

        // PowerShellでバッチサーバーへ接続して、ユニックスコマンドとしてバッチを実施する
        else if ("4".equals(RakurakuDBUtils.getProps().getProperty("BATCH_RUN_TYPE"))) {
            String command = "powershell.exe $Username = 'SERVER_USERNAME';" + "$Password = 'SERVER_PASSWORD';"
                    + "$pass = ConvertTo-SecureString -AsPlainText $Password -Force;"
                    + "$Cred = New-Object System.Management.Automation.PSCredential -ArgumentList $Username,$pass;"
                    + "Invoke-Command -ComputerName SERVER_HOST -ScriptBlock {cd SERVER_WORKFLDR;./RUN_BAT_COMMAND} -Credential $Cred;";
            command = command.replaceAll("SERVER_HOST", RakurakuDBUtils.getProps().getProperty("SERVER_HOST"));
            command = command.replaceAll("SERVER_USERNAME", RakurakuDBUtils.getProps().getProperty("SERVER_USERNAME"));
            command = command.replaceAll("SERVER_PASSWORD", RakurakuDBUtils.getProps().getProperty("SERVER_PASSWORD"));
            command = command.replaceAll("SERVER_WORKFLDR", RakurakuDBUtils.getProps().getProperty("SERVER_WORKFLDR"));
            command = command.replaceAll("RUN_BAT_COMMAND", jobId + parameter);
            Process powerShellProcess = Runtime.getRuntime().exec(command);
            powerShellProcess.getOutputStream().close();

            String line;
            bw.write("■バッチ実行ログ" + "\r\n");
            BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream(),
                    RakurakuDBUtils.getProps().getProperty("SERVER_CHARSET")));
            while ((line = stdout.readLine()) != null) {
                bw.write(line + "\r\n");
            }
            stdout.close();
            BufferedReader stderr = new BufferedReader(new InputStreamReader(powerShellProcess.getErrorStream(),
                    RakurakuDBUtils.getProps().getProperty("SERVER_CHARSET")));
            while ((line = stderr.readLine()) != null) {
                bw.write(line + "\r\n");
            }
            bw.close();
            stderr.close();
        }
    }

    /**
     * ログ出力
     *
     * @param logPath
     * @param guiCamera
     * @return
     * @throws Exception
     */
    public static boolean checkLog(String logPath, RakurakuCaptureUtils guiCamera) throws Exception {
        boolean retFlg = true;
        if (("3").equals(RakurakuDBUtils.getProps().getProperty("BATCH_RUN_TYPE"))) {
            JSch jsch = new JSch();
            Session session = jsch.getSession(RakurakuDBUtils.getProps().getProperty("SERVER_USERNAME"),
                    RakurakuDBUtils.getProps().getProperty("SERVER_HOST"), 22);
            session.setPassword(RakurakuDBUtils.getProps().getProperty("SERVER_PASSWORD"));
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(30000);
            if (!"ON".equals(RakurakuDBUtils.getProps().getProperty("SERVER_LOG_DL"))) {
                String fileName = logPath.split("//")[logPath.split("//").length];
                ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
                channelExec.setCommand(
                        "tail -n " + RakurakuDBUtils.getProps().getProperty("SERVER_LOG_MAXROW") + " " + fileName);
                channelExec.connect();
                channelExec.setInputStream(null);
                channelExec.setErrStream(System.err);
                BufferedReader reader = null;
                reader = new BufferedReader(new InputStreamReader(channelExec.getInputStream(),
                        RakurakuDBUtils.getProps().getProperty("SERVER_CHARSET")));
                String line;
                File file = new File(RakurakuDBUtils.getProps().getProperty("SERVER_LOG_TMPPATH"));
                if (!file.exists()) {
                    File dir = new File(file.getParent());
                    dir.mkdirs();
                    file.createNewFile();
                }
                FileOutputStream outStream = new FileOutputStream(file);
                while ((line = reader.readLine()) != null) {
                    outStream.write(line.getBytes());
                }
                outStream.close();
                channelExec.disconnect();
            } else {
                Channel channel = (Channel) session.openChannel("sftp");
                channel.connect(1000);
                ChannelSftp sftp = (ChannelSftp) channel;
                sftp.cd("//");
                sftp.get(logPath, RakurakuDBUtils.getProps().getProperty("SERVER_LOG_TMPPATH"));
                sftp.disconnect();
                channel.disconnect();
            }
            session.disconnect();
            logPath = RakurakuDBUtils.getProps().getProperty("SERVER_LOG_TMPPATH");
        }

        // PowerShellでバッチサーバーへ接続して、ユニックスコマンドとしてバッチを実施する
        else if ("4".equals(RakurakuDBUtils.getProps().getProperty("BATCH_RUN_TYPE"))) {
            String command = "powershell.exe $Username = 'SERVER_LOG_USERNAME';" + "$Password = 'SERVER_LOG_PASSWORD';"
                    + "$pass = ConvertTo-SecureString -AsPlainText $Password -Force;"
                    + "$Cred = New-Object System.Management.Automation.PSCredential -ArgumentList $Username,$pass;"
                    + "Invoke-Command -ComputerName SERVER_LOG_HOST -ScriptBlock {get-content SERVER_LOG_TMPPATH/RUN_BAT_COMMAND -tail SERVER_LOG_MAXROW -encoding SERVER_LOG_CHARSET} -Credential $Cred;";
            command = command.replaceAll("SERVER_LOG_HOST", RakurakuDBUtils.getProps().getProperty("SERVER_LOG_HOST"));
            command = command.replaceAll("SERVER_LOG_USERNAME",
                    RakurakuDBUtils.getProps().getProperty("SERVER_LOG_USERNAME"));
            command = command.replaceAll("SERVER_LOG_PASSWORD",
                    RakurakuDBUtils.getProps().getProperty("SERVER_LOG_PASSWORD"));
            command = command.replaceAll("SERVER_LOG_MAXROW",
                    RakurakuDBUtils.getProps().getProperty("SERVER_LOG_MAXROW"));
            command = command.replaceAll("SERVER_LOG_CHARSET",
                    RakurakuDBUtils.getProps().getProperty("SERVER_LOG_CHARSET"));
            command = command.replaceAll("SERVER_LOG_TMPPATH",
                    RakurakuDBUtils.getProps().getProperty("SERVER_LOG_TMPPATH"));
            command = command.replaceAll("RUN_BAT_COMMAND", logPath);
            Process powerShellProcess = Runtime.getRuntime().exec(command);
            powerShellProcess.getOutputStream().close();
            BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream(),
                    RakurakuDBUtils.getProps().getProperty("SERVER_CHARSET")));
            BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(guiCamera.getSeqNoPath() + ".log"), "UTF-8"));
            String line = null;
            boolean outFlag = false;
            Date runT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(RakurakuDBUtils.runTime);
            bw.write("■ログ出力　" + logPath + "\r\n");
            while ((line = stdout.readLine()) != null) {
                if (outFlag) {
                    if (line.contains(" ERROR ")) {
                        retFlg = false;
                    }
                    bw.write(line + "\r\n");
                    continue;
                }
                String[] lineArr = line.split(",");
                String logTime = "";
                if (lineArr.length >= 2) {
                    logTime = lineArr[0] + ".";
                    if (lineArr[1].length() >= 3) {
                        logTime = logTime + lineArr[1].substring(0, 3);
                    }
                }
                Date logT = null;
                try {
                    logT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(logTime);
                } catch (Exception e) {
                    continue;
                }
                if (logT.compareTo(runT) >= 0) {
                    if (line.contains(" ERROR ")) {
                        retFlg = false;
                    }
                    bw.write(line + "\r\n");
                    outFlag = true;
                }
            }
            stdout.close();
            bw.close();
            return retFlg;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logPath), "UTF-8"));
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(guiCamera.getSeqNoPath() + ".log"), "UTF-8"));
        String line = null;
        boolean outFlag = false;
        Date runT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(RakurakuDBUtils.runTime);
        bw.write("■ログ出力　" + logPath + "\r\n");
        while ((line = br.readLine()) != null) {
            if (outFlag) {
                if (line.contains(" ERROR ")) {
                    retFlg = false;
                }
                bw.write(line + "\r\n");
                continue;
            }
            String[] lineArr = line.split("\\,");
            String logTime = "";
            if (lineArr.length >= 2) {
                logTime = lineArr[0] + ".";
                if (lineArr[1].length() >= 3) {
                    logTime = logTime + lineArr[1].substring(0, 3);
                }
            }
            Date logT = null;
            try {
                logT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(logTime);
            } catch (Exception e) {
                continue;
            }
            if (logT.compareTo(runT) >= 0) {
                if (line.contains(" ERROR ")) {
                    retFlg = false;
                }
                bw.write(line + "\r\n");
                outFlag = true;
            }
        }
        if (br != null) {
            br.close();
        }
        if (bw != null) {
            bw.close();
        }
        return retFlg;
    }

    /**
     * CSV検証
     *
     * @param
     * @param guiCamera
     * @return
     * @throws Exception
     */
    public static void checkCSV(String csvName, String notAssertColumns, String opeVal, RakurakuCaptureUtils guiCamera)
            throws Exception {
        String csvPath = RakurakuCore.eachEviPath + "/Rakuraku_Work/downloads";
        long modifiedTemp = 0;
        String lastTimeCsvPath = "";
        File file = new File(csvPath);
        if (file.isDirectory()) {
            String[] fileList = file.list();
            for (int i = 0; i < fileList.length; i++) {
                File fileCSV = new File(csvPath + "//" + fileList[i]);
                if (fileCSV.isFile()) {
                    long modifiedTime = fileCSV.lastModified();
                    if (modifiedTime > modifiedTemp) {
                        modifiedTemp = modifiedTime;
                        lastTimeCsvPath = csvPath + "//" + fileList[i];
                    }
                }
            }
        }

        File file1 = new File(lastTimeCsvPath);
        String Path = guiCamera.getSeqNoPath() + "-" + lastTimeCsvPath.split("//")[1];
        FileUtils.copyFile(file1, new File(Path));
        if ("○".contentEquals(opeVal) || "〇".contentEquals(opeVal)) {
            return;
        }
        csvPath = lastTimeCsvPath;

        // BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath),
        // RakurakuDBUtils.getProps().getProperty("DL_FILE_CHARSET")));
        //
        // Assert.assertEquals("CSV[" + csvPath + "]のレコード数", exptCsv.size(), br.lines().count());
        // if (br != null) {
        // br.close();
        // }
        //
        // List<Integer> notAssertIndes = new ArrayList<Integer>();
        // String[] exptTitles = exptCsv.get(0).replace("\"", "").split(",");
        // for (int j = 0; j < notAssertColumns.length; j++) {
        // for (int n = 0; n < exptTitles.length; n++) {
        // if (exptTitles[n].equals(notAssertColumns[j])) {
        // notAssertIndes.add(n);
        // }
        // }
        // }
        // br = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath),
        // RakurakuDBUtils.getProps().getProperty("DL_FILE_CHARSET")));
        // String line = null;
        // int index = 0;
        // while ((line = br.readLine()) != null) {
        // String exptLine = exptCsv.get(index);
        // String[] lineStrs = line.replace("\"", "").split(",", -1);
        // String[] exptLineStrs = exptLine.replace("\"", "").split(",", -1);
        // for (int m = 0; m < lineStrs.length; m++) {
        // if (notAssertIndes.contains(m)) {
        // continue;
        // } else {
        // String errinfo = "value (CSV=" + csvPath + ", row=" + index + ", title=" + exptTitles[m] + "):";
        // Assert.assertEquals(errinfo, exptLineStrs[m], lineStrs[m]);
        // }
        // }
        // // String errinfo = "value (CSV=" + csvPath + ", row=" + index +
        // // "):";
        // // Assert.assertEquals(errinfo, exptLine.replace("\"", ""),
        // // line.replace("\"", ""));
        // index++;
        // }
        // if (br != null) {
        // br.close();
        // }
    }

    @SuppressWarnings("rawtypes")
    public static void checkCSVZIP(String csvPath, List<List<String>> exptCsv, RakurakuCaptureUtils guiCamera,
            String... notAssertColumns) throws Exception {
        String unZipAddress = "D:\\csv\\";
        File file = new File(csvPath);
        long modifiedTemp = 0;
        String lastTimeCsvPath = "";
        if (file.isDirectory()) {
            String[] fileList = file.list();
            for (int i = 0; i < fileList.length; i++) {
                File fileCSV = new File(csvPath + "//" + fileList[i]);
                long modifiedTime = fileCSV.lastModified();
                if (modifiedTime > modifiedTemp) {
                    modifiedTemp = modifiedTime;
                    lastTimeCsvPath = csvPath + "//" + fileList[i];
                }
            }
        }
        File file1 = new File(lastTimeCsvPath);
        long zipModifiedTime = file1.lastModified();
        ZipFile zipFile = null;
        zipFile = new ZipFile(file1);
        Enumeration e = zipFile.entries();
        while (e.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) e.nextElement();
            if (zipEntry.isDirectory()) {
                String name = zipEntry.getName();
                name = name.substring(0, name.length() - 1);
                File f = new File(unZipAddress + name);
                f.mkdirs();
            } else {
                File f = new File(unZipAddress + zipEntry.getName());
                f.getParentFile().mkdirs();
                f.createNewFile();
                InputStream is = zipFile.getInputStream(zipEntry);
                FileOutputStream fos = new FileOutputStream(f);
                int length = 0;
                byte[] b = new byte[1024];
                while ((length = is.read(b, 0, 1024)) != -1) {
                    fos.write(b, 0, length);
                }
                is.close();
                fos.close();
            }
        }
        if (zipFile != null) {
            zipFile.close();
        }
        int temp = 0;
        List<String> tempList = new ArrayList<String>();
        if (file.isDirectory()) {
            String[] fileList = file.list();
            for (int i = 0; i < fileList.length; i++) {
                File fileCSV = new File(csvPath + "//" + fileList[i]);
                long modifiedTime = fileCSV.lastModified();
                if (modifiedTime > zipModifiedTime) {
                    lastTimeCsvPath = csvPath + "//" + fileList[i];

                    File copyFile = new File(lastTimeCsvPath);
                    String Path = guiCamera.getSeqNoPath() + "-" + fileList[i];
                    FileUtils.copyFile(copyFile, new File(Path));

                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lastTimeCsvPath),
                            RakurakuDBUtils.getProps().getProperty("DL_FILE_CHARSET")));

                    if (br != null) {
                        br.close();
                    }

                    br = new BufferedReader(new InputStreamReader(new FileInputStream(lastTimeCsvPath),
                            RakurakuDBUtils.getProps().getProperty("DL_FILE_CHARSET")));
                    String line = null;
                    int index = 0;
                    tempList = exptCsv.get(temp);
                    List<Integer> notAssertIndes = new ArrayList<Integer>();
                    String[] exptTitles = tempList.get(0).replace("\"", "").split(",");
                    for (int j = 0; j < notAssertColumns.length; j++) {
                        for (int n = 0; n < exptTitles.length; n++) {
                            if (exptTitles[n].equals(notAssertColumns[j])) {
                                notAssertIndes.add(n);
                            }
                        }
                    }
                    while ((line = br.readLine()) != null) {
                        String exptLine = tempList.get(index);
                        String[] lineStrs = line.replace("\"", "").split(",", -1);
                        String[] exptLineStrs = exptLine.replace("\"", "").split(",", -1);
                        for (int m = 0; m < lineStrs.length; m++) {
                            if (notAssertIndes.contains(m)) {
                                continue;
                            } else {
                                String errinfo = "value (CSV=" + csvPath + ", row=" + index + ", title=" + exptTitles[m]
                                        + "):";
                                Assert.assertEquals(errinfo, lineStrs[m], exptLineStrs[m]);
                            }
                        }
                        index++;
                    }
                    if (br != null) {
                        br.close();
                    }
                    temp++;
                }
            }
        }
    }

    /**
     * API呼出
     *
     * @param
     *
     * @param
     *
     * @param apiKind
     * 呼出すAPI対象
     * @param callMeth
     * 呼出方式
     * @param guiCamera
     * ログキャプチャ
     * @throws Exception
     */
//    public static void callApi(String authKey, String requestStr, String apiKind, String callMeth,
//            RakurakuCaptureUtils guiCamera) throws Exception {
//        RestAssured.baseURI = RakurakuDBUtils.getProps().getProperty("REST_HOST");
//        RestAssured.port = Integer.parseInt(RakurakuDBUtils.getProps().getProperty("REST_PORT"));
//        BufferedWriter bw = null;
//        try {
//            bw = new BufferedWriter(
//                    new OutputStreamWriter(new FileOutputStream(guiCamera.getSeqNoPath() + ".log"), "UTF-8"));
//            if ("GET".equals(callMeth)) {
//                bw.write("■API呼出：　" + callMeth + "\r\n" + RestAssured.baseURI + "/" + apiKind + requestStr + "\r\n");
//                // API呼び出し設定
//                RequestSpecification reqSpec = RestAssured.given();
//                String[] headers = RakurakuDBUtils.getProps().getProperty("REST_GET_HEADER").split(",");
//                for (String header : headers) {
//                    reqSpec.header(header.split("=")[0], header.split("=")[1]);
//                }
//                reqSpec.header(RakurakuDBUtils.getProps().getProperty("REST_AUTH"), authKey);
//                reqSpec.when();
//                // API呼び出し実行
//                Response r = reqSpec.get("/" + apiKind + requestStr).andReturn();
//                bw.write("■APIレスポンス：");
//                bw.write(xmlFormat(r.asString()).replace("\n", "\r\n"));
//            } else {
//                bw.write("■API呼出：　" + callMeth + "　" + RestAssured.baseURI + "/" + apiKind + "\r\n");
//                bw.write("■APIリクエスト：");
//                bw.write(xmlFormat(requestStr).replace("\n", "\r\n") + "\r\n");
//                // API呼び出し設定
//                RequestSpecification reqSpec = RestAssured.given();
//                String[] headers = RakurakuDBUtils.getProps().getProperty("REST_POST_HEADER").split(",");
//                for (String header : headers) {
//                    reqSpec.header(header.split("=")[0], header.split("=")[1]);
//                }
//                reqSpec.header(RakurakuDBUtils.getProps().getProperty("REST_AUTH"), authKey);
//                reqSpec.when();
//                // API呼び出し実行
//                Response r = reqSpec.post("/" + apiKind).andReturn();
//                bw.write("■APIレスポンス：");
//                bw.write(xmlFormat(r.asString()).replace("\n", "\r\n"));
//            }
//        } catch (Exception e) {
//            bw.write(e.toString() + "\r\n");
//            throw e;
//        } finally {
//            bw.close();
//        }
//    }

    public static void callApi(String apiNm, String apiKind, String callMeth, String requestHeaders, String requestBody,
            String loginUser, RakurakuCaptureUtils guiCamera) throws Exception {
        RestAssured.baseURI = RakurakuDBUtils.getProps().getProperty("REST_HOST");
        RestAssured.port = Integer.parseInt(RakurakuDBUtils.getProps().getProperty("REST_PORT"));
        BufferedWriter bw = null;
//        String authKey = null;
        if(StringUtils.isNotBlank(requestHeaders)) {
        	requestHeaders = requestHeaders + "&&" + mockHeaders;
        }else {
        	requestHeaders = mockHeaders;
        }

        if (callMeth != "GET") {
            if (requestBody.contains("{")) {

            } else {
                File file = new File(requestBody);
                BufferedReader reader = null;
                StringBuffer sbf = new StringBuffer();
                reader = new BufferedReader(new FileReader(file));
                String tempStr;
                while ((tempStr = reader.readLine()) != null) {
                    sbf.append(tempStr);
                }
                reader.close();
                requestBody = sbf.toString();
            }
        }

        try {
            bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(guiCamera.getSeqNoPath() + ".log"), "UTF-8"));
            bw.write("■api名称：　"
                    + apiNm + "\r\n");
            bw.write("■api Request：　"
                    + "/" + apiKind + "\r\n");
            bw.write("■api方法：　"
                    + callMeth + "\r\n");
            bw.write("■api Headers：　"
                    + requestHeaders + "\r\n");
            bw.write("■api Body：　"
                    + requestBody + "\r\n");
            bw.write("■" + "loginUser：　" + loginUser + "\r\n");
            if ("GET".equals(callMeth)) {
//                if (!apiKind.contains("login")) {
//                    authKey = getLoginToken(loginUser, requestHeaders);
//                }
                RequestSpecification reqSpec = RestAssured.given();
                String[] headers = requestHeaders.split("&&");
                for (String header : headers) {
                    reqSpec.header(header.split("=")[0], header.split("=")[1]);
                }
//                if (!apiKind.contains("login")) {
//                    reqSpec.header(RakurakuDBUtils.getProps().getProperty("REST_AUTH"), authKey);
//                }
                reqSpec.when();
                Response r = reqSpec.get("/" + apiKind).andReturn();
                bw.write("■api Response：");
                String respStr = jsonFormat(r.asString()).replace("\n", "\r\n");
                bw.write(respStr);
                System.out.println(r.asString());
                apiMap.put(apiKind, r.asString());
            } else {
//                if (!apiKind.contains("login")) {
//                    authKey = getLoginToken(loginUser, requestHeaders);
//                }
                RequestSpecification reqSpec = RestAssured.given();
                String[] headers = requestHeaders.split("&&");
                for (String header : headers) {
                    reqSpec.header(header.split("=")[0], header.split("=")[1]);
                }
//                if (!apiKind.contains("login")) {
//                    reqSpec.header(RakurakuDBUtils.getProps().getProperty("REST_AUTH"), authKey);
//                }
                if(StringUtils.isNotBlank(requestBody)&&!"-".equals(requestBody)) {
                	reqSpec.body(requestBody);
                }
                reqSpec.when();
                Response r = reqSpec.post("/" + apiKind).andReturn();
                String respStr = jsonFormat(r.asString()).replace("\n", "\r\n");
                bw.write("■Response：：\r\n");
                bw.write(respStr);
                System.out.println(r.asString());
                apiMap.put(apiKind, r.asString());
            }
        } catch (Exception e) {
            bw.write(e.toString() + "\r\n");
            throw e;
        } finally {
            bw.close();
        }
    }

    private static String getLoginToken(String loginUser, String requestHeaders) throws Exception {
        RequestSpecification reqSpec = RestAssured.given();
        String[] headers = requestHeaders.split(",");
        for (String header : headers) {
            reqSpec.header(header.split("=")[0], header.split("=")[1]);
        }
        reqSpec.body(loginUser);
        reqSpec.when();
        Response r = reqSpec.post("/raku0000/login").andReturn();

        JSONObject jsonObject = JSON.parseObject(r.asString());
//        ResponseResult responseResult = JSONObject.toJavaObject(jsonObject, ResponseResult.class);
//        @SuppressWarnings("unchecked")
//        Map<String, String> maps = (Map<String, String>) JSON.parse(responseResult.getData());
//        String authKey = maps.get("token");
        return null;
    }

    public static void apiConfirm(String apiKind, String expected, String[] notAssertColumns,
            RakurakuCaptureUtils guiCamera) throws Exception {
        JunitAssert.assertJsonStr(expected, apiMap.get(apiKind), notAssertColumns);
    }

    /**
     * 要素情報クリア
     *
     * @param e
     * @param d
     */
    public static void webClearElementValue(WebElement e, WebDriver d) {
        JavascriptExecutor js = (JavascriptExecutor) d;
        js.executeScript("arguments[0].value=''", e);
    }

    /**
     * アップロード用ファイルパスをインプットボックスへコピー(IE)
     *
     * @param path
     */
    public static void webUploadFileInputForIE(String path) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(path);
        clip.setContents(tText, null);
        try {
            Thread.sleep(2000);
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(100);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.delay(100);
        } catch (AWTException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 操作前のウインド整理
     *
     * @param driver
     * @return
     */
    public static WebDriver webBeforeOperate(WebDriver driver) {
        if (isAlertPerform(driver)) {
            return driver;
        }
        Map<String, Integer> tempMap = new HashMap<String, Integer>();
        int i = 0;
        String finalHandle = "";
        try {
            finalHandle = driver.getWindowHandle();
        } catch (Exception e) {
            finalHandle = "";
        }
        for (String nowHandle : driver.getWindowHandles()) {
            if (handleList.containsKey(nowHandle)) {
                if (handleList.get(nowHandle) > i) {
                    i = handleList.get(nowHandle);
                    finalHandle = nowHandle;
                }
                tempMap.put(nowHandle, handleList.get(nowHandle));
            }
        }
        for (String nowHandle : driver.getWindowHandles()) {
            if (!handleList.containsKey(nowHandle)) {
                i++;
                tempMap.put(nowHandle, i);
                finalHandle = nowHandle;
            }
        }
        handleList = new HashMap<String, Integer>();
        handleList.putAll(tempMap);
        driver.switchTo().window(finalHandle);
        if (i > 1) {
            if (driver.manage().window().getSize()
                    .getHeight() > (GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height
                            - driver.manage().window().getPosition().y)) {
                Dimension dimension = new Dimension(driver.manage().window().getSize().width,
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height
                                - driver.manage().window().getPosition().y);
                driver.manage().window().setSize(dimension);
            }
            if (driver.manage().window().getSize()
                    .getWidth() > (GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width
                            - driver.manage().window().getPosition().x)) {
                Dimension dimension = new Dimension(
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width
                                - driver.manage().window().getPosition().x,
                        driver.manage().window().getSize().height);
                driver.manage().window().setSize(dimension);
            }
        }
        return driver;
    }

    /**
     * ページを閉じる
     *
     * @param driver
     * @return
     * @throws Exception
     */
    public static WebDriver webPageClose(WebDriver driver) throws Exception {
        driver.close();
        Thread.sleep(1000);
        driver = webBeforeOperate(driver);
        return driver;
    }

    /**
     * ページを戻る
     *
     * @param driver
     * @return
     * @throws Exception
     */
    public static WebDriver webPageBack(WebDriver driver) throws Exception {
        driver.navigate().back();
        Thread.sleep(1000);
        driver = webBeforeOperate(driver);
        return driver;
    }

    /**
     * ページを進む
     *
     * @param driver
     * @return
     * @throws Exception
     */
    public static WebDriver webPageForward(WebDriver driver) throws Exception {
        driver.navigate().forward();
        Thread.sleep(1000);
        driver = webBeforeOperate(driver);
        return driver;
    }

    /**
     * 変数値の置換え
     *
     * @param input
     * @return
     * @throws Exception
     */
    public static String getRealValueAfterReplace(String input) throws Exception {
        Matcher matcher = Pattern.compile("[$][{].*?[}]").matcher(input);
        while (matcher.find()) {
            String variableNm = matcher.group();
            String realValue = getVariableValue(variableNm);
            input = input.replace(variableNm, realValue);
        }
        return input;
    }

    /**
     * 変数値取得
     *
     * @param key
     * @return
     * @throws Exception
     */
    private static String getVariableValue(String key) throws Exception {
        if (key.startsWith("${SYS_")) {
            return getSystemVariableValue(key);
        }
        if (!RakurakuCore.variablesMap.containsKey(key)) {
            throw new RakurakuException("変数【" + key + "】の値は取得されていません。");
        }
        return RakurakuCore.variablesMap.get(key);
    }

    /**
     * システム変数値取得
     *
     * @param variableNm
     * @return
     */
    private static String getSystemVariableValue(String variableNm) {

        return "";
    }

    /**
     * ブラウザのウインドサイズ設定
     *
     * @param driver
     * @return
     * @throws Exception
     */
    private static WebDriver setWindowSize(WebDriver driver) throws Exception {
        String windowSize = RakurakuDBUtils.getProps().getProperty("DRIVER_SIZE");
        Dimension dimension = null;
        if (StringUtils.isNotBlank(windowSize)) {
            String[] windows = windowSize.split(",");
            if (windows.length == 1) {
                dimension = new Dimension(Integer.parseInt(windows[0]),
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height - 50);
            } else if (windows.length == 2) {
                if (StringUtils.isNotBlank(windows[0])) {
                    dimension = new Dimension(Integer.parseInt(windows[0]), Integer.parseInt(windows[1]) - 50);
                } else {
                    dimension = new Dimension(
                            GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width,
                            Integer.parseInt(windows[1]) - 50);
                }
            }
        }
        if (dimension != null) {
            driver.manage().window().setSize(dimension);
            driver.manage().window().setPosition(new Point(-8, 0));
        } else {
            driver.manage().window().maximize();
        }
        return driver;
    }

    /**
     * Alertが表現されたかどうか判定
     *
     * @return
     */
    private static boolean isAlertPerform(WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     *
     * @param target
     * @return
     * @throws Exception
     */
    private static String xmlFormat(String target) throws Exception {
        SAXReader reader = new SAXReader();
        StringReader in = new StringReader(target);
        Document doc = reader.read(in);
        OutputFormat formater = OutputFormat.createPrettyPrint();
        formater.setSuppressDeclaration(true);
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, formater);
        writer.write(doc);
        writer.close();
        return out.toString();
    }


    private static String jsonFormat(String target) throws Exception {
        try {
            JsonParser jsonparser = new JsonParser();
            JsonObject jsonObj = jsonparser.parse(target).getAsJsonObject();
            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            return gson.toJson(jsonObj);
        } catch (Exception e) {
            return target;
        }
    }

    public static void main(String args[]) throws Exception {
    }
}
