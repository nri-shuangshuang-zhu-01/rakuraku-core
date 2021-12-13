package com.linkage.rakuraku.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import com.linkage.rakuraku.core.RakurakuCore;

public class RakurakuFileUtils {

    /**
     * ケース実施前に、指定したフォルダーの中に、日付フォルダーを作成する
     *
     * @throws Exception
     */
    public static void createFloder() throws Exception {
        if (StringUtils.isBlank(RakurakuDBUtils.getProps().getProperty("FOLDER_CREATE_BFRORE_CASE"))) {
            return;
        }
        String[] crtfolders = RakurakuDBUtils.getProps().getProperty("FOLDER_CREATE_BFRORE_CASE").split(",");
        File file;
        for (String folderPth : crtfolders) {
            file = new File(folderPth + "/" + RakurakuDateUtils.getNowDateOrTime("yyyyMMdd"));
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    /**
     * クリアエビデンス
     *
     * @param
     * @throws Exception
     */
    public static void delAllFile() throws Exception {
        File file = new File(RakurakuCore.eachEviPath);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        FileUtils.forceDelete(file);
    }

    /**
     * ケース毎のエビデンスパス取得
     * 
     * @param casePath
     */
    public static void getEachEviPath(String casePath) {
        RakurakuCore.eachEviPath = getToolPath() + "/testresult/" + RakurakuCore.runDate + casePath;
    }

    /**
     * ケース毎のエビデンスパス取得
     * 
     * @param
     */
    public static String getEachInputPath() {
        return getToolPath() + "/src/test/resources/" + RakurakuDBUtils.testClassName + "/inputdb/";
    }

    /**
     * ケース毎のワークパス作成
     */
    public static void makeEachEviDir() {
        File file = new File(RakurakuCore.eachEviPath + "/Rakuraku_Work/DB情報");
        file.mkdirs();
        file = new File(RakurakuCore.eachEviPath + "/Rakuraku_Work/downloads");
        file.mkdirs();
    }

    /**
     * テストケース執行OKの場合、ログファイルに記入する
     *
     * @param
     */
    public static void logFileOK() {
        File file = new File(RakurakuCore.eachEviPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        File out = new File(RakurakuCore.eachEviPath + "/rakuraku.log");
        try {
            out.createNewFile();
            String time = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
            FileWriter fw = new FileWriter(out);
            fw.write("ケース番号=" + RakurakuCore.resultPath.split("/")[2].replace("case_", "No."));
            fw.write(";実施開始時間=" + RakurakuDBUtils.runTime);
            fw.write(";実施終了時間=" + time);
            fw.write(";実施結果=OK.\r\n");
            fw.write("======以下はアクション毎の確認ログ。======\r\n");
            fw.write(RakurakuCore.logBuilder.toString());
            fw.flush();
            fw.close();
            System.out.println("---------------------------------Start "
                    + RakurakuCore.resultPath.split("/")[2].replace("case_", "No.")
                    + "---------------------------------");
            System.out.print(RakurakuCore.logBuilder.toString());
            System.out.println("---------------------------------End "
                    + RakurakuCore.resultPath.split("/")[2].replace("case_", "No.")
                    + "---------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * テストケース執行NGの場合、ログファイルに記入する
     *
     * @param
     */
    public static void logFileNG(String message) {
        File file = new File(RakurakuCore.eachEviPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        File out = new File(RakurakuCore.eachEviPath + "/rakuraku.log");
        out.delete();
        try {
            out.createNewFile();
            String time = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
            OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(out, true), "UTF-8");
            fw.write("ケース番号=" + RakurakuCore.resultPath.split("/")[2].replace("case_", "No."));
            fw.write(";実施開始時間=" + RakurakuDBUtils.runTime);
            fw.write(";実施終了時間=" + time);
            fw.write(";実施結果=NG.\r\n");
            fw.write(StringUtils.trimToEmpty(message) + "\r\n");
            fw.write("======以下はアクション毎の確認ログ。======\r\n");
            fw.write(RakurakuCore.logBuilder.toString());
            fw.flush();
            fw.close();
            System.out.println("---------------------------------Start "
                    + RakurakuCore.resultPath.split("/")[2].replace("case_", "No.")
                    + "---------------------------------");
            System.out.print(RakurakuCore.logBuilder.toString());
            System.out.println(StringUtils.trimToEmpty(message));
            System.out.println("---------------------------------End "
                    + RakurakuCore.resultPath.split("/")[2].replace("case_", "No.")
                    + "---------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * テスト工程パス取得
     * 
     * @return
     */
    public static String getToolPath() {
        return System.getProperty("user.dir");
    }

    /**
     * ファイルから変数の置換え
     * 
     * @param filePath
     */
    public static void replaceVariableFromFile(String filePath) throws Exception {
        File file = new File(filePath);
        String lines = FileUtils.readFileToString(file, "UTF-8");
        lines = RakurakuWebUtils.getRealValueAfterReplace(lines);
        FileUtils.writeStringToFile(file, lines, "UTF-8");
    }

    /**
     * エビデンス作成
     * 
     * @throws Exception
     */
    public static void evidenceTool(String str) throws Exception {
        deleteChromeLog();
        String fileName = getToolPath() + "/testresult/";
        File file = new File(fileName + RakurakuCore.runDate);
        if (!file.exists()) {
            file.mkdirs();
        }

        RakurakuJacobExcelUtils tool = new RakurakuJacobExcelUtils();
        try {
            System.out.print("-エビデンス-　");
            String toolPath = getToolPath() + "/testtools/Rakuraku-Ver4.0/Utility/02_エビデンス作成ツール/"
                    + "DealEvidenceTool.xlsm";

            tool.OpenExcel(toolPath, true, false);
            tool.callMacro("java_Click", RakurakuCore.runDate + "::" + getToolPath() + "\\testresult\\"
                    + RakurakuCore.runDate + "\\" + RakurakuDBUtils.testClassName + "::" + str);
            System.out.println(getToolPath() + "\\testresult\\" + RakurakuDBUtils.testClassName + "_"
                    + RakurakuCore.runDate + ".xlsx");
        } catch (Exception e) {

        } finally {
            tool.CloseExcel(false, true);
        }
        CoverageTCP(RakurakuDBUtils.testClassName);
        RakurakuDBUtils.killOldProgress();
    }

    /**
     * サーバー側カバレッジ取得
     * 
     * @param testClassNm
     */
    private static void CoverageTCP(String testClassNm) {
        try {
            String resultPath = getToolPath() + "\\testresult\\" + RakurakuCore.runDate + "\\" + testClassNm + "\\"
                    + testClassNm + "_" + RakurakuCore.runDate + ".exec";
            final FileOutputStream localFile = new FileOutputStream(resultPath);
            final ExecutionDataWriter localWriter = new ExecutionDataWriter(localFile);

            final Socket socket = new Socket(InetAddress.getByName("localhost"), 8395);
            final RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
            final RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());

            reader.setSessionInfoVisitor(localWriter);
            reader.setExecutionDataVisitor(localWriter);

            writer.visitDumpCommand(true, false);
            reader.read();

            socket.close();
            localFile.close();
            System.out.println("-カバレッジ-　サーバー端カバレッジ分析用ファイルが下記作成されました。");
            System.out.println(resultPath);
        } catch (Exception e) {
            System.out.println("-カバレッジ-　サーバー端カバレッジ分析用ファイルが作成されていません。");
        }
    }

    /**
     * Chromeキャプチャ用ログ削除
     */
    private static void deleteChromeLog() {
        try {
            FileUtils.forceDelete(new File(getToolPath() + "/chromedriver.log"));
        } catch (Exception e) {
        }
    }

}
