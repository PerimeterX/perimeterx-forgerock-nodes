package com.perimeterx.pxVerificationNode.PX.ActivitiesData;

import com.perimeterx.pxVerificationNode.PX.PXConstants;

public enum BlockAction {
    BLOCK(PXConstants.BLOCK_ACTION_CAPTCHA), CAPTCHA(PXConstants.CAPTCHA_ACTION_CAPTCHA),
    RATE(PXConstants.BLOCK_ACTION_RATE);

    private final String code;

    BlockAction(String code) {
        this.code = code;
    }

    public final String getCode() {
        return code;
    }

}