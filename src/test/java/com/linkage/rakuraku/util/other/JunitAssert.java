package com.linkage.rakuraku.util.other;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import junit.framework.Assert;

@SuppressWarnings("deprecation")
public class JunitAssert {

    public static void assertData(Object expected, Object actual) throws Exception {
        assertData("", expected, actual, new String[] {});
    }

    public static void assertData(Object expected, Object actual, String... notAssertColumns) throws Exception {
        assertData("", expected, actual, notAssertColumns);
    }

    public static void assertData(String info, Object expected, Object actual) throws Exception {
        assertData(info, expected, actual, new String[] {});
    }

    /**
     * Compare for actual data and expected data(LIST MAP TableRecord)
     *
     * @param info Assert errot info
     * @param actual Data
     * @param expected Data
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes" })
    public static void assertData(String info, Object expected, Object actual, String... notAssertColumns)
            throws Exception {

        if (actual != null && expected != null) {
            if (!actual.getClass().toString().equals(expected.getClass().toString())) {
                Assert.fail(
                        info + "のJAVA形比較失敗" + "expected " + expected.getClass() + " but actual " + actual.getClass());
            } else {
                if (actual.getClass().toString()
                        .matches("int|char|short|byte|long|float|double|boolean|(class java\\.(math|lang)\\..*)")) {
                    Assert.assertEquals(info + "の値", expected, actual);
                } else if (actual.getClass().toString().matches("class java.+Timestamp||class java.+Date")) {
                    Assert.assertEquals(info + "の値", expected.toString(), actual.toString());
                } else if ((actual.getClass().toString().indexOf("[") != -1)
                        && (!actual.getClass().toString().startsWith("class"))) {
                    Assert.assertEquals(info + "の配列レングス", ((Object[]) expected).length, ((Object[]) actual).length);
                    for (int i = 0; i < ((Object[]) actual).length; i++) {
                        assertData(info + "[" + i + "]", ((Object[]) expected)[i], ((Object[]) actual)[i],
                                notAssertColumns);
                    }
                } else if (actual.getClass().toString().matches("(interface|class) java\\.util\\.(List|ArrayList)")) {
                    List datalist = (List) actual;
                    List exptlist = (List) expected;
                    Assert.assertEquals(info + "のリストサイズ", exptlist.size(), datalist.size());
                    for (int i = 0; i < datalist.size(); i++) {
                        assertData(info + "{List<" + datalist.get(i).getClass() + ">}(" + i + ")", exptlist.get(i),
                                datalist.get(i), notAssertColumns);
                    }
                } else if (actual.getClass().toString().matches("(interface|class) java\\.util\\.(Map|HashMap)")) {
                    Map datamap = (Map) actual;
                    Map exptmap = (Map) expected;

                    Assert.assertEquals(info + "のマップサイズ", exptmap.keySet().toArray().length,
                            datamap.keySet().toArray().length);

                    Iterator iterator = datamap.keySet().iterator();
                    while (iterator.hasNext()) {
                        Object key = iterator.next();
                        Boolean flag = true;
                        for (String notAssertKey : notAssertColumns) {
                            if (notAssertKey.equals(key)) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            Assert.assertEquals(info + "に[key=" + key + "]のExists", exptmap.containsKey(key),
                                    datamap.containsKey(key));
                            assertData(info + "に[key=" + key + "]のValue", exptmap.get(key), datamap.get(key),
                                    notAssertColumns);
                        }
                    }

                    iterator = exptmap.keySet().iterator();
                    while (iterator.hasNext()) {
                        Object key = iterator.next();
                        Boolean assertFlag = true;
                        for (String notAssertKey : notAssertColumns) {
                            if (notAssertKey.equals(key)) {
                                assertFlag = false;
                                break;
                            }
                        }
                        if (assertFlag) {
                            Assert.assertEquals(info + "に[key=" + key + "]のExists", exptmap.containsKey(key),
                                    datamap.containsKey(key));
                            assertData(info + "に[key=" + key + "]のValue", exptmap.get(key), datamap.get(key),
                                    notAssertColumns);
                        }
                    }
                    // }else
                    // if(actual.getClass().toString().matches(".+TableRecord")){
                    // Method[] ms = actual.getClass().getDeclaredMethods();
                    // for(int i = 0; i < ms.length ; i++){
                    // if(ms[i].getName().matches("get.+") &&
                    // !ms[i].getName().equals("getPKeyData") &&
                    // !ms[i].getName().equals("getValueData")){
                    // ms[i].setAccessible(true);
                    // assertData(info+"にTableRecordの" +
                    // ms[i].getName().replaceFirst("get",
                    // ""),ms[i].invoke(expected),ms[i].invoke(actual));
                    // }
                    // }
                } else {
                    Field[] fields = actual.getClass().getDeclaredFields();
                    for (int i = 0; i < fields.length; i++) {
                        fields[i].setAccessible(true);

                        // whether comparing flag
                        boolean assertFlg = getAssertFlg(fields[i].getName(), notAssertColumns);
                        if (!assertFlg) {
                            // when flag = false,not comparing
                            System.out.println("比較不要：" + fields[i].getName());
                            continue;
                        }

                        assertData(info + "の" + fields[i].getName(), fields[i].get(expected), fields[i].get(actual),
                                notAssertColumns);
                    }
                    if (!actual.getClass().getSuperclass().toString().matches("java..+")) {
                        fields = actual.getClass().getSuperclass().getDeclaredFields();
                        for (int i = 0; i < fields.length; i++) {
                            fields[i].setAccessible(true);

                            // whether comparing flag
                            boolean assertFlg = getAssertFlg(fields[i].getName(), notAssertColumns);
                            if (!assertFlg) {
                                // when flag = false,not comparing
                                System.out.println("比較不要：" + fields[i].getName());
                                continue;
                            }

                            assertData(info + "の" + fields[i].getName(), Util.getObject(expected, fields[i].getName()),
                                    Util.getObject(actual, fields[i].getName()), notAssertColumns);
                        }
                    }
                }
            }
        } else if (actual != null || expected != null) {
            Assert.fail(info + ":" + "expected " + (expected == null ? "null" : expected.toString()) + " but actual "
                    + (actual == null ? "null" : actual.toString()));
        }
    }

    /**
     * whether comparing flag
     *
     * @param fieldNm
     * @param notAssertColumns
     * @return
     */
    public static boolean getAssertFlg(String fieldNm, String... notAssertColumns) {
        boolean assertFlg = true;// whether comparing flag initial

        if (notAssertColumns == null) {
            return assertFlg;
        }

        for (String notAssertColumn : notAssertColumns) {
            if (fieldNm.equals(notAssertColumn) || fieldNm.equals("m_" + notAssertColumn)) {
                assertFlg = false;
                break;
            }
        }

        return assertFlg;
    }

