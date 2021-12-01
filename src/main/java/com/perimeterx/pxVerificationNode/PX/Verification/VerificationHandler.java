package com.perimeterx.pxVerificationNode.PX.Verification;

import com.perimeterx.pxVerificationNode.PX.PXContext;
import com.perimeterx.pxVerificationNode.PX.PXLogger;
import com.perimeterx.pxVerificationNode.PX.Activities.ActivityHandler;
import com.perimeterx.pxVerificationNode.PX.Exceptions.PXException;
import com.perimeterx.pxVerificationNode.PX.Utils.StringUtils;
import com.perimeterx.pxVerificationNode.pxVerificationNode.Config;

public class VerificationHandler {
    private static final PXLogger logger = PXLogger.getLogger(VerificationHandler.class);

    private Config pxConfiguration;
    private ActivityHandler activityHandler;

    public VerificationHandler(Config pxConfiguration, ActivityHandler activityHandler) {
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
