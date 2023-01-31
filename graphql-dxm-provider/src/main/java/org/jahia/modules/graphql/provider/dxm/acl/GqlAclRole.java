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
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclRole;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("AclRole")
@GraphQLDescription("GraphQL representation of a Jahia ACL role")
public class GqlAclRole {

    private final JahiaAclRole aclRole;

    public GqlAclRole(JahiaAclRole node) {
        this.aclRole = node;
    }

    @GraphQLField
    @GraphQLDescription("Role name")
    public String getName() {
        return this.aclRole.getName();
    }

    @GraphQLField
    @GraphQLDescription("Role group")
    public String getRoleGroup() throws RepositoryException {
        return this.aclRole.getRoleGroup();
    }

    @GraphQLField
    @GraphQLDescription("Role label for a given locale")
    public String getLabel(@GraphQLName("locale") @GraphQLDescription("locale") String locale) {
        return this.aclRole.getLabel(locale);
    }

    @GraphQLField
    @GraphQLDescription("Role description for a given locale")
    public String getDescription(@GraphQLName("locale") @GraphQLDescription("locale") String locale) {
        return this.aclRole.getDescription(locale);
    }

    @GraphQLField
    @GraphQLDescription("List of dependencies for a given role")
    public List<GqlAclRole> getDependencies() throws RepositoryException {
        return this.aclRole.getDependencies().stream()
                .map(GqlAclRole::new)
                .collect(Collectors.toList());
    }
}
