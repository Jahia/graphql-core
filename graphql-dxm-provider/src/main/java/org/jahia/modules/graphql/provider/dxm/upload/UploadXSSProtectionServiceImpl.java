package org.jahia.modules.graphql.provider.dxm.upload;

import org.apache.commons.io.FileUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// Ideally this needs to be a service which can aggregate other service and select one according to priority
@Component(service = UploadXSSProtectionService.class, immediate = true)
public class UploadXSSProtectionServiceImpl implements UploadXSSProtectionService {

    private static Logger logger = LoggerFactory.getLogger(UploadXSSProtectionServiceImpl.class);

    private Map<String, Policy> permissionForConfig;
    private AntiSamy as = new AntiSamy();

    @Activate
    public void activate(BundleContext bundleContext) {
        permissionForConfig = new HashMap<>();

        URL configResourceURL = bundleContext.getBundle().getResource("META-INF/antisamy/antisamy.xml");
        if (configResourceURL != null) {
            File tmp = null;
            try {
//                This fails with java.lang.IllegalArgumentException: http://www.w3.org/2001/XMLSchema either as file, direct input stream or path, must be an issue with the file
//                tmp = File.createTempFile("tempconfig", ".xml");
//                FileUtils.copyInputStreamToFile(configResourceURL.openStream(), tmp);
//                Policy p = Policy.getInstance(tmp);
                permissionForConfig.put("jcr:write", Policy.getInstance());
            } catch (PolicyException e) {
                e.printStackTrace();
            } finally {
                FileUtils.deleteQuietly(tmp);
            }
        }

       logger.info(UploadXSSProtectionServiceImpl.class.getName() + " Activated");
    }

