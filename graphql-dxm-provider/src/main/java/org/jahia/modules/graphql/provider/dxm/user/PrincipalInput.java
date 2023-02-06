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
package org.jahia.modules.graphql.provider.dxm.user;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.services.content.decorator.JCRGroupNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;


@GraphQLDescription("Describes input using name and with principal type (user/group)")
public class PrincipalInput {

    private final PrincipalType principalType;
    private final String principalName;
    private final String principalKey;

    public PrincipalInput(
            @GraphQLName("type") @GraphQLNonNull PrincipalType principalType,
            @GraphQLName("name") @GraphQLNonNull String principalName
    ) {
        this.principalType = principalType;
        this.principalName = principalName;
        this.principalKey = principalType.getPrincipalKey(principalName);
    }

    public PrincipalInput(String principalKey) {
        String[] principals = principalKey.split(":");
        this.principalType = PrincipalType.getByValue(principals[0]);
        this.principalName = principals[1];
        this.principalKey = principalKey;
    }

    public GqlPrincipal getPrincipal(String siteKey, JahiaUserManagerService userService, JahiaGroupManagerService groupService) {
        GqlPrincipal principal = null;
        if (principalType == PrincipalType.USER && userService != null) {
            JCRUserNode userNode = userService.lookupUser(principalName, siteKey);
            principal = (userNode != null) ? new GqlUser(userNode.getJahiaUser()) : null;
        } else if (principalType == PrincipalType.GROUP && groupService != null) {
            JCRGroupNode groupNode = groupService.lookupGroup(siteKey, principalName);
            principal = (groupNode != null) ? new GqlGroup(groupNode.getJahiaGroup()) : null;
        }
        return principal;
    }

    @GraphQLField
    @GraphQLDescription("Get principal type (user/group)")
    public PrincipalType getType() {
        return principalType;
    }

    @GraphQLField
    @GraphQLDescription("Get principal name")
    public String getName() {
        return principalName;
    }

    public String getPrincipalKey() {
        return principalKey;
    }

}
