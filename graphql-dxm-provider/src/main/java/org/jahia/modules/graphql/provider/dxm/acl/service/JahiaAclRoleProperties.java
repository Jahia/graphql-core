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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

public class JahiaAclRoleProperties {

    private final Logger logger = LoggerFactory.getLogger(JahiaAclRoleProperties.class);

    private final String label;
    private final String description;

    public JahiaAclRoleProperties(JCRNodeWrapper n) {
        this.label = getProperty(n, Constants.JCR_TITLE);
        this.description = getProperty(n, Constants.JCR_DESCRIPTION);
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    private String getProperty(JCRNodeWrapper node, String propName) {
        try {
            if (node.hasProperty(propName)) {
                return node.getPropertyAsString(propName);
            }
        } catch (RepositoryException e) {
            logger.error("Missing property {} for node {}", propName, node);
        }
        return "";
    }

}
