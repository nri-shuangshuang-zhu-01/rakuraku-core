package com.linkage.rakuraku.util;

import com.linkage.rakuraku.core.RakurakuCore;
import com.linkage.rakuraku.util.other.SortedTableExt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.mssql.MsSqlConnection;
import org.dbunit.ext.mysql.MySqlConnection;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class RakurakuDBUtils {

    private static Properties COMMON_PROPS;

    protected static String testClassName = "";

    public static String runTime = "";

    public static String runUser = "";

    public static Map<String, Connection> dbConnectionsMap = new HashMap<String, Connection>();

    /**
     * ケースクラス初期化
     *
     * @param packageName
     * @param testClassName
     * @throws Exception
     */
    public static void initialize(String packageName, String testClassName) throws Exception {
        RakurakuDBUtils.testClassName = testClassName;
        killOldProgress();
        runTime = RakurakuDateUtils.getNowDateOrTime("yyyy-MM-dd HH:mm:ss.SSS");
        if (!"ON".equals(COMMON_PROPS.get("DB_SWITCH"))) {
            return;
        }

        // **********************************************************
        // initDbConn("");
        // String getTimeSql = "";
        // if ("ORACLE".equals(COMMON_PROPS.get("DB_TYPE"))) {
        // getTimeSql = "SELECT SYSDATE NOWTIME FROM DUAL";
        // } else if ("MYSQL".equals(COMMON_PROPS.get("DB_TYPE"))) {
        // getTimeSql = "SELECT NOW() NOWTIME";
        // } else if ("SQLSERVER".equals(COMMON_PROPS.get("DB_TYPE"))) {
        // getTimeSql = "select GETDATE() NOWTIME";
        // }
        // Statement stmt = dbConnectionsMap.get("").createStatement();
        // ResultSet rs = stmt.executeQuery(getTimeSql);
        // rs.next();
        // Date nowDt = rs.getTimestamp("NOWTIME");
        // **********************************************************
    }

    /**
     * Transactionコミット
     */
    public static void commit() {
        try {
            Set<Entry<String, Connection>> dbConnectionsSet = dbConnectionsMap.entrySet();
            for (Entry<String, Connection> dbConnection : dbConnectionsSet) {
                dbConnection.getValue().commit();
            }
        } catch (SQLException e) {
        }
    }

    /**
     * Transaction閉じる
     */
    public static void release() {
        try {
            Set<Entry<String, Connection>> dbConnectionsSet = dbConnectionsMap.entrySet();
            for (Entry<String, Connection> dbConnection : dbConnectionsSet) {
                dbConnection.getValue().close();
            }
        } catch (Exception e) {
        }
    }

    /**
     * 操作(DB準備) CUD
     *
     * @param tableName
     * @param opeType
     * @param tableNo
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public static void operateTable(String tableName, String opeType, String tableNo, RakurakuCaptureUtils guiCamera)
            throws Exception {
        if (!"ON".equals(COMMON_PROPS.get("DB_SWITCH"))) {
            return;
        }
        try {
            String DBNum = StringUtils.substringBefore(StringUtils.substringAfter(tableName, "["), "]");
            String tName = StringUtils.substringBefore(tableName, "[");
            IDatabaseConnection connection = getOperateFlatXmlConn(DBNum);

            String initDbFile = RakurakuFileUtils.getEachInputPath() + tableName + "_" + tableNo + ".xml";
            outputTableDefineXml(connection, tName, DBNum);
            String outxmlName = "■操作(DB準備)　" + opeType + "　" + tName + ".xml";
            String outXmlPath = guiCamera.getSeqNoPath() + "_rakurakuResult_" + outxmlName;
            FileUtils.copyFile(new File(initDbFile), new File(outXmlPath));

            RakurakuFileUtils.replaceVariableFromFile(outXmlPath);// XML変数変換

            IDataSet dataSet = new FlatXmlDataSet(new FileInputStream(outXmlPath));
            ReplacementDataSet rds = new ReplacementDataSet(dataSet);
            rds.addReplacementObject("null", null);
            connection.getConfig().setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
            connection.getConfig().setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, false);
            switch (opeType) {
                case "データクリア・挿入":
                    DatabaseOperation.CLEAN_INSERT.execute(connection, rds);
                    break;
                case "データ挿入":
                    DatabaseOperation.INSERT.execute(connection, rds);
                    break;
                case "データ削除":
                    DatabaseOperation.DELETE.execute(connection, rds);
                    break;
                case "データ削除・挿入":
                    DatabaseOperation.DELETE.execute(connection, rds);
                    DatabaseOperation.INSERT.execute(connection, rds);
                    break;
                case "カスタマイズSQL":
                    DatabaseOperation.DELETE_ALL.execute(connection, rds);
                    break;
                case "DELETE_ALL":
                  DatabaseOperation.DELETE_ALL.execute(connection, rds);
                    break;
                case "シーケンス設定":
                    resetSeq(tName, tableNo, DBNum);
                    break;
            }
        } catch (Exception e) {
            throw new DatabaseUnitException(e.getMessage(), e);
        }
    }

    /**
     * 操作(DB確認) R
     *
     * @param tableName
     * @param
     * @param tableNo
     * @param guiCamera
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public static void confirmTable(String tableName, String sql, String tableNo,
            RakurakuCaptureUtils guiCamera) throws Exception {
//        String[] notAssertColumns = notAssertColumnsStr.split(",");
    	String[] notAssertColumns = {};

        String DBNum = StringUtils.substringBefore(StringUtils.substringAfter(tableName, "["), "]");
        String tName = StringUtils.substringBefore(tableName, "[");

        IDatabaseConnection connection = getOperateFlatXmlConn(DBNum);
        sql = "SELECT * FROM " + tName.toLowerCase();
        String outxmlName = "■操作(DB確認)　" + tName + ".xml";
        String outXmlPath = guiCamera.getSeqNoPath() + "_rakurakuResult_" + outxmlName;
        QueryDataSet queryDataSet = outputTableDataXml(connection, tName, sql, outXmlPath);
        outputTableDefineXml(connection, tName, DBNum);

        if ("○".equals(tableNo) || "〇".equals(tableNo)) {
            return;
        }

        /* 実際テーブル */
        ITable filtertable1 = DefaultColumnFilter.excludedColumnsTable(queryDataSet.getTable(tName), notAssertColumns);

        /* 予想テーブル */
        String srcFile = RakurakuFileUtils.getEachInputPath() + tableName + "_" + tableNo + ".xml";
        String tempXmlPath = RakurakuCore.eachEviPath + "/Rakuraku_Work/DB情報/" + tableName + "_" + tableNo + ".xml";
        File file = new File(tempXmlPath);
        FileUtils.copyFile(new File(srcFile), file);
        RakurakuFileUtils.replaceVariableFromFile(tempXmlPath);
        IDataSet expectedDataSet = new FlatXmlDataSet(file);
        FileUtils.forceDelete(file);
        ITable expectedTable = expectedDataSet.getTable(tName);
        ITable filtertable2 = DefaultColumnFilter.excludedColumnsTable(expectedTable, notAssertColumns);

        Assert.assertEquals("テープル[" + tName.toUpperCase() + "]のレコード数", filtertable2.getRowCount(),
                filtertable1.getRowCount());
        if (filtertable2.getRowCount() == 0) {
            return;
        }

        Column[] primaryKeys = connection.createDataSet().getTableMetaData(tName).getPrimaryKeys();
        if (primaryKeys == null || primaryKeys.length == 0) {
            primaryKeys = filtertable2.getTableMetaData().getColumns();
        }

        SortedTableExt tableActl = new SortedTableExt(queryDataSet.getTable(tName), primaryKeys);
        SortedTableExt tableExpt = new SortedTableExt(expectedTable, primaryKeys);

        int rowCount = filtertable1.getRowCount();
        Column[] column = filtertable1.getTableMetaData().getColumns();
        for (int irow = 0; irow < rowCount; irow++) {
            for (int icolumn = 0; icolumn < column.length; icolumn++) {
                String columnValActl;
                Object objValActl = tableActl.getValue(irow, column[icolumn].getColumnName());
                if (objValActl != null && objValActl.getClass().getName().equals(BigDecimal.class.getName())) {
                    columnValActl = ((BigDecimal) objValActl).toPlainString();
                } else {
                    columnValActl = String.valueOf(objValActl);
                }
                String columnValExpt = getFieldValue(tableExpt, irow, column[icolumn].getColumnName());
                if (columnValActl == null) {
                    columnValActl = "null";
                }
                if (columnValExpt == null) {
                    columnValExpt = "null";
                }
                String errinfo = "value (table=" + tName + ", row=" + irow + ", col=" + column[icolumn].getColumnName()
                        + "):";
                Assert.assertEquals(errinfo, columnValExpt.replace("\r\n", ""), columnValActl.replace("\r\n", ""));
            }
        }
    }

    private static String getFieldValue(ITable filtertable, int rowNum, String columnName) {
        String val = null;
        try {
            val = "".equals(String.valueOf(filtertable.getValue(rowNum, columnName))) ? ""
                    : String.valueOf(filtertable.getValue(rowNum, columnName));
        } catch (DataSetException e) {
            // val = "";
        } catch (Exception e) {
            // val = "";
        }
        return val;
    }

    /**
     * FlatXML操作用connection取得
     *
     * @param DBNum
     * @return
     * @throws Exception
     */
    private static IDatabaseConnection getOperateFlatXmlConn(String DBNum) throws Exception {
        IDatabaseConnection connection = null;
        if ("ORACLE".equals(getProps().getProperty("DB_TYPE" + DBNum))) {
            connection = new DatabaseConnection(getDbConn(DBNum), COMMON_PROPS.getProperty("DB_SCHEMA" + DBNum));
        } else if ("MYSQL".equals(getProps().getProperty("DB_TYPE" + DBNum))) {
            connection = new MySqlConnection(getDbConn(DBNum), COMMON_PROPS.getProperty("DB_SCHEMA" + DBNum));
        } else if ("SQLSERVER".equals(getProps().getProperty("DB_TYPE" + DBNum))) {
            connection = new MsSqlConnection(getDbConn(DBNum), COMMON_PROPS.getProperty("DB_SCHEMA" + DBNum));
        }else if ("postgresql".equals(getProps().getProperty("DB_TYPE" + DBNum))) {
            connection = new MySqlConnection(getDbConn(DBNum), COMMON_PROPS.getProperty("DB_SCHEMA" + DBNum));
        }
        return connection;
    }

    /**
     * テーブル定義XML出力
     *
     * @param connection
     * @param tName
     * @param DBNum
     * @throws Exception
     */
    private static void outputTableDefineXml(IDatabaseConnection connection, String tName, String DBNum)
            throws Exception {
        String TBLPath = RakurakuCore.eachEviPath + "/Rakuraku_Work/DB情報/";
        File outFile = new File(TBLPath);
        outFile.mkdirs();
        QueryDataSet queryDataSet = new QueryDataSet(connection);
        if ("ORACLE".equals(COMMON_PROPS.get("DB_TYPE"))) {
            queryDataSet.addTable(tName + DBNum + "ColName",
                    "SELECT COLUMN_NAME FIELD FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + tName + "'");
        } else if ("MYSQL".equals(COMMON_PROPS.get("DB_TYPE"))) {
            queryDataSet.addTable(tName + DBNum + "ColName",
                    "select column_name as Field ,column_comment as Comment from information_schema.columns where table_name='"
                            + tName + "' order by ordinal_position");
        } else if ("SQLSERVER".equals(COMMON_PROPS.get("DB_TYPE"))) {
            queryDataSet.addTable(tName + DBNum + "ColName",
                    "select a.name FIELD,b.value COMMENT,c.value TBLNM" + " from syscolumns a"
                            + " left join sys.extended_properties b on a.id = b.major_id and a.colid= b.minor_id"
                            + " left join sys.extended_properties c on a.id = c.major_id and '0'= c.minor_id"
                            + " where a.id = object_id('" + tName + "') order by colorder");
        }else if ("postgresql".equals(COMMON_PROPS.get("DB_TYPE"))) {
            queryDataSet.addTable(tName + DBNum + "ColName",
                    "SELECT a.attname as FIELD,col_description(a.attrelid,a.attnum) as COMMENT FROM pg_attribute as a,pg_class as c WHERE c.relname = '"+ tName.toLowerCase() +"' AND a.attrelid = c.oid And a.attnum> 0");
        }
        outFile = new File(TBLPath + tName + "_TBL.xml");
        FlatXmlDataSet.write(queryDataSet, new FileOutputStream(outFile));
    }

    /**
     * テーブルデータを出力
     *
     * @param connection
     * @param tName
     * @param sql
     * @param outPath
     * @return
     * @throws Exception
     */
    private static QueryDataSet outputTableDataXml(IDatabaseConnection connection, String tName, String sql,
            String outPath) throws Exception {
        connection.getConfig().setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
        connection.getConfig().setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, false);
        QueryDataSet queryDataSet = new QueryDataSet(connection);
        queryDataSet.addTable(tName, sql);
        File outFile = new File(outPath);
        FlatXmlDataSet.write(queryDataSet, new FileOutputStream(outFile));
        return queryDataSet;
    }

    /**
     * Sequenceリセット
     *
     * @param
     */
    private static void resetSeq(String seqName, String resetNo, String DBNum) throws Exception {
        String sql1 = "ALTER SEQUENCE " + seqName + " RESTART WITH " + resetNo;
        String sql2 = "SELECT NEXT VALUE FOR " + seqName;
        Statement stmt = dbConnectionsMap.get(DBNum).createStatement();
        stmt.execute(sql1);
        stmt.execute(sql2);
    }

    /**
     * DB Connection取得
     *
     * @param DBNum
     * @return
     * @throws Exception
     */
    private static Connection getDbConn(String DBNum) throws Exception {
        initDbConn(DBNum);
        return dbConnectionsMap.get(DBNum);
    }

    /**
     * rakurakuプロパティ取得
     *
     * @return
     * @throws Exception
     */
    public static Properties getProps() throws Exception {
        if (COMMON_PROPS == null) {
            COMMON_PROPS = new Properties();
            InputStreamReader in = new InputStreamReader(
                    new FileInputStream("./testtools/confs/rakuraku_common.properties"), "UTF-8");
            COMMON_PROPS.load(in);
        }
        return COMMON_PROPS;
    }

    /**
     * プロパティ設定
     *
     * @param key
     * @param value
     * @return
     * @throws Exception
     */
    public static Properties setProps(String key, String value) throws Exception {
        COMMON_PROPS = getProps();
        COMMON_PROPS.setProperty(key, value);
        return COMMON_PROPS;
    }

    /**
     * DB接続初期化
     *
     * @param DBNum
     * @throws Exception
     */
    private static void initDbConn(String DBNum) throws Exception {
        if (!dbConnectionsMap.containsKey(DBNum)) {
            if (StringUtils.isNotBlank(COMMON_PROPS.getProperty("DB_DRIVERNAME" + DBNum))) {
                Class.forName(COMMON_PROPS.getProperty("DB_DRIVERNAME" + DBNum));
            }
            Connection eachConn = DriverManager.getConnection(
                    COMMON_PROPS.getProperty("DB_CONNECTIONURL" + DBNum)
                            + COMMON_PROPS.getProperty("DB_SUFFIX" + DBNum),
                    COMMON_PROPS.getProperty("DB_USERNAME" + DBNum), COMMON_PROPS.getProperty("DB_PASSWORD" + DBNum));
            dbConnectionsMap.put(DBNum, eachConn);
        }
    }

    /**
     * ツールに関するプログレス閉じる
     */
    public static void killOldProgress() {
        try {
            Process pro = Runtime.getRuntime()
                    .exec("taskkill /im "
                            + RakurakuDBUtils.getProps().getProperty("DRIVER_IE").split(
                                    "/")[RakurakuDBUtils.getProps().getProperty("DRIVER_IE").split("/").length - 1]
                            + " /f");
            pro.waitFor();
            pro = Runtime.getRuntime()
                    .exec("taskkill /im "
                            + RakurakuDBUtils.getProps().getProperty("DRIVER_CHROME").split(
                                    "/")[RakurakuDBUtils.getProps().getProperty("DRIVER_CHROME").split("/").length - 1]
                            + " /f");
            pro.waitFor();
            pro = Runtime.getRuntime().exec("taskkill /fi \"WINDOWTITLE eq DealEvidenceTool.xlsm - Excel\" /f");
            pro.waitFor();
            pro = Runtime.getRuntime()
                    .exec("taskkill /fi \"WINDOWTITLE eq DealEvidenceTool.xlsm  [読み取り専用] - Excel\" /f");
            pro.waitFor();
        } catch (Exception e) {
        }
    }

    /**
     * DB接続テスト用
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        int i = 0;
        String indexString = "";
        getProps();
        while (StringUtils.isNotBlank(COMMON_PROPS.getProperty("DB_TYPE" + indexString))) {
            try {
                initDbConn(indexString);
                System.out.println("DB_TYPE" + indexString + ":" + COMMON_PROPS.getProperty("DB_TYPE" + indexString)
                        + "へ接続成功しました");
            } catch (Exception e) {
                System.out.println("DB_TYPE" + indexString + ":" + COMMON_PROPS.getProperty("DB_TYPE" + indexString)
                        + "へ接続失敗しました");
            }
            i++;
            indexString = String.valueOf(i);
        }
    }

}
