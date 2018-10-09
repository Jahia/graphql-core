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
 *     Copyright (C) 2002-2018 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

/**
 * Object that defines order used by GqlJcrCriteriaInput
 *
 * @author yousria
 */
public class GqlOrdering {

    public enum OrderType {

        /**
         * Ascendant order
         */
        @GraphQLDescription("Ascendant order")
        ASC,

        /**
         * Descendant order
         */
        @GraphQLDescription("Descendant order")
        DESC
    }

    private String property;
    private OrderType orderType;

    public GqlOrdering(@GraphQLName("property") @GraphQLNonNull @GraphQLDescription("The property to order by") String property,
            @GraphQLName("orderType") @GraphQLNonNull @GraphQLDescription("orderType") OrderType orderType) {
        this.property = property;
        this.orderType = orderType;
    }

    @GraphQLField
    @GraphQLDescription("The property to order by")
    public String getProperty() {
        return property;
    }

    @GraphQLField
    @GraphQLDescription("ASC or DESC order")
    public OrderType getOrderType() {
        return orderType;
    }
}
