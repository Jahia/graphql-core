package org.jahia.modules.graphql.provider.dxm.upload;

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
            try {
                Policy p = Policy.getInstance(configResourceURL.openStream());
                permissionForConfig.put("jcr:write", p);
            } catch (PolicyException | IOException e) {
                e.printStackTrace();
            }
        }

       logger.info("*** Activated UploadXSSProtectionServiceImpl");
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
}
