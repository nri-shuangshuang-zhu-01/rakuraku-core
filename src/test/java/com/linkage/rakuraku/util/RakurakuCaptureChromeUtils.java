package com.linkage.rakuraku.util;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;

import com.google.common.collect.ImmutableMap;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

public class RakurakuCaptureChromeUtils {

    public static WebSocket webSocket = null;
    static Object waitCoordinator = new Object();
    final static int timeoutValue = 5;
    private static String response;

    /**
     * get mobileEmulation by phoneType
     *
     * @param phoneType
     * @return
     * @throws Exception
     */
    public static Map<String, Object> getSPDevice(String phoneType) throws Exception {

        String key = StringUtils.replace(phoneType, " ", "");

        Properties prop = RakurakuDBUtils.getProps();

        if (Boolean.valueOf(prop.getProperty("SP_DEVICE." + key + ".enable", "false"))) {
            Map<String, Object> mobileEmulation = new HashMap<>();

            Map<String, Object> deviceMetrics = new HashMap<>();
            deviceMetrics.put("width", Integer.parseInt(prop.getProperty("SP_DEVICE." + key + ".width", "0")));
            deviceMetrics.put("height", Integer.parseInt(prop.getProperty("SP_DEVICE." + key + ".height", "0")));
            String pixelRatio = prop.getProperty("SP_DEVICE." + key + ".pixelRatio");
            if (StringUtils.isNotBlank(pixelRatio)) {
                deviceMetrics.put("pixelRatio", Double.parseDouble(pixelRatio));
            }

            mobileEmulation.put("deviceMetrics", deviceMetrics);
            mobileEmulation.put("userAgent", prop.getProperty("SP_DEVICE." + key + ".userAgent", ""));
            return mobileEmulation;
        }
        return ImmutableMap.of("deviceName", phoneType);
    }

    /**
     * capture full size page
     * <p>
     * run with chrome devtools
     *
     * @return
     * @throws IOException
     * @throws WebSocketException
     * @throws InterruptedException
     */
    public static BufferedImage captureFullSizePageForPC(String imageFormat)
            throws IOException, WebSocketException, InterruptedException {
        webSocket = null;
        waitCoordinator = new Object();
        response = null;
        String webSocketURL = getWebSocketDebuggerUrl();

        String deviceJson = getDeviceMetrics(webSocketURL);
        setDeviceMetrics(webSocketURL, deviceJson);
        String base64Data = getbase64ScreenShotData(webSocketURL, imageFormat);
        clearDeviceMetrics(webSocketURL);

        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
        return ImageIO.read(new ByteArrayInputStream(decodedBytes));
    }

    /**
     * capture full size page
     * <p>
     * run with chrome devtools
     *
     * @return
     * @throws IOException
     * @throws WebSocketException
     * @throws JSONException
     * @throws InterruptedException
     */
    public static BufferedImage captureFullSizePageForSP(WebDriver driver, String imageFormat)
            throws IOException, WebSocketException, JSONException, InterruptedException {
        String webSocketURL = getWebSocketDebuggerUrl();

        // https://stackoverflow.com/questions/4573956/taking-screenshot-using-javascript-for-chrome-extensions
        JavascriptExecutor js = ((JavascriptExecutor) driver);
        String pageInfo = js.executeScript(
                "return JSON.stringify({width:window.visualViewport.width, height:window.visualViewport.height,scrollWidth:document.documentElement.scrollWidth, scrollHeight:document.documentElement.scrollHeight})")
                .toString();
        JSONObject contentSize = new JSONObject(pageInfo);
        long width = contentSize.getLong("scrollWidth");
        long height = contentSize.getLong("scrollHeight");

        setDeviceMetrics(webSocketURL,
                "{\"mobile\": true, \"width\": " + width + ", \"height\":  " + height + ", \"deviceScaleFactor\": 2}");

        // capture screenshot
        String base64Data = getbase64ScreenShotData(webSocketURL, imageFormat);

        // restore device metrics
        long clientWidth = contentSize.getLong("width");
        long clientHeight = contentSize.getLong("height");
        setDeviceMetrics(webSocketURL, "{\"mobile\": true, \"width\": " + clientWidth + ", \"height\":  " + clientHeight
                + ", \"deviceScaleFactor\": 1}");

        return ImageIO.read(new ByteArrayInputStream(OutputType.BYTES.convertFromBase64Png(base64Data)));
    }

