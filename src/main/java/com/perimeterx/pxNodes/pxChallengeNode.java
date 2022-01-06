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

import java.util.Arrays;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;

import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;

import com.google.inject.assistedinject.Assisted;
import com.perimeterx.pxNodes.PX.PXLogger;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;

/**
 * A node that checks to see if zero-page login headers have specified username
 * and whether that username is in a group
 * permitted to use zero-page login headers.
 */
@Node.Metadata(outcomeProvider = pxChallengeNode.OutcomeProvider.class, configClass = pxChallengeNode.Config.class)
public class pxChallengeNode extends SingleOutcomeNode {

    private static final PXLogger logger = PXLogger.getLogger(pxChallengeNode.class);
    private static final String VALIGN_NEUTRAL_ANCHOR = "HTMLMessageNode_vAlign_Neutral";
    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 10)
        default String pxCssRef() {
            return "";
        }

        @Attribute(order = 20)
        default String pxJsRef() {
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
    public pxChallengeNode(@Assisted Config config, @Assisted Realm realm) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        if (context.hasCallbacks()) {
            logger.debug("Entered hasCallbacks. Done.");
            return goToNext().build();
        }
        logger.debug("About to get ScriptTextOutputCallback 7");
        ScriptTextOutputCallback scriptAndSelfSubmitCallback = new ScriptTextOutputCallback(
                createClientSideScriptExecutorFunction(getScript(context)));
        logger.debug("About to get ScriptTextOutputCallback 8");

        return Action.send(Arrays.asList(scriptAndSelfSubmitCallback, new HiddenValueCallback(VALIGN_NEUTRAL_ANCHOR)))
                .build();
    }

    private String getScript(TreeContext context) {
        String appId = context.sharedState.get("appId").asString();
        String refId = context.sharedState.get("refId").asString();
        String vid = context.sharedState.get("vid").asString();
        String uuid = context.sharedState.get("uuid").asString();
        String cssRef = this.config.pxCssRef();
        String jsRef = this.config.pxJsRef();
        String hostUrl = context.sharedState.get("hostUrl").asString();
        String blockScript = context.sharedState.get("blockScript").asString();
        String jsClientSrc = context.sharedState.get("jsClientSrc").asString();
        boolean firstPartyEnabled = context.sharedState.get("firstPartyEnabled").asBoolean();

        StringBuffer script = new StringBuffer()
                .append("document.head.insertAdjacentHTML(\"beforeend\", \"<style>.px-container{align-items:center;display:flex;flex:1;justify-content:space-between;flex-direction:column;height:100%}.px-container>div{width:100%;display:flex;justify-content:center}.px-container>div>div{display:flex;width:80%}.page-title-wrapper{flex-grow:2}.page-title{flex-direction:column-reverse}.content-wrapper{flex-grow:5}.content{flex-direction:column}.page-footer-wrapper{align-items:center;flex-grow:.2;background-color:#000;color:#c5c5c5;font-size:70%}.content p{margin:14px 0;}</style>\");\n")
                .append("submitted = true;\n")
                .append("\n")
                .append("function createElement(type, classes, attributes, styles) {\n")
                .append("    const elm = document.createElement(type);\n")
                .append("    if (classes && classes.length > 0) {\n")
                .append("        classes.forEach((e) => {\n")
                .append("            elm.classList.add(e);\n")
                .append("        });\n")
                .append("    }\n")
                .append("    if (attributes) {\n")
                .append("        Object.keys(attributes).forEach((k) => {\n")
                .append("            elm.setAttribute(k, attributes[k]);\n")
                .append("        });\n")
                .append("    }\n")
                .append("    if (styles) {\n")
                .append("         Object.keys(styles).forEach((k) => {\n")
                .append("             elm.style[k] = styles[k];\n")
                .append("         });\n")
                .append("    }\n")
                .append("    return elm;\n")
                .append("}\n")
                .append("function insertAfter(el, referenceNode) {\n")
                .append("    referenceNode.parentNode.insertBefore(el, referenceNode.nextSibling);\n")
                .append("}\n")
                .append("\n")
                .append("function callback() {\n")
                .append("    const container = document.createElement(\"div\");\n")
                .append("    container.classList.add(\"px-container\");\n")
                .append("    const pageTitleWrapper = createElement('div', ['page-title-wrapper']);\n")
                .append("    const pageTitle = createElement('div', ['page-title']);\n")
                .append("    const title = createElement('h1');\n")
                .append("    title.innerText = 'Please verify you are a human';\n")
                .append("    pageTitle.appendChild(title);\n")
                .append("    pageTitleWrapper.appendChild(pageTitle);\n")
                .append("    const contentWrapper = createElement('div', ['content-wrapper']);\n")
                .append("    const content = createElement('div', ['content']);\n")
                .append("    const pxCaptcha = createElement('div', null, { id: 'px-captcha' });\n")
                .append("    content.appendChild(pxCaptcha);\n")
                .append("    const p1 = createElement('p');\n")
                .append("    p1.innerText = 'Access to this page has been denied because we believe you are using automation tools to browse the website.';\n")
                .append("    content.appendChild(p1);\n")
                .append("    const p2 = createElement('p');\n")
                .append("    p2.innerText = 'This may happen as a result of the following:';\n")
                .append("    content.appendChild(p2);\n")
                .append("    const ul = createElement('ul');\n")
                .append("    const li1 = createElement('li');\n")
                .append("    li1.innerText = 'Javascript is disabled or blocked by an extension (ad blockers for example)';\n")
                .append("    ul.appendChild(li1);\n")
                .append("    const li2 = createElement('li');\n")
                .append("    li2.innerText = 'Your browser does not support cookies';\n")
                .append("    ul.appendChild(li2);\n")
                .append("    content.appendChild(ul);\n")
                .append("    const p3 = createElement('p');\n")
                .append("    p3.innerText = 'Please make sure that Javascript and cookies are enabled on your browser and that you are not blocking them from loading.';\n")
                .append("    content.appendChild(p3);\n")
                .append("    const p4 = createElement('p');\n")
                .append("    p4.innerText = 'Reference ID: #" + refId + "';\n")
                .append("    content.appendChild(p4);\n")
                .append("    contentWrapper.appendChild(content);\n")
                .append("    const pageFooterWrapper = createElement('div', ['page-footer-wrapper']);\n")
                .append("    const pageFooter = createElement('div', ['page-footer']);\n")
                .append("    const preLink = createElement('span');\n")
                .append("    preLink.innerText = 'Powered by\u00A0';\n")
                .append("    const link = createElement('a', null, { href: 'https://www.perimeterx.com/whywasiblocked' });\n")
                .append("    link.innerText = '\u00A0PerimeterX';\n")
                .append("    const postLink = createElement('span');\n")
                .append("    postLink.innerText = ' , Inc.';\n")

                .append("    pageFooter.appendChild(preLink);\n")
                .append("    pageFooter.appendChild(link);\n")
                .append("    pageFooter.appendChild(postLink);\n")
                .append("    pageFooterWrapper.appendChild(pageFooter);\n")

                .append("    container.appendChild(pageTitleWrapper);\n")
                .append("    container.appendChild(contentWrapper);\n")
                .append("    container.appendChild(pageFooterWrapper);\n")
                .append("    document.querySelector(\".page-header\").remove();\n")
                .append("    document.forms[0].remove();\n")
                .append("    const anchor = document.querySelector(\".container\");\n")
                .append("    anchor.appendChild(container);\n")

                .append(getCssRefValue(cssRef))
                .append(getJSRefValue(jsRef))

                .append("    window._pxAppId='" + appId + "';\n")
                .append("    window._pxJsClientSrc='" + jsClientSrc + "';\n")
                .append("    window._pxFirstPartyEnabled=" + firstPartyEnabled + ";\n")
                .append("    window._pxVid='" + vid + "';\n")
                .append("    window._pxUuid='" + uuid + "';\n")
                .append("    window._pxHostUrl='" + hostUrl + "';\n")
                .append("    const blockScript = document.createElement('script');\n")
                .append("    blockScript.src = '" + blockScript + "';\n")
                .append("    const head = document.getElementsByTagName('head')[0];\n")
                .append("    head.insertBefore(blockScript, null);\n")
                .append("}\n")
                .append("\n")
                .append("if (document.readyState !== 'loading') {\n")
                .append("  callback();\n")
                .append("} else {\n")
                .append("  document.addEventListener(\"DOMContentLoaded\", callback);\n")
                .append("}");

        return script.toString();
    }

    private String getCssRefValue(String cssRef) {
        logger.debug("reached getCssRefValue: {}", cssRef);
        if (cssRef != null && !cssRef.isEmpty()) {
            StringBuffer css = new StringBuffer()
                    .append("const style = document.createElement('link');\n")
                    .append("style.setAttribute('type', 'text/css');\n")
                    .append("style.setAttribute('rel', 'stylesheet');\n")
                    .append("style.setAttribute('href', '" + cssRef + "');\n")
                    .append("document.getElementsByTagName('head')[0].appendChild(style);\n");
            return css.toString();
        }
        return "\n";
    }

    private String getJSRefValue(String jsRef) {
        if (jsRef != null && !jsRef.isEmpty()) {
            StringBuffer js = new StringBuffer()
                    .append("const js = document.createElement('script');\n")
                    .append("style.setAttribute('src', '" + jsRef + "');\n")
                    .append("document.getElementsByTagName('head')[0].appendChild(js);\n");
            return js.toString();
        }
        return "\n";
    }

    public static String createClientSideScriptExecutorFunction(String script) {
        return String.format(
                "(function(output) {\n" +
                        "    var autoSubmitDelay = 0,\n" +
                        "        submitted = false;\n" +
                        "    function submit() {\n" +
                        "        if (submitted) {\n" +
                        "            return;\n" +
                        "        }" +
                        "        document.forms[0].submit();\n" +
                        "        submitted = true;\n" +
                        "    }\n" +
                        "    %s\n" + // script
                        "    setTimeout(submit, autoSubmitDelay);\n" +
                        "}) (document.forms[0].elements['nada']);\n",
                script);
    }

}
