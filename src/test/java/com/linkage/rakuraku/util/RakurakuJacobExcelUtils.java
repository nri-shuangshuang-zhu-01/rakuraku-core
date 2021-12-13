package com.linkage.rakuraku.util;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.linkage.rakuraku.exp.RakurakuException;

public class RakurakuJacobExcelUtils {

    private ActiveXComponent xl = null; // Excelアプリケーション

    private Dispatch workbooks = null; // ワークブック

    private Dispatch workbook = null; // ワークブック

    private Dispatch sheets = null;// ワークシート

    private Dispatch currentSheet = null;// 活動ワークシート

    public ActiveXComponent getXl() {
        return xl;
    }

    public Dispatch getWorkbooks() {
        return workbooks;
    }

    public Dispatch getWorkbook() {
        return workbook;
    }

    /**
     * 
     * Excelファイルを開く
     * 
     * @param filepath
     * ファイルパス
     * @param visible
     * 表示フラグ
     * @param readonly
     * 編集フラグ
     * 
     */

    public void OpenExcel(String filepath, boolean visible, boolean readonly) {
        try {
            initComponents();
            ComThread.InitSTA();
            // ComThread.InitMTA(true);
            if (xl == null)
                xl = new ActiveXComponent("Excel.Application"); // Excelアプリケーション初期化
            xl.setProperty("Visible", new Variant(visible));// Excelファイル表示するかどうか確定
            if (workbooks == null)
                workbooks = xl.getProperty("Workbooks").toDispatch();
            workbook = Dispatch.invoke(workbooks, "Open", Dispatch.Method,
                    new Object[] { filepath, new Variant(false), new Variant(readonly) }, // Excelファイルを開く
                    new int[1]).toDispatch();
        } catch (Exception e) {
            e.printStackTrace();
            releaseSource();
        }
    }

    /**
     * 
     * Excelファイルを保存する
     * 
     * @param filePath
     * ファイルパス
     * 
     */
    public void SaveAs(String filePath) {
        Dispatch.invoke(workbook, "SaveAs", Dispatch.Method, new Object[] { filePath, new Variant(44) }, new int[1]);
    }

    /**
     * 
     * Excelファイルを閉じる
     * 
     * @param
     */

    public void CloseExcel(boolean f, boolean quitXl) {
        try {
            // Dispatch.call(workbook, "Save");
            Dispatch.call(workbook, "Close", new Variant(f));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (quitXl) {
                releaseSource();
            }
        }
    }

    /**
     * 
     * Excelファイルをリリース
     * 
     */
    public void releaseSource() {
        if (xl != null) {
            xl.invoke("Quit", new Variant[] {});
            xl = null;
        }
        workbooks = null;
        ComThread.Release();
        System.gc();
    }

    /**
     * 
     * Excelファイルにシートを新規追加する
     * 
     */
    public Dispatch addSheet() {
        return Dispatch.get(Dispatch.get(workbook, "sheets").toDispatch(), "add").toDispatch();
    }

    /**
     * 
     * 活動シートの名称を改修する
     * 
     * @param newName
     * 
     */
    public void modifyCurrentSheetName(String newName) {
        Dispatch.put(getCurrentSheet(), "name", newName);
    }

    /**
     * 
     * 活動シートの名称を取得する
     * 
     * @return
     * 
     */
    public String getCurrentSheetName() {
        return Dispatch.get(getCurrentSheet(), "name").toString();
    }

    /**
     * 
     * ワークブックの名称を取得する
     * 
     * @return
     * 
     */
    public String getWorkbookName() {
        if (workbook == null)
            return null;
        return Dispatch.get(workbook, "name").toString();

    }

    /**
     * 
     * すべてのワークシートを取得する
     * 
     * @return
     * 
     */
    public Dispatch getSheets() {
        if (sheets == null)
            sheets = Dispatch.get(workbook, "sheets").toDispatch();
        return sheets;
    }

    /**
     * 
     * 活動シートを取得する
     * 
     * @return
     * 
     */
    public Dispatch getCurrentSheet() {
        currentSheet = Dispatch.get(workbook, "ActiveSheet").toDispatch();
        return currentSheet;
    }

    /**
     * 
     * シート名称によりワークシートを取得する
     * 
     * @param name
     * sheetName
     * @return
     * 
     */
    public Dispatch getSheetByName(String name) {
        return Dispatch.invoke(getSheets(), "Item", Dispatch.Get, new Object[] { name }, new int[1]).toDispatch();
    }

    /**
     * 
     * シートインデックスによりワークシートを取得する
     * 
     * @param index
     * @return sheet・ｽ・ｽ・ｽ・ｽ
     * 
     */

    public Dispatch getSheetByIndex(Integer index) {
        return Dispatch.invoke(getSheets(), "Item", Dispatch.Get, new Object[] { index }, new int[1]).toDispatch();
    }

    /**
     * 
     * シート数を取得する
     * 
     * @return
     * 
     */

    public int getSheetCount() {
        @SuppressWarnings("deprecation")
        int count = Dispatch.get(getSheets(), "count").toInt();
        return count;
    }

    /**
     * 
     * マクロを呼出す
     * 
     * @param macroName
     * マクロ名称
     * 
     */
    public void callMacro(String macroName) {
        Dispatch.call(xl, "Run", new Variant(macroName));
    }

    /**
     * 
     * マクロを呼出す
     * 
     * @param macroName
     * マクロ名称
     * @param param
     * マクロパラメータ
     */
    public void callMacro(String macroName, Object param) throws Exception {
        Variant variant = Dispatch.call(xl, "Run", new Variant(macroName), new Variant(param));
        System.out.println(variant);
        if (!variant.toString().contains("作成完了")) {
            throw new RakurakuException(variant.toString());
        }
    }

    /**
     * 
     * 値を設定する
     * 
     * @param sheet
     * ワークシート
     * @param position
     * 位置
     * @param type
     * バリュータイプ
     * @param value
     * 
     */

    public void setValue(Dispatch sheet, String position, String type, Object value) {
        Dispatch cell = Dispatch.invoke(sheet, "Range", Dispatch.Get, new Object[] { position }, new int[1])
                .toDispatch();
        Dispatch.put(cell, type, value);
    }

    /**
     * 
     * 値を取得する
     * 
     * @param position
     * 位置
     * @param sheet
     * @return
     * 
     */
    public Variant getValue(String position, Dispatch sheet) {
        Dispatch cell = Dispatch.invoke(sheet, "Range", Dispatch.Get, new Object[] { position }, new int[1])
                .toDispatch();
        Variant value = Dispatch.get(cell, "Value");
        return value;

    }

    private void initComponents() {
        workbook = null;
        currentSheet = null;
        sheets = null;
    }

}
