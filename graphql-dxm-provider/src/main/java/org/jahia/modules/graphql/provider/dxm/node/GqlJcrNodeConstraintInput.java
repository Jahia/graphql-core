/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * An optional part of the JCR node criteria to filter nodes, specifically by their arbitrary properties.
 */
@GraphQLDescription("An optional part of the JCR node criteria to filter nodes, specifically by their arbitrary properties")
public class GqlJcrNodeConstraintInput {

    private String lessThenOrEqualTo;
    private String greaterThenOrEqualTo;
    private String like;
    private String contains;
    private String property;

    /**
     * Create an instance of the node constraint.
     *
     * Exactly one parameter that defines the way node property values are compared/matched (such as 'like', 'contains', etc) must be non-null.
     *
     * @param like A value to compare the node property value to, using the 'like' operator
     * @param contains A search expression to match the node property value(s) against: dependent on whether the property parameter is null, either that specific property only or all node properties will be matched
     * @param property The name of the node property to compare/match; should be null when not applicable, may be null when optional, dependent on other parameter values
     */
    public GqlJcrNodeConstraintInput(
        @GraphQLName("like") @GraphQLDescription("A value to compare the node property value to, using the 'like' operator") String like,
        @GraphQLName("contains") @GraphQLDescription("A search expression to match the node property value(s) against, either specific property only or all node properties, dependent on the 'property' parameter value passed") String contains,
        @GraphQLName("lessThenOrEqualTo") @GraphQLDescription("A value to compare the node property value to, using the 'lessThenOrEqualTo' operator") String lessThenOrEqualTo,
        @GraphQLName("greaterThenOrEqualTo") @GraphQLDescription("A value to compare the node property value to, using the 'greaterThenOrEqualTo' operator") String greaterThenOrEqualTo,
        @GraphQLName("property") @GraphQLDescription("The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values") String property
    ) {
        this.like = like;
        this.contains = contains;
        this.lessThenOrEqualTo = lessThenOrEqualTo;
        this.greaterThenOrEqualTo = greaterThenOrEqualTo;
        this.property = property;
    }

    /**
     * @return A value to compare the node property value to, using the 'like' operator
     */
    @GraphQLField
    @GraphQLName("like")
    @GraphQLDescription("A value to compare the node property value to, using the 'like' operator")
    public String getLike() {
        return like;
    }

    /**
     * @return A search expression to match the node property value(s) against: dependent on whether the property parameter is null, either that specific property only or all node properties will be matched
     */
    @GraphQLField
    @GraphQLName("contains")
    @GraphQLDescription("A search expression to match the node property value(s) against, either specific property only or all node properties, dependent on the 'property' parameter value passed")
    public String getContains() {
        return contains;
    }

    /**
     * @return The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values
     */
    @GraphQLField
    @GraphQLName("lessThenOrEqualTo")
    @GraphQLDescription("A value to compare the node property value to, using the 'lessThenOrEqualTo' operator")
    public String getLessThenOrEqualTo() {
        return lessThenOrEqualTo;
    }

    /**
     * @return The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values
     */
    @GraphQLField
    @GraphQLName("greaterThenOrEqualTo")
    @GraphQLDescription("A value to compare the node property value to, using the 'lessThenOrEqualTo' operator")
    public String getGreaterThenOrEqualTo() {
        return greaterThenOrEqualTo;
    }

    /**
     * @return The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values
     */
    @GraphQLField
    @GraphQLName("property")
    @GraphQLDescription("The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values")
    public String getProperty() {
        return property;
    }

}