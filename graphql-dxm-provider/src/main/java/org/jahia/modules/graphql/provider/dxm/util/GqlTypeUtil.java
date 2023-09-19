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
 *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.util;

import graphql.schema.*;

/**
 * GraphQLType utilities
 */
public class GqlTypeUtil {

    public static String getTypeName(GraphQLType type) {
        GraphQLType rawType = GqlTypeUtil.unwrapType(type); // ok to unwrap list?
        return GraphQLTypeUtil.simplePrint(rawType);
    }

    public static GraphQLNamedType unwrapType(GraphQLType type) {
        return (GraphQLNamedType) GraphQLTypeUtil.unwrapType(type).pop();
    }


}
