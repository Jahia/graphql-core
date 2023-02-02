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

@GraphQLDescription("Principal type")
public enum PrincipalType {

    @GraphQLDescription("User principal type")
    USER("u"),

    @GraphQLDescription("Group principal type")
    GROUP("g");

    private final String principalType;


    PrincipalType(String principalType) {
        this.principalType = principalType;
    }

    public String getValue() {
        return principalType;
    }

    public String getPrincipalKey(String principalName) {
        return String.join(":", getValue(), principalName);
    }
}
