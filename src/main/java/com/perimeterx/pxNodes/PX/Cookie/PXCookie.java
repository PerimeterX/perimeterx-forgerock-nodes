package com.perimeterx.pxNodes.PX.Cookie;

import com.fasterxml.jackson.databind.JsonNode;
import com.perimeterx.pxNodes.PX.Exceptions.PXException;

public interface PXCookie {
    String getHmac();

    String getUUID();

    String getVID();

    String getBlockAction();

    long getTimestamp();

    int getScore();

    boolean isCookieFormatValid(JsonNode decodedCookie);

    boolean isSecured() throws PXException;

}