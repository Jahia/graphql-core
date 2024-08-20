package org.jahia.modules.graphql.provider.dxm.acl.service;/*
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

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.usermanager.JahiaUser;

import javax.jcr.RepositoryException;
import java.util.List;

/**
 * OSGI service interface for ACL query/operations
 */
public interface JahiaAclService {

    /**
     * Get all roles that are not hidden or privileged
     */
    public List<JahiaAclRole> getRoles() throws RepositoryException;

    /**
     * Fetch role for a given role name; return null if it is hidden or privileged
     */
    public JahiaAclRole getRole(String roleName) throws RepositoryException;


    /**
     * Add GRANT permission on roleNames for a given node and principalKey
     * Removes DENY permission or do nothing if node already has inherited role or has ACL inheritance break
     *
     * @param node to grant roleNames permissions
     * @param principalKey one of <code>u:[username]</code> for users, or <code>g:[groupname]</code> for groups
     * @param roleNames role names to add
     * @return true if successful
     * @throws RepositoryException
     */
    public boolean grantRoles(JCRNodeWrapper node, String principalKey, List<String> roleNames) throws RepositoryException;

    /**
     * Remove GRANT permission on roleNames for a given node and principalKey
     * Add DENY permission if node has inherited role or has ACL inheritance break
     *
     * @param node to grant roleNames permissions
     * @param principalKey one of <code>u:[username]</code> for users, or <code>g:[groupname]</code> for groups
     * @param roleNames role names to revoke
     * @return true if successful
     * @throws RepositoryException
     */
    public boolean revokeRoles(JCRNodeWrapper node, String principalKey, List<String> roleNames) throws RepositoryException;

    /**
     * @return true if principalKey has GRANT permission on a roleName for a given node; false otherwise.
     */
    public boolean hasInheritedPermission(JCRNodeWrapper node, String principalKey, String roleName);

    public boolean hasInheritedUserRole(JCRNodeWrapper node, JahiaUser user, String roleName) throws RepositoryException;

    public List<JahiaAclEntry> getAclEntries(JCRNodeWrapper jcrNode);

    public List<JahiaAclEntry> getAclEntries(JCRNodeWrapper jcrNode, String principalKey);
}
