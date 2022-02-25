/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
@GraphQLDescription("Ordering")
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
    @GraphQLName("property")
    @GraphQLDescription("The property to order by")
    public String getProperty() {
        return property;
    }

    @GraphQLField
    @GraphQLName("orderType")
    @GraphQLDescription("ASC or DESC order")
    public OrderType getOrderType() {
        return orderType;
    }
}
