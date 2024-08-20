/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm.acl.service;

import org.jahia.modules.graphql.provider.dxm.user.PrincipalType;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

public class JahiaAclEntry {

    private static Logger logger = LoggerFactory.getLogger(JahiaAclEntry.class);

    public enum AclEntryType { GRANT, DENY, EXTERNAL }

    private final String fromPath;

    private final String sourcePath;

    private String siteKey;

    private final AclEntryType type;

    private final String principalKey;

    private String roleName;

    private String externalPermissionsName = "";


    public JahiaAclEntry(JCRNodeWrapper jcrNode, String principalKey, String[] permission) {
        this.principalKey = principalKey;
        this.sourcePath = jcrNode.getPath();
        this.fromPath = permission[0];
        setSiteKey(jcrNode);
        this.type = AclEntryType.valueOf(permission[1]);

        if (type == AclEntryType.EXTERNAL) {
            String[] roleNames = permission[2].split("/");
            this.roleName = roleNames[0];
            this.externalPermissionsName = roleNames[1];
        } else {
            this.roleName = permission[2];
        }
    }

    private void setSiteKey(JCRNodeWrapper jcrNode) {
        JCRSiteNode site = null;
        try {
            site = jcrNode.getResolveSite();
        } catch (RepositoryException e) {
            logger.debug("unable to fetch site for node {}", jcrNode.getPath());
        }
        siteKey = (site != null) ? site.getSiteKey() : null;
    }

    public boolean isInherited() {
        return !this.sourcePath.equals(fromPath);
    }

    public AclEntryType getType() {
        return type;
    }

    public String getFromPath() {
        return fromPath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public String getPrincipalKey() {
        return principalKey;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getExternalRoleName() {
        String externalRoleName = getRoleName();
        if (isExternalType() && !externalPermissionsName.isEmpty()) {
            externalRoleName += "/" + externalPermissionsName;
        }
        return externalRoleName;
    }

    public String getExternalPermissionsName() {
        return externalPermissionsName;
    }

    public boolean isExternalType() {
        return type == AclEntryType.EXTERNAL;
    }

    public boolean isGrantType() {
        return type == AclEntryType.GRANT;
    }

    public boolean isDenyType() {
        return type == AclEntryType.DENY;
    }

    public boolean isUserPrincipal() {
        return PrincipalType.isUser(principalKey);
    }

    public boolean isGroupPrincipal() {
        return PrincipalType.isGroup(principalKey);
    }

    public String getPrincipalName() {
        return principalKey.substring(2);
    }

}