    @Override
    public boolean hasPermissionToUploadFile(String fileContent, JCRNodeWrapper node) throws ScanException, PolicyException {

        boolean hasPermission = false;
        for (Map.Entry<String, Policy> es : permissionForConfig.entrySet()) {
            if (node.hasPermission(es.getKey()) && as.scan(fileContent, es.getValue()).getErrorMessages().isEmpty()) {
                hasPermission = true;
                break;
            }
        }

        return hasPermission;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean canHandleContentType(String mime) {
        return Arrays.asList("text/html", "text/xml", "image/svg+xml").contains(mime);
    }

    private Policy getPolicy() throws PolicyException {
        InputStream inputStreamRoute = new ByteArrayInputStream(
                ("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                        "<anti-samy-rules xmlns:xsi=\"http://www.w3.org/2001/XMLSchema\" xsi:noNamespaceSchemaLocation=\"antisamy.xsd\">\n" +
                        "    <directives>\n" +
                        "        <directive name=\"omitXmlDeclaration\" value=\"true\" />\n" +
                        "        <directive name=\"omitDoctypeDeclaration\" value=\"true\" />\n" +
                        "        <directive name=\"maxInputSize\" value=\"5000\" />\n" +
                        "        <directive name=\"formatOutput\" value=\"true\" />\n" +
                        "        <directive name=\"embedStyleSheets\" value=\"false\" />\n" +
                        "        <directive name=\"noopenerAndNoreferrerAnchors\" value=\"true\" />\n" +
                        "    </directives>\n" +
                        "    <common-regexps>\n" +
                        "        <regexp name=\"htmlTitle\" value=\"[\\p{L}\\p{N}\\s\\-_',:\\[\\]!\\./\\\\\\(\\)&amp;]*\" /> <!-- force non-empty with a '+' at the end instead of '*' -->\n" +
                        "        <regexp name=\"onsiteURL\"\n" +
                        "                value=\"^(?!//)(?![\\p{L}\\p{N}\\\\\\.\\#@\\$%\\+&amp;;\\-_~,\\?=/!]*(&amp;colon))[\\p{L}\\p{N}\\\\\\.\\#@\\$%\\+&amp;;\\-_~,\\?=/!]*\" />\n" +
                        "        <regexp name=\"offsiteURL\"\n" +
                        "                value=\"(\\s)*((ht|f)tp(s?)://|mailto:)[\\p{L}\\p{N}]+[~\\p{L}\\p{N}\\p{Zs}\\-_\\.@\\#\\$%&amp;;:,\\?=/\\+!\\(\\)]*(\\s)*\" />\n" +
                        "    </common-regexps>\n" +
                        "    <common-attributes>\n" +
                        "        <attribute name=\"lang\"\n" +
                        "                   description=\"The 'lang' attribute tells the browser what language the element's attribute values and content are written in\">\n" +
                        "            <regexp-list>\n" +
                        "                <regexp value=\"[a-zA-Z0-9-]{2,20}\" />\n" +
                        "            </regexp-list>\n" +
                        "        </attribute>\n" +
                        "        <attribute name=\"title\"\n" +
                        "                   description=\"The 'title' attribute provides text that shows up in a 'tooltip' when a user hovers their mouse over the element\">\n" +
                        "            <regexp-list>\n" +
                        "                <regexp name=\"htmlTitle\" />\n" +
                        "            </regexp-list>\n" +
                        "        </attribute>\n" +
                        "        <attribute name=\"href\" onInvalid=\"filterTag\">\n" +
                        "            <regexp-list>\n" +
                        "                <regexp name=\"onsiteURL\" />\n" +
                        "                <regexp name=\"offsiteURL\" />\n" +
                        "            </regexp-list>\n" +
                        "        </attribute>\n" +
                        "        <attribute name=\"align\"\n" +
                        "                   description=\"The 'align' attribute of an HTML element is a direction word, like 'left', 'right' or 'center'\">\n" +
                        "            <literal-list>\n" +
                        "                <literal value=\"center\" />\n" +
                        "                <literal value=\"left\" />\n" +
                        "                <literal value=\"right\" />\n" +
                        "                <literal value=\"justify\" />\n" +
                        "                <literal value=\"char\" />\n" +
                        "            </literal-list>\n" +
                        "        </attribute>\n" +
                        "    </common-attributes>\n" +
                        "    <global-tag-attributes>\n" +
                        "        <attribute name=\"title\" />\n" +
                        "        <attribute name=\"lang\" />\n" +
                        "    </global-tag-attributes>\n" +
                        "    <tags-to-encode>\n" +
                        "        <tag>g</tag>\n" +
                        "        <tag>grin</tag>\n" +
                        "    </tags-to-encode>\n" +
                        "    <tag-rules>\n" +
                        "        <tag name=\"script\" action=\"remove\" />\n" +
                        "        <tag name=\"noscript\" action=\"remove\" />\n" +
                        "        <tag name=\"iframe\" action=\"remove\" />\n" +
                        "        <tag name=\"frameset\" action=\"remove\" />\n" +
                        "        <tag name=\"frame\" action=\"remove\" />\n" +
                        "        <tag name=\"noframes\" action=\"remove\" />\n" +
                        "        <tag name=\"style\" action=\"remove\" />\n" +
                        "        <tag name=\"p\" action=\"validate\">\n" +
                        "            <attribute name=\"align\" />\n" +
                        "        </tag>\n" +
                        "        <tag name=\"div\" action=\"validate\" />\n" +
                        "        <tag name=\"i\" action=\"validate\" />\n" +
                        "        <tag name=\"b\" action=\"validate\" />\n" +
                        "        <tag name=\"em\" action=\"validate\" />\n" +
                        "        <tag name=\"blockquote\" action=\"validate\" />\n" +
                        "        <tag name=\"tt\" action=\"validate\" />\n" +
                        "        <tag name=\"strong\" action=\"validate\" />\n" +
                        "        <tag name=\"br\" action=\"truncate\" />\n" +
                        "        <tag name=\"quote\" action=\"validate\" />\n" +
                        "        <tag name=\"ecode\" action=\"validate\" />\n" +
                        "        <tag name=\"a\" action=\"validate\">\n" +
                        "            <attribute name=\"href\" onInvalid=\"filterTag\" />\n" +
                        "            <attribute name=\"nohref\">\n" +
                        "                <literal-list>\n" +
                        "                    <literal value=\"nohref\" />\n" +
                        "                    <literal value=\"\" />\n" +
                        "                </literal-list>\n" +
                        "            </attribute>\n" +
                        "            <attribute name=\"rel\">\n" +
                        "                <literal-list>\n" +
                        "                    <literal value=\"nofollow\" />\n" +
                        "                </literal-list>\n" +
                        "            </attribute>\n" +
                        "        </tag>\n" +
                        "        <tag name=\"ul\" action=\"validate\" />\n" +
                        "        <tag name=\"ol\" action=\"validate\" />\n" +
                        "        <tag name=\"li\" action=\"validate\" />\n" +
                        "    </tag-rules>\n" +
                        "    <css-rules>\n" +
                        "    </css-rules>\n" +
                        "    <allowed-empty-tags>\n" +
                        "        <literal-list>\n" +
                        "            <literal value=\"br\" />\n" +
                        "            <literal value=\"hr\" />\n" +
                        "            <literal value=\"a\" />\n" +
                        "            <literal value=\"img\" />\n" +
                        "            <literal value=\"link\" />\n" +
                        "            <literal value=\"iframe\" />\n" +
                        "            <literal value=\"script\" />\n" +
                        "            <literal value=\"object\" />\n" +
                        "            <literal value=\"applet\" />\n" +
                        "            <literal value=\"frame\" />\n" +
                        "            <literal value=\"base\" />\n" +
                        "            <literal value=\"param\" />\n" +
                        "            <literal value=\"meta\" />\n" +
                        "            <literal value=\"input\" />\n" +
                        "            <literal value=\"textarea\" />\n" +
                        "            <literal value=\"embed\" />\n" +
                        "            <literal value=\"basefont\" />\n" +
                        "            <literal value=\"col\" />\n" +
                        "            <literal value=\"div\" />\n" +
                        "        </literal-list>\n" +
                        "    </allowed-empty-tags>\n" +
                        "</anti-samy-rules>").getBytes());
        return Policy.getInstance(inputStreamRoute);
    }
}
