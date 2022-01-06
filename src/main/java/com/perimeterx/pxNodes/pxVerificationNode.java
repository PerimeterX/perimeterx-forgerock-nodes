/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2018 ForgeRock AS.
 */

package com.perimeterx.pxNodes;

import java.util.Set;

import javax.inject.Inject;

import com.perimeterx.pxNodes.PX.PXContext;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;

import com.google.inject.assistedinject.Assisted;
import com.perimeterx.pxNodes.PX.PXLogger;
import com.perimeterx.pxNodes.PX.Activities.ActivityHandler;
import com.perimeterx.pxNodes.PX.Exceptions.PXException;
import com.perimeterx.pxNodes.PX.Template.PXTemplateFactory;
import com.perimeterx.pxNodes.PX.Validators.PXCookieValidator;
import com.perimeterx.pxNodes.PX.Validators.PXS2SValidator;
import com.perimeterx.pxNodes.PX.Verification.VerificationHandler;

/**
 * A node that checks to see if zero-page login headers have specified username
 * and whether that username is in a group
 * permitted to use zero-page login headers.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class, configClass = pxVerificationNode.Config.class)
public class pxVerificationNode extends AbstractDecisionNode {
    private static final PXLogger logger = PXLogger.getLogger(pxVerificationNode.class);
    private final Config config;
    private final Realm realm;
    private PXCookieValidator cookieValidator;
    private PXS2SValidator s2sValidator;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The PerimeterX application id.
         */
        @Attribute(order = 10)
        String pxAppId();

        /**
         * The PerimeterX authentication token.
         */
        @Attribute(order = 11)
        String pxAuthToken();

        /**
         * The PerimeterX cookie secret value.
         */
        @Attribute(order = 12)
        String pxCookieSecret();

        /**
         * A list of user-agents that the module should always allow.
         */
        @Attribute(order = 13)
        Set<String> pxWhitelistUAs();

        /**
         * A list of IP addresses that the module should always allow.
         */
        @Attribute(order = 14)
        Set<String> pxWhitelistIPs();

        /**
         * The score of which PerimeterX module will block the request.
         */
        @Attribute(order = 15)
        default int pxBlockingScore() {
            return 100;
        }

        /**
         * The PerimeterX module mode flag. 0 = monitor, 1 = blocking
         */
        @Attribute(order = 16)
        default int pxModuleMode() {
            return 0;
        }

        /**
         * Connection timeout to PerimeterX servers
         */
        @Attribute(order = 17)
        default int pxConnectionTimeout() {
            return 1000;
        }

        /**
         * API Connection timeout to PerimeterX servers
         */
        @Attribute(order = 18)
        default int pxAPITimeout() {
            return 1000;
        }

        /**
         * A list of sensitive routes for the module.
         */
        @Attribute(order = 19)
        Set<String> pxSensitiveRoutes();

        /**
         * The bypass monitor header name
         */
        @Attribute(order = 20)
        default String pxBypassMonitorHeader() {
            return "";
        }

        @Attribute(order = 21)
        default String pxHardcodedCookie() {
            return "";
        }
    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to
     * obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     * @param realm  The realm the node is in.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public pxVerificationNode(@Assisted Config config, @Assisted Realm realm) throws NodeProcessException {
        this.config = config;
        this.realm = realm;
        this.cookieValidator = new PXCookieValidator(config);
        this.s2sValidator = new PXS2SValidator(config);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        ActivityHandler activityHandler = new ActivityHandler(config);
        VerificationHandler verificationHandler = new VerificationHandler(config, activityHandler);

        String userAgent = context.request.headers.containsKey("User-Agent")
                ? context.request.headers.get("User-Agent").get(0)
                : "";
        String ipAddress = context.request.clientIp;

        logger.debug("Starting request verification");

        if (config.pxWhitelistUAs().contains(userAgent)) {
            logger.debug("Skipping verification for filtered user agent " + userAgent);
            return goTo(true).build();
        }

        if (config.pxWhitelistIPs().contains(ipAddress)) {
            logger.debug("Skipping verification for filtered ip address " + ipAddress);
            return goTo(true).build();
        }

        PXContext ctx = new PXContext(context.request, config, logger);
        logger.debug("Request context created successfully");

        verifyCookie(ctx);

        try {
            boolean verificationResult = verificationHandler.handleVerification(ctx);
            if (!verificationResult) {
                JsonValue stateWithProps = PXTemplateFactory.getProps(context, ctx, config);
                return goTo(false).replaceSharedState(stateWithProps).build();
            }
            return goTo(true).build();

        } catch (PXException e) {
            logger.error("Error in verification result: {}", e);
            return goTo(true).build();
        }
    }

    private void verifyCookie(PXContext ctx) {
        if (cookieValidator.verify(ctx)) {
            return;
        }
        if (!s2sValidator.verify(ctx)) {
            logger.debug("Risk score is higher or equal to blocking score. score: {}, blocking score: {}.",
                    ctx.getRiskScore(), config.pxBlockingScore());
        }
    }
}
