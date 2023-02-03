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

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = JahiaAclService.class, immediate = true)
public class JahiaAclServiceImpl implements JahiaAclService {

    @Reference
    private JahiaUserManagerService userService;

    @Reference
    private JahiaGroupManagerService groupService;

    public static final String JCR_ROLE_TYPE = "jnt:role";
    public static final String JCR_ROLEGROUP_TYPE = "j:roleGroup";
    public static final String JCR_ROLE_DEPENDENCIES_TYPE = "j:dependencies";

    public static final String REMOVE = "REMOVE";


    public List<JahiaAclRole> getRoles() throws RepositoryException {
        List<JahiaAclRole> roles = new ArrayList<>();
        NodeIterator ni = execQuery("select * from [jnt:role] as role");
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            if (!next.getName().equals("privileged")) {
                roles.add(new JahiaAclRole(next));
            }
        }
        return roles;
    }

    public boolean grantRoles(JCRNodeWrapper jcrNode, String principalKey, List<String> roleNames) throws RepositoryException {
        if (!isValidPrincipal(jcrNode, principalKey)) {
            throw new ItemNotFoundException("Invalid user");
        }
        Map<String, String> roles = new HashMap<>(roleNames.size());
        boolean breakInheritance = jcrNode.getAclInheritanceBreak();
        for (String r: roleNames) {
            roles.put(r, (breakInheritance || !hasInheritedPermission(jcrNode, principalKey, r)) ? Constants.GRANT : REMOVE);
        }
        return jcrNode.changeRoles(principalKey, roles);
    }

    public boolean revokeRoles(JCRNodeWrapper jcrNode, String principalKey, List<String> roleNames) throws RepositoryException {
        Map<String, String> roles = new HashMap<>(roleNames.size());
        boolean breakInheritance = jcrNode.getAclInheritanceBreak();
        for (String r: roleNames) {
            roles.put(r, (breakInheritance || hasInheritedPermission(jcrNode, principalKey, r)) ? Constants.DENY : REMOVE);
        }
        return jcrNode.changeRoles(principalKey, roles);
    }

    private boolean isValidPrincipal(JCRNodeWrapper jcrNode, String principalKey) throws RepositoryException {
        String siteKey = null;
        JCRSiteNode site = jcrNode.getResolveSite();
        if (site != null) {
            siteKey = site.getSiteKey();
        }

        String[] principalItems = principalKey.split(":");
        boolean isValid = false;
        if ("u".equals(principalItems[0])) {
            isValid = userService.lookupUser(principalItems[1], siteKey) != null;
        } else if ("g".equals(principalItems[0])) {
            isValid = groupService.lookupGroup(siteKey, principalItems[1]) != null;
        }
        return isValid;
    }

    public boolean hasInheritedPermission(JCRNodeWrapper jcrNode, String principalKey, String roleName) {
        Map<String, List<String[]>> acl = jcrNode.getAclEntries();
        if (acl == null) {
            return false;
        }

        List<String[]> permissions = acl.get(principalKey);
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        for (String[] p: permissions) {
            String fromAclPath = p[0];
            String type = p[1];
            String pRole = p[2];
            if (!jcrNode.getPath().equals(fromAclPath)
                    && roleName.equals(pRole)
                    && Constants.GRANT.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private NodeIterator execQuery(String query) throws RepositoryException {
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
        QueryManager qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery(query, Query.JCR_SQL2);
        return q.execute().getNodes();
    }
}
