package com.perimeterx.pxNodes.PX.Verification;

import com.perimeterx.pxNodes.PX.PXContext;
import com.perimeterx.pxNodes.PX.PXLogger;
import com.perimeterx.pxNodes.PX.Activities.ActivityHandler;
import com.perimeterx.pxNodes.PX.Exceptions.PXException;
import com.perimeterx.pxNodes.PX.Utils.StringUtils;
import com.perimeterx.pxNodes.pxVerificationNode;

public class VerificationHandler {
    private static final PXLogger logger = PXLogger.getLogger(VerificationHandler.class);

    private pxVerificationNode.Config pxConfiguration;
    private ActivityHandler activityHandler;

    public VerificationHandler(pxVerificationNode.Config pxConfiguration, ActivityHandler activityHandler) {
        this.pxConfiguration = pxConfiguration;
        this.activityHandler = activityHandler;
    }

    public boolean handleVerification(PXContext ctx) throws PXException {
        boolean verified = isRequestVerified(ctx);
        if (verified) {
            logger.debug("Passing request {} {}", verified, this.pxConfiguration.pxModuleMode());
            this.activityHandler.handlePageRequestedActivity(ctx);
        } else {
            logger.debug("Request invalid");
            this.activityHandler.handleBlockActivity(ctx);
        }

        boolean shouldBypassMonitor = shouldBypassMonitor(ctx);
        if ((pxConfiguration.pxModuleMode() == 1 || shouldBypassMonitor) && !verified) {
            return false;
        }

        return true;
    }

    private boolean shouldBypassMonitor(PXContext ctx) {
        String bypassHeader = this.pxConfiguration.pxBypassMonitorHeader();
        return !StringUtils.isEmpty(bypassHeader) && ctx.getHeaders().containsKey(bypassHeader.toLowerCase())
                && ctx.getHeaders().get(bypassHeader.toLowerCase()).equals("1");
    }

    private boolean isRequestVerified(PXContext ctx) {
        int score = ctx.getRiskScore();
        int blockingScore = this.pxConfiguration.pxBlockingScore();

        boolean verified = score < blockingScore;
        if (verified) {
            logger.debug("Risk score is lower than blocking score. score: {} blockingScore: {}", score, blockingScore);
        }

        return verified;
    }
}
