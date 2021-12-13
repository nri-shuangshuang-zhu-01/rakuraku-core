package com.linkage.rakuraku.util.other;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.linkage.rakuraku.core.RakurakuCore;

public class AnalysisReport {

    private static String JENKINS_HOME;
    private static String JOB_NAME;
    private static String BUILD_NUMBER;

    public static void main(String[] args) {
        JENKINS_HOME = args[0];
        JOB_NAME = args[1];
        BUILD_NUMBER = args[2];
        analysis();

    }

    private static void analysis() {

        File file = null;
        BufferedReader br = null;
        BufferedWriter bw = null;

        String line = "";
        List<String> outList = new ArrayList<String>();
        String spr = "\"";
        String tmp = "";
        try {
            file = new File(JENKINS_HOME + "/jobs/" + JOB_NAME + "/builds/" + BUILD_NUMBER + "/log");
            // file = new File("C:/Users/liufen/.jenkins/jobs/MAUN/builds/11/log");
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "MS932"));
            while ((line = br.readLine()) != null) {
                // タイトル
                if (line.contains("init:")) {
                    line = spr + RakurakuCore.runDate + " AUTO UTest Result" + spr + ",";
                    line = line + spr + "Runs" + spr + ",";
                    line = line + spr + "Failures" + spr + ",";
                    line = line + spr + "Errors" + spr + ",";
                    line = line + spr + "Skipped" + spr + ",";
                    line = line + spr + "Time" + spr + ",";
                    outList.add(line);
                }

                // 内容
                if (line.contains("[junit] Running")) {
                    tmp = spr + line.split("\\.")[line.split("\\.").length - 1] + spr + ",";
                }
                if (line.contains("[junit] Tests run")) {
                    String[] elemtStrings = line.split(":");
                    tmp = tmp + spr + elemtStrings[1].split(",")[0].trim() + spr + ",";
                    tmp = tmp + spr + elemtStrings[2].split(",")[0].trim() + spr + ",";
                    tmp = tmp + spr + elemtStrings[3].split(",")[0].trim() + spr + ",";
                    tmp = tmp + spr + elemtStrings[4].split(",")[0].trim() + spr + ",";
                    tmp = tmp + spr + elemtStrings[5].trim() + spr;
                    outList.add(tmp);
                    tmp = "";
                }

                // Analysis Complete
                if (line.contains("test.report:")) {
                    break;
                }
            }
            file = new File("./AnalysisReport.csv");
            file.delete();
            file.createNewFile();
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "MS932"));
            for (String string : outList) {
                System.out.println(string);
                bw.write(string + "\n");
            }
            System.out.println("OUTPUT COMPLETE!!!");
        } catch (Exception e) {
            System.out.println(line);
            System.out.println("Analysis Failed!");
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
