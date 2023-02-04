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
package org.jahia.modules.graphql.provider.dxm.acl;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclEntry;
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclRole;
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclService;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.user.GqlPrincipal;
import org.jahia.modules.graphql.provider.dxm.user.PrincipalInput;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

@GraphQLName("GqlAclEntry")
@GraphQLDescription("ACL entry")
public class GqlAclEntry {

    Logger logger = LoggerFactory.getLogger(GqlAclEntry.class);

    @Inject
    @GraphQLOsgiService
    private JahiaAclService aclService;

    private final JahiaAclEntry aclEntry;

    public GqlAclEntry(JahiaAclEntry aclEntry) {
        this.aclEntry = aclEntry;
    }

    @GraphQLField
    @GraphQLDescription("Get principal for this entry")
    public GqlPrincipal getPrincipal() {
        String principalKey = aclEntry.getPrincipalKey();
        PrincipalInput principal = new PrincipalInput(principalKey);
        return principal.getPrincipal(aclEntry.getSitePath());
    }

    @GraphQLField
    @GraphQLDescription("TEMP: Get principal key for this entry")
    public String getPrincipalKey() {
        return aclEntry.getPrincipalKey();
    }

    @GraphQLField
    @GraphQLDescription("Get role for this entry")
    public GqlAclRole getRole() throws RepositoryException {
        String roleName = aclEntry.getRoleName();
        JahiaAclRole aclRole = aclService.getRole(roleName);
        return (aclRole != null) ? new GqlAclRole(aclService.getRole(roleName)): null;
    }

    @GraphQLField
    @GraphQLDescription("TEMP: Get role name for this entry")
    public String getRoleName() throws RepositoryException {
        return aclEntry.getRoleName();
    }

    @GraphQLField
    @GraphQLDescription("TEMP: Get external role name for this entry")
    public String getExternalRoleName() throws RepositoryException {
        return aclEntry.getExternalRoleName();
    }

    @GraphQLField
    @GraphQLDescription("Get node where this ACL entry originated from")
    public GqlJcrNode getInheritedFrom() {
        String fromPath = aclEntry.getFromPath();
        JCRNodeWrapper node = null;
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            node = session.getNode(fromPath);
        } catch (RepositoryException e) {
            logger.error("Unable to fetch node {}", fromPath);
        }
        return (node != null) ? new GqlJcrNodeImpl(node) : null;
    }

    @GraphQLField
    @GraphQLDescription("Type of access for this ACL entry - one of GRANT, DENY or EXTERNAL")
    public String getAclEntryType() {
        return aclEntry.getType().toString();
    }

}
