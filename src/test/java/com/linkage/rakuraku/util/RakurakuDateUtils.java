package com.linkage.rakuraku.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.linkage.rakuraku.core.RakurakuCore;

public class RakurakuDateUtils {

    /**
     * 実行開始日付設定
     */
    public static void setRunDate() {
        if (StringUtils.isBlank(RakurakuCore.runDate)) {
            RakurakuCore.runDate = getNowDateOrTime("yyyy-MM-dd");
        }
    }

    /**
     * 現在日時取得
     * 
     * @param format
     * @return
     */
    public static String getNowDateOrTime(String format) {
        return new SimpleDateFormat(format).format(new Date());
    }

}
