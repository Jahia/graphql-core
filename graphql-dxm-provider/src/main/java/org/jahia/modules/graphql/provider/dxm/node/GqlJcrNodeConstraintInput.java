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

import java.util.List;

/**
 * An optional part of the JCR node criteria to filter nodes, specifically by their arbitrary properties.
 */
@GraphQLDescription("An optional part of the JCR node criteria to filter nodes, specifically by their arbitrary properties")
public class GqlJcrNodeConstraintInput {

    public enum FieldNames {
        LIKE("like"),
        CONTAINS("contains"),
        EQUALS("equals"),
        NOTEQUALS("notEquals"),
        LT("lt"),
        GT("gt"),
        LTE("lte"),
        GTE("gte"),
        EXISTS("exists"),
        LASTDAYS("lastDays"),
        ALL("all"),
        ANY("any"),
        NONE("none");

        private String value;

        public String getValue() {
            return value;
        }

        private FieldNames(String value) {
            this.value = value;
        }
    }

    public enum QueryFunction {

        @GraphQLDescription("Query function for lower case comparison")
        LOWER_CASE(),

        @GraphQLDescription("Query function for upper case comparison")
        UPPER_CASE(),

        @GraphQLDescription("Query function for node name comparison")
        NODE_NAME(),

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
    private List<GqlJcrNodeConstraintInput> all;
    private List<GqlJcrNodeConstraintInput> any;
    private List<GqlJcrNodeConstraintInput> none;

    /**
     * Create an instance of the node constraint.
     *
     * Exactly one parameter that defines the way node property values are compared/matched (such as 'like', 'contains', etc) must be non-null.
     *
     * @param like A value to compare the node property value to, using the 'like' operator
     * @param contains A search expression to match the node property value(s) against: dependent on whether the property parameter is null, either that specific property only or all node properties will be matched
     * @param property The name of the node property to compare/match; should be null when not applicable, may be null when optional, dependent on other parameter values
     * @param function The query function name for the node for comparison
     * @param equals A value to compare the node property value to, using the 'equals to' operator
     * @param notEquals A value to compare the node property value to, using the 'not equals to' operator
     * @param lt A value to compare the node property value to, using the 'less than' operator
     * @param gt A value to compare the node property value to, using the 'greater than' operator
     * @param lte A value to compare the node property value to, using the 'less than or equals to' operator
     * @param gte A value to compare the node property value to, using the 'greater than or equals to' operator
     * @param exists A value to compare the node property value to, using the 'exists' operator
     * @param lastDays A value to compare the node property value to, using the 'exists' operator
     * @param all A list of child constraint input for all composition
     * @param any A list of child constraint input for any composition
     * @param none A list of child constraint input for none composition
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
        @GraphQLName("lastDays") @GraphQLDescription("A value to compare the node property value to, using the 'exists' operator") Integer lastDays,
        @GraphQLName("all") @GraphQLDescription("A list of child constraint input for all composition") List<GqlJcrNodeConstraintInput> all,
        @GraphQLName("any") @GraphQLDescription("A list of child constraint input for any composition") List<GqlJcrNodeConstraintInput> any,
        @GraphQLName("none") @GraphQLDescription("A list of child constraint input for none composition") List<GqlJcrNodeConstraintInput> none
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
        this.all = all;
        this.any = any;
        this.none = none;
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

    @GraphQLField
    @GraphQLName("all")
    @GraphQLDescription("A list of child constraint input for all composition")
    public List<GqlJcrNodeConstraintInput> getAll() {
        return all;
    }

    @GraphQLField
    @GraphQLName("any")
    @GraphQLDescription("A list of child constraint input for any composition")
    public List<GqlJcrNodeConstraintInput> getAny() {
        return any;
    }

    @GraphQLField
    @GraphQLName("none")
    @GraphQLDescription("A list of child constraint input for none composition")
    public List<GqlJcrNodeConstraintInput> getNone() {
        return none;
    }
}