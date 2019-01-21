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

    public static final String LIKE = "like";
    public static final String CONTAINS = "contains";
    public static final String EQUALS = "equals";
    public static final String NOTEQUALS = "notEquals";
    public static final String LT = "lt";
    public static final String GT = "gt";
    public static final String LTE = "lte";
    public static final String GTE = "gte";
    public static final String EXISTS = "exists";
    public static final String LASTDAYS = "lastDays";

    public enum QueryFunction {

        @GraphQLDescription("Query function for lower case comparison")
        LOWER_CASE,

        @GraphQLDescription("Query function for upper case comparison")
        UPPER_CASE,

        @GraphQLDescription("Query function for node name comparison")
        NODE_NAME,

        @GraphQLDescription("Query function for node local name comparison")
        NODE_LOCAL_NAME
    }

    private String like;
    private String contains;
    private String property;
    private QueryFunction function;
    private String equals;
    private String notEquals;
    private String lt;
    private String gt;
    private String lte;
    private String gte;
    private Boolean exists;
    private Integer lastDays;

    /**
     Create an instance of the node constraint.
     *
     * Exactly one parameter that defines the way node property values are compared/matched (such as 'like', 'contains', etc) must be non-null.
     *
     * @param like A value to compare the node property value to, using the 'like' operator
     * @param contains A search expression to match the node property value(s) against: dependent on whether the property parameter is null, either that specific property only or all node properties will be matched
     * @param property The name of the node property to compare/match; should be null when not applicable, may be null when optional, dependent on other parameter values
     * @param function
     * @param equals
     * @param notEquals
     * @param lt
     * @param gt
     * @param lte
     * @param gte
     * @param exists
     */
    public GqlJcrNodeConstraintInput(
        @GraphQLName("like") @GraphQLDescription("A value to compare the node property value to, using the 'like' operator") String like,
        @GraphQLName("contains") @GraphQLDescription("A search expression to match the node property value(s) against, either specific property only or all node properties, dependent on the 'property' parameter value passed") String contains,
        @GraphQLName("property") @GraphQLDescription("The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values") String property,
        @GraphQLName("function") @GraphQLDescription("The query function name for the node for comparison") QueryFunction function,
        @GraphQLName("equals") @GraphQLDescription("A value to compare the node property value to, using the 'equals to' operator") String equals,
        @GraphQLName("notEquals") @GraphQLDescription("A value to compare the node property value to, using the 'not equals to' operator") String notEquals,
        @GraphQLName("lt") @GraphQLDescription("A value to compare the node property value to, using the 'less than' operator") String lt,
        @GraphQLName("gt") @GraphQLDescription("A value to compare the node property value to, using the 'greater than' operator") String gt,
        @GraphQLName("lte") @GraphQLDescription("A value to compare the node property value to, using the 'less than or equals to' operator") String lte,
        @GraphQLName("gte") @GraphQLDescription("A value to compare the node property value to, using the 'greater than or equals to' operator") String gte,
        @GraphQLName("exists") @GraphQLDescription("A value to compare the node property value to, using the 'exists' operator") Boolean exists,
        @GraphQLName("lastDays") @GraphQLDescription("A value to compare the node property value to, using the 'exists' operator") Integer lastDays
    ) {
        this.like = like;
        this.contains = contains;
        this.property = property;
        this.function = function;
        this.equals = equals;
        this.notEquals = notEquals;
        this.lt = lt;
        this.gt = gt;
        this.lte = lte;
        this.gte = gte;
        this.exists = exists;
        this.lastDays = lastDays;
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
    @GraphQLName("property")
    @GraphQLDescription("The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values")
    public String getProperty() {
        return property;
    }

    @GraphQLField
    @GraphQLName("function")
    @GraphQLDescription("The query function name for the node for comparison")
    public QueryFunction getFunction() {
        return function;
    }

    @GraphQLField
    @GraphQLName("equals")
    @GraphQLDescription("A value to compare the node property value to, using the 'equals to' operator")
    public String getEquals() {
        return equals;
    }

    @GraphQLField
    @GraphQLName("notEquals")
    @GraphQLDescription("A value to compare the node property value to, using the 'not equals to' operator")
    public String getNotEquals() {
        return notEquals;
    }

    @GraphQLField
    @GraphQLName("lt")
    @GraphQLDescription("A value to compare the node property value to, using the 'less than' operator")
    public String getLt() {
        return lt;
    }

    @GraphQLField
    @GraphQLName("gt")
    @GraphQLDescription("A value to compare the node property value to, using the 'greater than' operator")
    public String getGt() {
        return gt;
    }

    @GraphQLField
    @GraphQLName("lte")
    @GraphQLDescription("A value to compare the node property value to, using the 'less than or equals to' operator")
    public String getLte() {
        return lte;
    }

    @GraphQLField
    @GraphQLName("gte")
    @GraphQLDescription("A value to compare the node property value to, using the 'greater than or equals to' operator")
    public String getGte() {
        return gte;
    }

    @GraphQLField
    @GraphQLName("exists")
    @GraphQLDescription("A value to compare the node property value to, using the 'exists' operator")
    public Boolean getExists() {
        return exists;
    }

    @GraphQLField
    @GraphQLName("lastDays")
    @GraphQLDescription("A value to pick the last days for node property date value, using the 'lastDays' operator")
    public Integer getLastDays() {
        return lastDays;
    }
}