    public static void assertJsonStr(String jsonExpt, String jsonActual, String[] notAssertColumns) throws Exception {
        JSONObject jsonObjExpt = new JSONObject(jsonExpt);
        JSONObject jsonObjActual = new JSONObject(jsonActual);
        Map<String, Object> exptMap = jsonObjExpt.toMap();
        Map<String, Object> actualMap = jsonObjActual.toMap();
        assertJsonMap(exptMap, actualMap, notAssertColumns, "APIの戻りJSON対象");
    }

    @SuppressWarnings("unchecked")
    public static void assertJsonMap(Map<String, Object> exptMap, Map<String, Object> actualMap,
            String[] notAssertColumns, String msgInfo) throws Exception {
        // 比較したくない項目をリスト化にする
        List<String> notAssertList = new ArrayList<String>(Arrays.asList(notAssertColumns));
        // サイズ比較
        assertData(msgInfo + "のサイズ", exptMap.size(), actualMap.size());
        for (Map.Entry<String, Object> entry : exptMap.entrySet()) {
            String keyExpt = entry.getKey();
            // 比較したくない項目をスキップする
            if (notAssertList.contains(keyExpt)) {
                continue;
            }
            Object vlObjExpt = entry.getValue();
            if (vlObjExpt instanceof String) {
                String vlExpt = (String) vlObjExpt;
                String vlActual = (String) actualMap.getOrDefault(keyExpt, "実際値が取得されていません");
                if (isDateTimeStr(vlActual)) {
                    continue;
                }
                assertData(msgInfo + "の項目" + keyExpt, vlExpt, vlActual, notAssertColumns);
            } else if (vlObjExpt instanceof ArrayList) {
                ArrayList<Map<String, Object>> jsonListExpt = (ArrayList<Map<String, Object>>) vlObjExpt;
                ArrayList<Map<String, Object>> jsonListActual = (ArrayList<Map<String, Object>>) actualMap
                        .getOrDefault(keyExpt, "実際値が取得されていません");
                assertData(msgInfo + "の項目" + keyExpt + "に対するリストサイズ", jsonListExpt.size(), jsonListActual.size());
                for (int i = 0; i < jsonListExpt.size(); i++) {
                    Map<String, Object> jsonObjExpt = (Map<String, Object>) jsonListExpt.get(i);
                    Map<String, Object> jsonObjActual = (Map<String, Object>) jsonListActual.get(i);
                    // 再帰呼び出す
                    assertJsonMap(jsonObjExpt, jsonObjActual, notAssertColumns,
                            msgInfo + "の項目" + keyExpt + "に対するリストの" + (i + 1) + "個目対象");
                }
            } else {
                assertData(msgInfo + "の項目" + keyExpt, vlObjExpt, actualMap.getOrDefault(keyExpt, "実際値が取得されていません"),
                        notAssertColumns);
            }
        }
    }

    public static boolean isDateTimeStr(String inputStr) {
        List<String> formatList = new ArrayList<String>();
        formatList.add("yyyyMMddHHmmssSSS");
        formatList.add("yyyy-MM-dd HH:mm:ss.SSS");
        formatList.add("yyyy-MM-dd HH:mm:ss,SSS");
        formatList.add("yyyy/MM/dd HH:mm:ss.SSS");
        formatList.add("yyyy/MM/dd HH:mm:ss,SSS");
        for (String format : formatList) {
            try {
                new SimpleDateFormat(format).parse(inputStr);
                return true;
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static void main(String[] args) throws ParseException {
    }
}
