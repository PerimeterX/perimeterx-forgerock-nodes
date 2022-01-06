package com.perimeterx.pxNodes.PX.Template;

import com.perimeterx.pxNodes.PX.PXConstants;
import com.perimeterx.pxNodes.PX.PXContext;

import com.perimeterx.pxNodes.pxVerificationNode;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeContext;


public class PXTemplateFactory {
    public static JsonValue getProps(TreeContext context, PXContext pxContext, pxVerificationNode.Config config) {
        JsonValue copyState = context.sharedState.copy();
        copyState.put("appId", config.pxAppId());
        copyState.put("refId", pxContext.getUuid());
        copyState.put("vid", pxContext.getVid());
        copyState.put("uuid", pxContext.getUuid());


        String urlVid = pxContext.getVid() != null ? pxContext.getVid() : "";
        
        String blockScript = "//" + PXConstants.CAPTCHA_HOST + "/" + config.pxAppId() + "/captcha.js?a=" + pxContext.getBlockAction().getCode() + "&u=" + pxContext.getUuid() + "&v=" + urlVid + "&m=" + (pxContext.isMobileToken() ? "1" : "0");
        String jsClientSrc = "//" + PXConstants.CLIENT_HOST + "/" + config.pxAppId() + "/main.min.js";
        String hostUrl = String.format(PXConstants.COLLECTOR_URL, config.pxAppId());

        copyState.put("hostUrl", hostUrl);
        copyState.put("blockScript", blockScript);
        copyState.put("jsClientSrc", jsClientSrc);
        copyState.put("firstPartyEnabled", false);
        
        return copyState;
    }
}
