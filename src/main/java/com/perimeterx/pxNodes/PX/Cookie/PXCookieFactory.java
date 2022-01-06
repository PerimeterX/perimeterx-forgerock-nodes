package com.perimeterx.pxNodes.PX.Cookie;

import com.perimeterx.pxNodes.pxVerificationNode;

public abstract class PXCookieFactory {
    public static AbstractPXCookie create(pxVerificationNode.Config pxConfiguration, CookieData cookieData) {
        switch (cookieData.getCookieVersion()) {
            case "_px3":
                return new PXCookieV3(pxConfiguration, cookieData);
        }
        return null;
    }
}
