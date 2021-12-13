package com.linkage.rakuraku.core;

import com.linkage.rakuraku.constant.RakurakuConst;
import com.linkage.rakuraku.exp.RakurakuException;
import com.linkage.rakuraku.util.RakurakuCaptureUtils;
import com.linkage.rakuraku.util.RakurakuDBUtils;
import com.linkage.rakuraku.util.RakurakuDateUtils;
import com.linkage.rakuraku.util.RakurakuFileUtils;
import com.linkage.rakuraku.util.RakurakuWebUtils;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

public class RakurakuCore {

    /* ブラウザインスタンス */
    public static WebDriver driver;

    /* ブラウザ種別 */
    private String driverType;

    private static String apiMock = "";

    /* キャプチャツール */
    public static RakurakuCaptureUtils guiCamera;

    /* logPathを取得 */
    public static String resultPath;

    /* ケース毎のエビデンスパス */
    public static String eachEviPath;

    /* ケース実施日 */
    public static String runDate;

    public static String s1;

    /* 実行ロガー */
    public static StringBuilder logBuilder;

    /* ケース実行OK、NG */
    public static boolean OKFlag = true;

    public static boolean flg;

    public static String flg1;

    /* エラーメッセージ */
    public static String messageStr;

    /* 変数マップ */
    public static Map<String, String> variablesMap;

    /**
     * ケース毎の自動化インスタンス
     *
     * @param
     * @throws Exception
     */
    public RakurakuCore(String eachOnePath, String driverType) throws Exception {
        RakurakuDateUtils.setRunDate();
        resultPath = eachOnePath.substring(0, eachOnePath.lastIndexOf(RakurakuConst.ONE_SLASH));
        RakurakuFileUtils.getEachEviPath(resultPath);
        RakurakuFileUtils.delAllFile();
        RakurakuFileUtils.makeEachEviDir();
        RakurakuWebUtils.mockHeaders = "";
        this.driverType = driverType;
        logBuilder = new StringBuilder();
        variablesMap = new HashMap<String, String>();
        guiCamera = new RakurakuCaptureUtils(eachOnePath.substring(eachOnePath.lastIndexOf(RakurakuConst.ONE_SLASH)));
    }

    /**
     * テストクラス開始
     *
     * @param dbunit
     * @param funcId
     * @throws Exception
     */
    public static void initialize(String dbunit, String funcId) throws Exception {

        File dir = new File(RakurakuFileUtils.getToolPath() + "/testdata/mockapis/" + funcId);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                String filesPath = RakurakuFileUtils.getToolPath() + "/testdata/mockapis/" + funcId;
                File files = new File(filesPath);
                File[] fs = files.listFiles();
                String dockerPath;
                for(File f:fs){
                    String[] aa = f.getName().split("\\.");
                    dockerPath = RakurakuDBUtils.getProps().getProperty("YAML_PATH") + "\\" + RakurakuDBUtils.getProps().getProperty(aa[0]);
                    FileUtils.copyFile(new File(f.getPath()), new File(dockerPath));
                }
                String dockerSERVICE = RakurakuDBUtils.getProps().getProperty("DOCKER_SERVICE");
                Process pro;
                String batFile = RakurakuFileUtils.getToolPath() + "/testdata/mockapis/" + funcId + "/dockerCmd.bat";
                if(new File(batFile).exists()) {
                    FileUtils.forceDelete(new File(batFile));
                }
                String fileStr = "cd /d " + dockerSERVICE + "\r\ndocker-compose restart\r\nexit";
                FileUtils.writeStringToFile(new File(batFile), fileStr);

                String command = "cmd /c start " + batFile;
                pro = Runtime.getRuntime().exec(command);
                pro.waitFor();
                if (pro.exitValue() == 0) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                    }
                    if (br != null) {
                        br.close();
                    }
                    pro.destroy();
                    pro = null;
                    System.out.println("dockerの起動に成功しました");
                    FileUtils.forceDelete(new File(batFile));
                    Thread.sleep(1000);
                } else {
                    System.out.println("dockerの起動に失敗しました");
                    throw new RakurakuException("dockerの起動に失敗しました");
                }
            }
        }



