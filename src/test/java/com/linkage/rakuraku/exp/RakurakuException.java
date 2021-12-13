package com.linkage.rakuraku.exp;

import org.junit.ComparisonFailure;

import com.linkage.rakuraku.core.RakurakuCore;

public class RakurakuException extends Exception {

    private static final long serialVersionUID = -2508805250225002941L;

    public RakurakuException(String errMsg) {
        super(errMsg);
        RakurakuCore.OKFlag = false;
        RakurakuCore.messageStr = errMsg;
    }

    public RakurakuException(String errMsg, Throwable e) {
        super(errMsg, e);
        RakurakuCore.OKFlag = false;
        RakurakuCore.messageStr = errMsg + "\n" + e.toString();
        if (e instanceof ComparisonFailure) {
            throw new AssertionError(RakurakuCore.messageStr);
        }
    }
}
