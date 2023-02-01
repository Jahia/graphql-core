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

import org.apache.commons.lang3.stream.Streams;
import org.jahia.api.Constants;
import org.jahia.services.content.*;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.stream.Collectors;

import static org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclServiceImpl.*;

public class JahiaAclRole {

    private final JCRNodeWrapper roleNode;
    private final String roleName;
    private final Map<String, JahiaAclRoleProperties> i18nProperties = new TreeMap<>();


    public JahiaAclRole(JCRNodeWrapper node) throws RepositoryException {
        this.roleNode = node;
        String uuid = node.getIdentifier();
        this.roleName = node.getName();
        setI18nProperties(node.getExistingLocales(), uuid);
    }

    private void setI18nProperties(List<Locale> locales, String uuid) throws RepositoryException {
        for (Locale l: locales) {
            JCRNodeWrapper n = getSession(l).getNodeByIdentifier(uuid);
            i18nProperties.put(l.getLanguage(), new JahiaAclRoleProperties(n));
        }
    }

    public String getName() {
        return this.roleName;
    }

    public String getLabel(String locale) {
        JahiaAclRoleProperties i18nProps = i18nProperties.get(locale);
        return (i18nProps != null) ? i18nProps.getLabel() : null;
    }

    public String getRoleGroup() throws RepositoryException {
        return (roleNode.hasProperty(JCR_ROLEGROUP_TYPE)) ?
                roleNode.getPropertyAsString(JCR_ROLEGROUP_TYPE) : null;
    }


    public String getDescription(String locale) {
        JahiaAclRoleProperties i18nProps = i18nProperties.get(locale);
        return (i18nProps != null) ? i18nProps.getDescription() : null;
    }

    public List<JahiaAclRole> getDependencies() throws RepositoryException {
        List<JahiaAclRole> result = new ArrayList<>();
        if (roleNode.hasProperty(JCR_ROLE_DEPENDENCIES_TYPE)) {
            JCRValueWrapper[] dependencies = roleNode.getProperty(JCR_ROLE_DEPENDENCIES_TYPE).getValues();
            for (JCRValueWrapper d: dependencies) {
                result.add(new JahiaAclRole(d.getNode()));
            }
        }
        return result;
    }

    private JCRSessionWrapper getSession(Locale locale) throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE, locale);
    }

}