//    	if("Y".equals(RakurakuDBUtils.getProps().getProperty("DOCKER_FLG"))) {
//	    	String copyPath = RakurakuFileUtils.getToolPath() + "/testdata/mockapis/" + funcId + "/" + funcId + ".yaml";
//	    	if(!new File(copyPath).exists()) {
//	    		System.out.println(copyPath + "は存在しません");
//	    		throw new RakurakuException(copyPath + "は存在しません");
//	    	}
//	    	String yamlPath = RakurakuDBUtils.getProps().getProperty("DOCKER_YAML");
//	    	FileUtils.copyFile(new File(copyPath), new File(yamlPath));
//
//	    	String dockerPath = RakurakuDBUtils.getProps().getProperty("DOCKER_SERVICE");
//	    	Process pro;
//	    	String batFile = RakurakuFileUtils.getToolPath() + "/testdata/mockapis/" + funcId + "/dockerCmd.bat";
//	    	if(new File(batFile).exists()) {
//	    		FileUtils.forceDelete(new File(batFile));
//	    	}
//	    	String fileStr = "cd /d " + dockerPath + "\r\ndocker-compose restart\r\nexit";
//	    	FileUtils.writeStringToFile(new File(batFile), fileStr);
//
//	    	String command = "cmd /c start " + batFile;
//	    	pro = Runtime.getRuntime().exec(command);
//	    	pro.waitFor();
//	    	if (pro.exitValue() == 0) {
//	    		BufferedReader br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
//	            String line;
//	            while ((line = br.readLine()) != null) {
//	            }
//	            if (br != null) {
//	                br.close();
//	            }
//	            pro.destroy();
//	            pro = null;
//	    		System.out.println("dockerの起動に成功しました");
//	    		FileUtils.forceDelete(new File(batFile));
//	    		Thread.sleep(1000);
//	        } else {
//	        	System.out.println("dockerの起動に失敗しました");
//	            throw new RakurakuException("dockerの起動に失敗しました");
//	        }
//    	}else if("Ym".equals(RakurakuDBUtils.getProps().getProperty("DOCKER_FLG"))) {
//            String dockerPath = RakurakuDBUtils.getProps().getProperty("DOCKER_SERVICE");
//            Process pro;
//            String batFile = RakurakuFileUtils.getToolPath() + "/testdata/mockapis/dockerCmd.bat";
//            if(new File(batFile).exists()) {
//                FileUtils.forceDelete(new File(batFile));
//            }
//            String fileStr = "cd /d " + dockerPath + "\r\ndocker-compose restart\r\nexit";
//            FileUtils.writeStringToFile(new File(batFile), fileStr);
//
//            String command = "cmd /c start " + batFile;
//            pro = Runtime.getRuntime().exec(command);
//            pro.waitFor();
//            if (pro.exitValue() == 0) {
//                BufferedReader br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
//                String line;
//                while ((line = br.readLine()) != null) {
//                }
//                if (br != null) {
//                    br.close();
//                }
//                pro.destroy();
//                pro = null;
//                System.out.println("dockerの起動に成功しました");
//                FileUtils.forceDelete(new File(batFile));
//                Thread.sleep(1000);
//            } else {
//                System.out.println("dockerの起動に失敗しました");
//                throw new RakurakuException("dockerの起動に失敗しました");
//            }
//        }
    }

    /**
     * テストケース開始
     *
     * @param dbunit
     * @param funcId
     * @throws Exception
     */
    public static void setUp(String dbunit, String funcId) throws Exception {
        RakurakuDBUtils.initialize(dbunit, funcId);
        messageStr = RakurakuConst.BLANK;
        RakurakuCore.OKFlag = true;
        flg = true;
    }

    /**
     * テストケース終了
     *
     * @throws Exception
     */
    public static void tearDown() throws Exception {
    String ApiBatch = "E:\\ApiBatch.log";
        if(new File(ApiBatch).exists()) {
            FileUtils.forceDelete(new File(ApiBatch));
        }
        String copyPath = "E:\\request.log";
        if(!new File(copyPath).exists()) {

        }else{
            String requestPath = guiCamera.getSeqNoPath() + "request.log";
            FileUtils.writeStringToFile(new File(requestPath), "");
            FileUtils.copyFile(new File(copyPath), new File(requestPath));
            FileUtils.forceDelete(new File(copyPath));
        }
        String cpath = "E:\\intercept.log";
        if(!new File(cpath).exists()) {

        }else{
            String interceptPath = guiCamera.getSeqNoPath() + "response.log";
            FileUtils.writeStringToFile(new File(interceptPath), "");
            FileUtils.copyFile(new File(cpath), new File(interceptPath));
            FileUtils.forceDelete(new File(cpath));
        }
        if (RakurakuCore.OKFlag) {
            RakurakuFileUtils.logFileOK();
        } else {
            RakurakuFileUtils.logFileNG(messageStr);
        }
        if (driver != null) {
            driver.quit();
            RakurakuCore.driver = null;
        }

        String apiPath = eachEviPath + "\\Rakuraku_api\\apimock.csv";
        File file = new File(apiPath);
        File fileParent = file.getParentFile();
        if (!fileParent.exists()) {
            fileParent.mkdirs();
        }
        file.createNewFile();

        FileWriter fw = null;
        fw = new FileWriter(apiPath);
        fw.write(apiMock);
        fw.close();
        apiMock = "";
    }

    /**
     * テストクラス終了
     *
     * @throws Exception
     */
    public static void release(String str) throws Exception {
	RakurakuDBUtils.release();
        RakurakuFileUtils.evidenceTool(str);
    }

    public void operateFiles(String filepath,String flg) throws IOException {
        if (!"〇".equals(flg) && !"○".equals(flg)) {

        }else{
            String fName = filepath.trim();
            String temp[] = fName.split("\\\\");
            String fileName = temp[temp.length-1];
            String fpath = "";
            for(int i=0;i<temp.length - 1;i++){
                fpath = fpath + temp[i] + "\\";
            }
            File file = new File(fpath);
            File[] fs = file.listFiles();
            for(File f:fs){
                if(!f.isDirectory())
                    if(f.getName().contains(fileName)){
                        if(f.getName().contains(".zip")){

                        }else{
                            File filelog =new File(guiCamera.getSeqNoPath() + ".log");
                            filelog.createNewFile();
                            FileUtils.copyFile(f, filelog);
                        }
                    }
            }

        }
    }

    /**
     * 操作(DB準備)
     *
     * @param
     * @param
     * @throws Exception
     */
    public void operateDBPrepare(String tableName, String opeType, String tableNo) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        try {

            // 処理開始====================================
            RakurakuDBUtils.operateTable(tableName, opeType, tableNo, guiCamera);
            RakurakuDBUtils.commit();
            // 処理終了====================================

            logging("【操作(DB準備)】【テーブル：" + tableName + ",DB状態：" + tableNo + "】", startTime, "OK");
        } catch (Throwable e) {
            logging("【操作(DB準備)】【テーブル：" + tableName + ",DB状態：" + tableNo + "】", startTime, "NG");
            throw new RakurakuException("【操作(DB準備)】【テーブル：" + tableName + ",DB状態：" + tableNo + "】の操作に失敗しました。", e);
        }
    }

    /**
     * 操作(DB確認)
     *
     * @param
     * @param
     * @throws Exception
     */
    public void operateDBConfirm(String tableName, String notAssertColumns, String tableNo) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        try {

            // 処理開始====================================
            RakurakuDBUtils.confirmTable(tableName, notAssertColumns, tableNo, guiCamera);
            // 処理終了====================================

            logging("【操作(DB確認)】【テーブル：" + tableName + ",DB状態：" + tableNo + "】", startTime, "OK");
        } catch (Throwable e) {
            logging("【操作(DB確認)】【テーブル：" + tableName + ",DB状態：" + tableNo + "】", startTime, "NG");
            throw new RakurakuException("【操作(DB確認)】【テーブル：" + tableName + ",DB状態：" + tableNo + "】の操作に失敗しました。", e);
        }
    }

    /**
     * 操作(画面)
     *
     * @param actionViewNm 対象画面名称
     * @param actionType 画面アクション種別
     * @param actionEleNm 画面アクション論理名
     * @param actionEle 画面アクション物理名
     * @param actionEleWay 画面アクション取得方式
     * @param actionOpeWay 画面アクション操作方法
     * @param actionOpeValue 画面アクション操作値
     * @throws Exception
     */
    public void operateWeb(String actionViewNm, String actionType, String actionEleNm, String actionEle,
            String actionEleWay, String actionOpeWay, String actionOpeValue) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        String logOpeNm = "【操作(画面)】【操作タイプ：" + actionType + ",操作対象：" + actionViewNm + "." + actionEleNm + ",操作値："
                + actionOpeValue + "】";
        try {

            // 処理開始====================================
            if (!RakurakuConst.WEB_VARIABLE_SET.equals(actionType)) {
                actionOpeValue = RakurakuWebUtils.getRealValueAfterReplace(actionOpeValue);
            }
            String targetPartialUrl = actionViewNm.split(RakurakuConst.LEFT_BRACK_2BYTE)[0];
            targetPartialUrl = targetPartialUrl.split(RakurakuConst.SPLIT_LEFT_BRACK)[0];
            WebElement editELe = null;
            List<WebElement> editELeList = null;
            switch (actionType) {
                case RakurakuConst.WEB_URL_REDIRECT:
                    operateWebGetUrl(actionOpeValue);
                    break;

                case RakurakuConst.WEB_FRAME_SWITCH:
                    operateWebFrameSwitch(targetPartialUrl, actionOpeValue);
                    break;

                case RakurakuConst.WEB_TEXTBOX:
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    operateWebTextBox(editELe, actionOpeWay, actionOpeValue);
                    break;

                case RakurakuConst.WEB_RADIO:
                case RakurakuConst.WEB_CHECKBOX:
                case RakurakuConst.WEB_BUTTON_LINK:
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    operateWebSaveElementPosition(editELe, actionType);
                    operateWebClick(editELe, actionOpeWay, actionOpeValue);
                    break;

                case RakurakuConst.WEB_PULLDOWN:
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    operateWebSelect(editELe, actionOpeValue);
                    break;

                // TODO case "マルチプルダウンリスト":break;

                case RakurakuConst.WEB_FILEBOX:
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    operateWebFileBox(editELe, actionOpeWay, actionOpeValue);
                    break;

                case RakurakuConst.WEB_DETAIL_LIST:
                    editELeList = operateWebGetElements(targetPartialUrl, actionEle, actionEleWay);
                    operateWebList(editELeList, actionOpeWay, actionOpeValue);
                    break;

                case RakurakuConst.WEB_DIALOG:
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    operateWebDialog(editELe, actionOpeValue);
                    break;

                case RakurakuConst.WEB_SLEEP:
                    if (StringUtils.isNumeric(actionOpeValue)) {
                        Thread.sleep(Long.parseLong(actionOpeValue) * 1000);
                    }
                    break;

                case RakurakuConst.WEB_FOCUS:
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    editELe.click();
                    break;

                case RakurakuConst.WEB_TAB_KEY:
                    Robot robot = new Robot();
                    robot.keyPress(KeyEvent.VK_TAB);
                    robot.keyRelease(KeyEvent.VK_TAB);
                    robot.delay(100);
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    if (!editELe.getLocation().equals(driver.switchTo().activeElement().getLocation())) {
                        throw new RakurakuException("【タブキー確認】タブキーを押下した後、フォーカスが想定外要素へ飛びました。");
                    }
                    break;

                case RakurakuConst.WEB_OUTPUT:
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    operateWebOutput(editELe, actionOpeValue);
                    break;

                case RakurakuConst.WEB_PAGE_CLOSE:
                    RakurakuWebUtils.webPageClose(driver);
                    break;

                case RakurakuConst.WEB_PAGE_BACK:
                    RakurakuWebUtils.webPageBack(driver);
                    break;

                case RakurakuConst.WEB_PAGE_FORWARD:
                    RakurakuWebUtils.webPageForward(driver);
                    break;

                case RakurakuConst.WEB_VARIABLE_SET:
                    operateWebSetVarible(targetPartialUrl, actionEle, actionEleWay, actionOpeWay, actionEleNm,
                            actionOpeValue);
                    break;

                case RakurakuConst.WEB_CAPTURE_FULLSCREEN:
                    guiCamera.snapshotFullScreen(driver);
                    break;

                case RakurakuConst.WEB_CAPTURE_SCROLLSCREEN:
                    guiCamera.snapshotScrollScreen(driver);
                    break;

                default:
                    editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
                    operateWebTextBox(editELe, actionOpeWay, actionOpeValue);
                    break;
            }
            // 処理終了====================================

            logging(logOpeNm, startTime, "OK");
        } catch (Throwable e) {
            logging(logOpeNm, startTime, "NG");
            throw new RakurakuException("【操作(画面)】【" + actionViewNm + "." + actionEleNm + "】の操作に失敗しました。", e);
        }
    }

    /**
     * 操作(バッチ)
     *
     * @param batchName
     * @param batchCmd
     * @param params
     * @throws Exception
     */
    public void operateBatch(String batchName, String batchCmd, String params) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        try {

            // 処理開始====================================
            RakurakuWebUtils.runBat(batchCmd, params);
            // 処理終了====================================

            logging("【操作(バッチ)】【" + batchName + "】", startTime, "OK");
        } catch (Throwable e) {
            logging("【操作(バッチ)】【" + batchName + "】", startTime, "NG");
            throw new RakurakuException("【操作(バッチ)】【" + batchName + "】の操作に失敗しました。", e);
        }
    }

    public void DOCKERYaml(String funcId,int maxYanml) throws Exception {
        for (int j = 1; j < maxYanml + 1; j++) {
            String copyPath = RakurakuFileUtils.getToolPath() + "/testdata/mockapis/" + funcId + "/" + funcId + j + ".yaml";
            if(!new File(copyPath).exists()) {
                System.out.println(copyPath + "は存在しません");
                throw new RakurakuException(copyPath + "は存在しません");
            }
            String yamlPath = RakurakuDBUtils.getProps().getProperty("DOCKER_YAML" + j);
            FileUtils.copyFile(new File(copyPath), new File(yamlPath));
            String a = "";
        }

    }

    public void operateYamls(String mockApiNm,String apiName ,String example,String maxYaml) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        String wPath = "E:\\ApiBatch.log";
        File filelog =new File(wPath);
        if(!filelog.exists()) {
            filelog.createNewFile();
        }
        FileWriter fw = null;
        fw = new FileWriter(wPath,true);
        fw.write(mockApiNm  + "::" + example + "\r\n");
        fw.close();