    private static String getWebSocketDebuggerUrl() throws IOException {
        String webSocketDebuggerURL = "";
        File file = new File(System.getProperty("user.dir") + "/chromedriver.log");
        try {

            Scanner sc = new Scanner(file, "UTF-8");
            String urlString = "";
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.toLowerCase().contains("devtools request: http://localhost")
                        || line.toLowerCase().contains("devtools http request: http://localhost")) {
                    urlString = line.substring(line.indexOf("http"), line.length()).replace("/version", "");
                    break;
                }
            }
            sc.close();

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String json = org.apache.commons.io.IOUtils.toString(reader);
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("type").equals("page")) {
                    webSocketDebuggerURL = jsonObject.getString("webSocketDebuggerUrl");
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            throw e;
        }
        if (webSocketDebuggerURL.equals(""))
            throw new RuntimeException("webSocketDebuggerURL not found..");
        return webSocketDebuggerURL;
    }

    private static String sendWSMessage(String url, String message)
            throws IOException, WebSocketException, InterruptedException {
        if (webSocket == null) {
            webSocket = new WebSocketFactory().createSocket(url).addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket ws, String result) {
                    response = result;
                    synchronized (waitCoordinator) {
                        waitCoordinator.notifyAll();
                    }
                }
            }).connect();
        }

        webSocket.sendText(message);
        synchronized (waitCoordinator) {
            waitCoordinator.wait();
        }
        return response;
    }

    private static String getDeviceMetrics(String wsURL) throws IOException, WebSocketException, InterruptedException {
        String msg = "{\"id\":0,\"method\" : \"Runtime.evaluate\", \"params\" : {\"returnByValue\" : true, \"expression\" : \"({width: Math.max(window.innerWidth,document.body.scrollWidth,document.documentElement.scrollWidth)|0,height: Math.max(window.innerHeight,document.body.scrollHeight,document.documentElement.scrollHeight)|0,deviceScaleFactor: window.devicePixelRatio || 1,mobile: typeof window.orientation !== 'undefined'})\"}}";
        JSONObject responseParser = new JSONObject(sendWSMessage(wsURL, msg));
        JSONObject result1Parser = responseParser.getJSONObject("result");
        JSONObject result2Parser = result1Parser.getJSONObject("result");
        String ret = "";
        while ("".equals(ret)) {
            try {
                ret = result2Parser.getJSONObject("value").toString();
            } catch (Exception e) {
            }
        }
        return ret;
    }

    private static void setDeviceMetrics(String wsURL, String devicePropertiesJSON)
            throws IOException, WebSocketException, InterruptedException {
        String msg = "{\"id\":1,\"method\":\"Emulation.setDeviceMetricsOverride\", \"params\":" + devicePropertiesJSON
                + "}";
        sendWSMessage(wsURL, msg);
    }

    private static void clearDeviceMetrics(String wsURL) throws IOException, WebSocketException, InterruptedException {
        String msg = "{\"id\":3,\"method\":\"Emulation.clearDeviceMetricsOverride\", \"params\":{}}";
        sendWSMessage(wsURL, msg);
    }

    /**
     *
     * @see https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-captureScreenshot
     * @param wsURL
     * @return
     * @throws IOException
     * @throws WebSocketException
     * @throws InterruptedException
     */
    private static String getbase64ScreenShotData(String wsURL, String imageFormat)
            throws IOException, WebSocketException, InterruptedException {
        // only support jpeg/png
        String format = "png";
        if (StringUtils.equals("jpg", imageFormat)) {
            format = "jpeg";
        }
        String msg = "{\"id\":2,\"method\":\"Page.captureScreenshot\", \"params\":{\"format\":\"" + format
                + "\", \"fromSurface\":true}}";
        JSONObject responseParser = new JSONObject(sendWSMessage(wsURL, msg));
        JSONObject resultParser = responseParser.getJSONObject("result");
        return resultParser.getString("data");
    }

    public static void cleanup() {
        if (webSocket != null) {
            webSocket.disconnect();
        }
    }
}
