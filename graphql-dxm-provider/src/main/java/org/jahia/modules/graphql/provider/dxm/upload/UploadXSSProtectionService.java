package org.jahia.modules.graphql.provider.dxm.upload;

import org.jahia.services.content.JCRNodeWrapper;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

import java.util.List;

public interface UploadXSSProtectionService {
    boolean hasPermissionToUploadFile(String fileContent, JCRNodeWrapper node) throws ScanException, PolicyException;
    int priority();
    boolean canHandleContentType(String mimeType);
}