//            flg1 = "ok";
//            if (flg = true){
//                flg = false;
//                int a = Integer.parseInt(maxYaml);
//                s1 = yamlName.substring(0, yamlName.length() - 1);
//                DOCKERYaml(s1,a);
//            }
        try {

            // 処理開始====================================
            if(StringUtils.isNotBlank(RakurakuWebUtils.mockHeaders)) {
                RakurakuWebUtils.mockHeaders = RakurakuWebUtils.mockHeaders + "&" + mockApiNm + "->" + example;
            }else {
                RakurakuWebUtils.mockHeaders = "Prefer=" + mockApiNm + "->" + example;
            }

            apiMock = apiMock+ mockApiNm + "::" + apiName + "::" + example + "\\r\\n";
            // 処理終了====================================

            logging("【操作(api)】【" + mockApiNm + "】", startTime, "OK");
        } catch (Throwable e) {
            logging("【操作(api)】【" + mockApiNm + "】", startTime, "NG");
            throw new RakurakuException("【操作(api)】【" + mockApiNm + "】の操作に失敗しました。", e);
        }
    }

    public void operateMockApi(String mockApiNm,String apiName ,String example) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");

    	String wPath = "E:\\ApiBatch.log";
        File filelog =new File(wPath);
        if(!filelog.exists()) {
            filelog.createNewFile();
        }
        FileWriter fw = null;
        fw = new FileWriter(wPath,true);
        fw.write(mockApiNm  + "::" + example + "\r\n");
        fw.close();
        try {

            // 処理開始====================================
        	if(StringUtils.isNotBlank(RakurakuWebUtils.mockHeaders)) {
        		RakurakuWebUtils.mockHeaders = RakurakuWebUtils.mockHeaders + "&" + mockApiNm + "->" + example;
        	}else {
        		RakurakuWebUtils.mockHeaders = "Prefer=" + mockApiNm + "->" + example;
        	}

            // 処理終了====================================

            logging("【操作(api)】【" + mockApiNm + "】", startTime, "OK");
        } catch (Throwable e) {
            logging("【操作(api)】【" + mockApiNm + "】", startTime, "NG");
            throw new RakurakuException("【操作(api)】【" + mockApiNm + "】の操作に失敗しました。", e);
        }
    }

    /**
     * 操作(api)
     *
     * @param apiNm
     * @param
     * @param callMeth
     * @param
     * @param apiKind
     * @throws Exception
     */
    public void operateApi(String apiNm, String apiKind, String callMeth, String requestHeaders, String loginUser,
            String requestBody) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");

        try {
            // 処理開始====================================
        	apiKind = RakurakuWebUtils.getRealValueAfterReplace(apiKind);
            callMeth = RakurakuWebUtils.getRealValueAfterReplace(callMeth);
            RakurakuWebUtils.callApi(apiNm, apiKind, callMeth, requestHeaders, requestBody, loginUser, guiCamera);

            // 処理終了====================================

            logging("【操作(api)】【" + apiNm + "】", startTime, "OK");

        } catch (Throwable e) {
            logging("【操作(api)】【" + apiNm + "】", startTime, "NG");
            throw new RakurakuException("【操作(api)】【" + apiNm + "】の操作に失敗しました。", e);
        }

    }

    public void operateApiConfirm(String apiKind, String notAssertColumns, String expected) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        if (expected.contains("{")) {

        } else {
            File file = new File(expected);
            BufferedReader reader = null;
            StringBuffer sbf = new StringBuffer();
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            expected = sbf.toString();
        }
        try {

            // handle begin====================================
            apiKind = RakurakuWebUtils.getRealValueAfterReplace(apiKind);
            String[] notAssertColumn = new String[] {};
            if (!"".equals(notAssertColumns) && !"NOT_ASSERT_COLUMN".equals(notAssertColumns)) {
                if (notAssertColumns.contains("NOT_ASSERT_COLUMN")) {
                    notAssertColumns = notAssertColumns.replace("NOT_ASSERT_COLUMN", "");
                    notAssertColumn = notAssertColumns.split(",");
                }
            }
            RakurakuWebUtils.apiConfirm(apiKind, expected, notAssertColumn, guiCamera);
            // handle end====================================

            logging("【操作(api確認】【" + apiKind
                    + "】", startTime, "OK");
        } catch (Throwable e) {
            logging("【操作(api確認】【" + apiKind
                    + "】", startTime, "NG");
            throw new RakurakuException(
                    "【操作(api確認】【" + apiKind
                            + "】の操作に失敗しました。",
                    e);
        }
    }

    /**
     * 操作(csvチェック)
     *
     * @param csvName
     * @param
     * @param
     * @param notAssertColumns
     * @throws Exception
     */
    public void operateCsvCheck(String csvName, String notAssertColumns, String opeVal) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        try {

            // 処理開始====================================
            RakurakuWebUtils.checkCSV(csvName, notAssertColumns, opeVal, guiCamera);
            // 処理終了====================================

            logging("【操作(csvチェック)】【CSV名称：" + csvName + ",CSV状態：" + opeVal + "】", startTime, "OK");
        } catch (Throwable e) {
            logging("【操作(csvチェック)】【CSV名称：" + csvName + ",CSV状態：" + opeVal + "】", startTime, "NG");
            throw new RakurakuException("【操作(csvチェック)】【CSV名称：" + csvName + ",CSV状態：" + opeVal + "】の操作に失敗しました。", e);
        }
    }

    /**
     * 操作(ログチェック)
     *
     * @param logName
     * @param logPath
     * @param actionOpeValue
     * @throws Exception
     */
    public void operateLogCheck(String logName, String logPath, String actionOpeValue) throws Exception {
        String startTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        try {

            // 処理開始====================================
            if (RakurakuConst.HIGH_ROUND.equals(actionOpeValue) || RakurakuConst.LOW_ROUND.equals(actionOpeValue)) {
                RakurakuWebUtils.checkLog(logName, guiCamera);
            }
            // 処理終了====================================

            logging("【操作(ログチェック)】【" + logName + "】", startTime, "OK");
        } catch (Throwable e) {
            logging("【操作(ログチェック)】【" + logName + "】", startTime, "NG");
            throw new RakurakuException("【操作(ログチェック)】【" + logName + "】の操作に失敗しました。", e);
        }
    }

    /**
     * URLリダイレクト
     *
     * @param url
     * @throws Exception
     */
    private void operateWebGetUrl(String url) throws Exception {
        boolean firstFlag = false;
        if (driver == null) {
            firstFlag = true;
            if (driverType.contains(RakurakuConst.BROWSER_IE)) {
                driver = RakurakuWebUtils.getIEDriver();
            } else if (RakurakuConst.BROWSER_FIREFOX.equals(driverType)) {
                driver = RakurakuWebUtils.getFireFoxDriver();
            } else if (RakurakuConst.BROWSER_CHROME.equals(driverType)) {
                driver = RakurakuWebUtils.getChromeDriver();
            } else {
                driver = RakurakuWebUtils.getSPDriver(driverType);
            }
        }
        driver.get(url);
        RakurakuWebUtils.waitForLoad(driver);
        if (!firstFlag) {
            guiCamera.snapshotFullScreen(driver);
        }
    }

    /**
     * Frameスイッチ
     *
     * @param targetPartialUrl
     * @param actionOpeValue
     * @throws Exception
     */
    private void operateWebFrameSwitch(String targetPartialUrl, String actionOpeValue) throws Exception {
        RakurakuWebUtils.jumpToView(driver, targetPartialUrl);
        RakurakuWebUtils.waitForLoad(driver);
        if (StringUtils.isNumeric(actionOpeValue)) {
            driver.switchTo().frame(Integer.parseInt(actionOpeValue));
        } else {
            driver.switchTo().frame(actionOpeValue);
        }
        Thread.sleep(500);
    }

    /**
     * テキストボックス
     *
     * @param
     * @param editELe
     * @param actionOpeWay
     * @throws Exception
     */
    private void operateWebTextBox(WebElement editELe, String actionOpeWay, String actionOpeValue) throws Exception {
        switch (actionOpeWay) {
            case RakurakuConst.SELENIUM_ACTIONS:
                Actions action = new Actions(driver);
                action.click(editELe).perform();
                RakurakuWebUtils.webClearElementValue(editELe, driver);
                action.sendKeys(editELe, actionOpeValue).perform();
                break;
            case RakurakuConst.SELENIUM_JS:
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click()", editELe);
                js.executeScript("arguments[0].value='" + actionOpeValue + "'", editELe);
                break;
            default:
                editELe.click();
                editELe.clear();
                editELe.sendKeys(actionOpeValue);
                break;
        }
    }

    /**
     * ラジオボタン,チェックボックス,ボタンリンク
     *
     * @param
     * @param editELe
     * @param actionOpeWay
     * @param actionOpeValue
     * @throws Exception
     */
    private void operateWebClick(WebElement editELe, String actionOpeWay, String actionOpeValue) throws Exception {
        // シングルクリック
        if (RakurakuConst.HIGH_ROUND.equals(actionOpeValue) || RakurakuConst.LOW_ROUND.equals(actionOpeValue)) {
            switch (actionOpeWay) {
                case RakurakuConst.SELENIUM_ACTIONS:
                    Actions action = new Actions(driver);
                    action.click(editELe).perform();
                    break;
                case RakurakuConst.SELENIUM_JS:
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].click()", editELe);
                    break;
                default:
                    editELe.click();
                    break;
            }
        }

        // ダブルクリック
        if (RakurakuConst.DOUBLE_ROUND.equals(actionOpeValue)) {
            Actions action = new Actions(driver);
            action.doubleClick(editELe).perform();
        }

        // コンテキストクリック
        if (RakurakuConst.SOLID_ROUND.equals(actionOpeValue)) {
            Actions action = new Actions(driver);
            action.contextClick(editELe).perform();
        }

        // マウスオバー
        if (RakurakuConst.SOLID_ANGLE.equals(actionOpeValue)) {
            Actions action = new Actions(driver);
            action.moveToElement(editELe).perform();
            Thread.sleep(1000);
        }
    }

    /**
     * プルダウンリスト
     *
     * @param
     * @param editELe
     * @param actionOpeValue
     * @throws Exception
     */
    private void operateWebSelect(WebElement editELe, String actionOpeValue) throws Exception {
        new Select(editELe).selectByVisibleText(actionOpeValue);
    }

    /**
     * ファイルボックス
     *
     * @param
     * @param editELe
     * @param actionOpeWay
     * @param actionOpeValue
     * @throws Exception
     */
    private void operateWebFileBox(WebElement editELe, String actionOpeWay, String actionOpeValue) throws Exception {
        switch (actionOpeWay) {
            case RakurakuConst.SELENIUM_ACTIONS:
            case RakurakuConst.SELENIUM_JS:
                Actions action = new Actions(driver);
                action.doubleClick(editELe).perform();
                RakurakuWebUtils.webUploadFileInputForIE(actionOpeValue);
                break;
            default:
                editELe.sendKeys(actionOpeValue);
                break;
        }
    }

    /**
     * 一覧リスト
     *
     * @param
     * @param editELeList
     * @param actionOpeWay
     * @param actionOpeValue
     * @throws Exception
     */
    private void operateWebList(List<WebElement> editELeList, String actionOpeWay, String actionOpeValue)
            throws Exception {
        if (actionOpeValue.length() < 3) {
            return;
        }
        actionOpeValue = actionOpeValue.substring(1, actionOpeValue.length() - 1);
        String[] valueAttr = actionOpeValue.split(RakurakuConst.COMMA);
        for (String index : valueAttr) {
            switch (actionOpeWay) {
                case RakurakuConst.SELENIUM_ACTIONS:
                    Actions action = new Actions(driver);
                    action.click(editELeList.get(Integer.parseInt(index))).perform();
                    break;
                case RakurakuConst.SELENIUM_JS:
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].click()", editELeList.get(Integer.parseInt(index)));
                    break;
                default:
                    editELeList.get(Integer.parseInt(index)).click();
                    break;
            }
        }
    }

    /**
     * ダイアログ(alert,confirm,prompt)
     *
     * @param
     * @param editELe
     * @param
     * @param actionOpeValue
     * @throws Exception
     */
    private void operateWebDialog(WebElement editELe, String actionOpeValue) throws Exception {
        // シングルクリック
        Thread.sleep(500);
        if (RakurakuConst.HIGH_ROUND.equals(actionOpeValue) || RakurakuConst.LOW_ROUND.equals(actionOpeValue)) {
            driver.switchTo().alert().accept();
        } else if (RakurakuConst.CROSS.equals(actionOpeValue)) {
            driver.switchTo().alert().dismiss();
        } else {
            driver.switchTo().alert().sendKeys(actionOpeValue);
            driver.switchTo().alert().accept();
        }
        Thread.sleep(500);
    }

    /**
     * 出力確認
     *
     * @param editELe
     * @param actionOpeValue
     * @throws Exception
     */
    private void operateWebOutput(WebElement editELe, String actionOpeValue) throws Exception {
        boolean retFlag;
        String msgStr = "";
        switch (actionOpeValue) {
            case RakurakuConst.SOLID_ROUND:// 活性
                retFlag = editELe.isEnabled();
                msgStr = "活性非活性の検証に失敗しました。予想：活性。実際：非活性。";
                break;

            case RakurakuConst.HIGH_ROUND:// 非活性
            case RakurakuConst.LOW_ROUND:// 非活性
                retFlag = !editELe.isEnabled();
                msgStr = "活性非活性の検証に失敗しました。予想：非活性。実際：活性。";
                break;

            case RakurakuConst.DOUBLE_ROUND:// 選択されている
                retFlag = editELe.isSelected();
                msgStr = "選択非選択の検証に失敗しました。予想：選択されている。実際：選択されていない。";
                break;

            case RakurakuConst.CROSS:// 選択されていない
                retFlag = !editELe.isSelected();
                msgStr = "選択非選択の検証に失敗しました。予想：選択されていない。実際：選択されている。";
                break;

            case RakurakuConst.SOLID_ANGLE:// 表示
                retFlag = editELe.isDisplayed();
                msgStr = "表示非表示の検証に失敗しました。予想：表示される。実際：表示されていない。";
                break;

            case RakurakuConst.HOLLOW_ANGLE:// 非表示
                retFlag = !editELe.isDisplayed();
                msgStr = "表示非表示の検証に失敗しました。予想：表示されいない。実際：表示されている。";
                break;

            default:// テキスト
                String value = editELe.getAttribute("value");
                retFlag = actionOpeValue.equals(value);
                String text = "";
                if (!retFlag) {
                    text = editELe.getText();
                    retFlag = actionOpeValue.equals(text);
                }
                msgStr = "出力内容の検証に失敗しました。予想：” + actionOpeValue + ”。実際：属性value=" + value + ";htmltext=" + text + "。";
                break;
        }
        if (!retFlag) {
            throw new RakurakuException(msgStr);
        }
    }

    private void operateWebSetVarible(String targetPartialUrl, String actionEle, String actionEleWay,
            String actionOpeWay, String variableVal, String variableNm) throws Exception {
        if (!Pattern.compile("[$][{].*?[}]").matcher(variableNm).matches()) {
            throw new RakurakuException("変数は「${variable}」の形にしてください。");
        }
        if (StringUtils.isNotBlank(actionEle) && StringUtils.isNotBlank(actionEleWay)
                && StringUtils.isNotBlank(actionOpeWay)) {
            WebElement editELe = operateWebGetElement(targetPartialUrl, actionEle, actionEleWay);
            variableVal = editELe.getAttribute("value");
            if (StringUtils.isNotBlank(variableVal)) {
                variableVal = editELe.getText();
            }
            variablesMap.put(variableNm, variableVal);
        } else if (StringUtils.isNotBlank(variableVal)) {
            variablesMap.put(variableNm, variableVal);
        } else {
            throw new RakurakuException("設定したい変数はみ見つかりませんでした。");
        }
    }

    /**
     * 画面要素取得
     *
     * @param actionEle
     * @param actionEleWay
     * @throws Exception
     */
    private WebElement operateWebGetElement(String targetPartialUrl, String actionEle, String actionEleWay)
            throws Exception {
        RakurakuWebUtils.jumpToView(driver, targetPartialUrl);
        RakurakuWebUtils.waitForLoad(driver);
        By by = null;
        switch (actionEleWay) {
            case RakurakuConst.BY_ID:
                by = By.id(actionEle);
                break;
            case RakurakuConst.BY_NAME:
                by = By.name(actionEle);
                break;
            case RakurakuConst.BY_XPATH:
                by = By.xpath(actionEle);
                break;
            case RakurakuConst.BY_LINKTEXT:
                by = By.linkText(actionEle);
                break;
            case RakurakuConst.BY_PARTIALLINKTEXT:
                by = By.partialLinkText(actionEle);
                break;
            case RakurakuConst.BY_TAGNAME:
                by = By.tagName(actionEle);
                break;
            case RakurakuConst.BY_CSSSELECTOR:
                by = By.cssSelector(actionEle);
                break;
            case RakurakuConst.BY_CLASSNAME:
                by = By.className(actionEle);
                break;
            default:
                by = By.id(actionEle);
                break;
        }
        WebElement we = null;
        try {
            we = driver.findElement(by);
        } catch (NoSuchElementException e) {
            throw new RakurakuException("画面要素が見つかりませんでした。", e);
        }
        return we;
    }

    /**
     * 画面要素複数取得
     *
     * @param actionEle
     * @param actionEleWay
     * @throws Exception
     */
    private List<WebElement> operateWebGetElements(String targetPartialUrl, String actionEle, String actionEleWay)
            throws Exception {
        RakurakuWebUtils.jumpToView(driver, targetPartialUrl);
        RakurakuWebUtils.waitForLoad(driver);
        By by = null;
        switch (actionEleWay) {
            case RakurakuConst.BY_ID:
                by = By.id(actionEle);
                break;
            case RakurakuConst.BY_NAME:
                by = By.name(actionEle);
                break;
            case RakurakuConst.BY_XPATH:
                by = By.id(actionEle);
                break;
            case RakurakuConst.BY_LINKTEXT:
                by = By.linkText(actionEle);
                break;
            case RakurakuConst.BY_PARTIALLINKTEXT:
                by = By.partialLinkText(actionEle);
                break;
            case RakurakuConst.BY_TAGNAME:
                by = By.tagName(actionEle);
                break;
            case RakurakuConst.BY_CSSSELECTOR:
                by = By.cssSelector(actionEle);
                break;
            case RakurakuConst.BY_CLASSNAME:
                by = By.className(actionEle);
                break;
            default:
                by = By.id(actionEle);
                break;
        }
        List<WebElement> weList = null;
        try {
            weList = driver.findElements(by);
        } catch (NoSuchElementException e) {
            throw new RakurakuException("画面要素リストが見つかりませんでした。", e);
        }
        return weList;
    }

    /**
     * 画面ボタン要素位置保存
     *
     * @param we
     * @param actionType
     */
    private void operateWebSaveElementPosition(WebElement we, String actionType) throws Exception {
        if (!"Y".equals(RakurakuDBUtils.getProps().getProperty("CAPTURE_LINE_MARK"))) {
            return;
        }
        if (!RakurakuConst.WEB_BUTTON_LINK.equals(actionType) || StringUtils.isBlank(RakurakuCaptureUtils.nowPicName)
                || !RakurakuCaptureUtils.lastPicUrl.equals(driver.getCurrentUrl())) {
            return;
        }
        int x = we.getLocation().getX() * 72 / 96;
        int y = we.getLocation().getY() * 72 / 96;
        int w = we.getSize().getWidth() * 72 / 96;
        int h = we.getSize().getHeight() * 72 / 96;
        String eachPosition = x + "," + y + "," + w + "," + h;
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(eachEviPath + "/Rakuraku_Work/WebActionPositions.properties", true), "UTF-8"));
        bw.write(RakurakuCaptureUtils.nowPicName + "=" + eachPosition + "\r\n");
        bw.close();
    }

    private void logging(String operateNm, String startTime, String result) {
        RakurakuCore.logBuilder.append("【実行結果=" + result + "】");
        RakurakuCore.logBuilder.append(operateNm);
        RakurakuCore.logBuilder.append(":");
        RakurakuCore.logBuilder.append("開始時間=" + startTime);
        RakurakuCore.logBuilder.append(";終了時間=" + RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS"));
        RakurakuCore.logBuilder.append("\n");
    }

    public static void main(String args[]) throws Exception {
    }

}
