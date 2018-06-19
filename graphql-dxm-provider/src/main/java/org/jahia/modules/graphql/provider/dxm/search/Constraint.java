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
package org.jahia.modules.graphql.provider.dxm.search;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

/**
 * Short description of the class
 *
 * @author yousria
 */
public class Constraint{

    private String property;
    private QueryFunctions function;
    private String equals;
    private String notEquals;
    private String lt;
    private String gt;
    private String lte;
    private String gte;
    private String like;
    private String contains;
    private boolean exists;
    private List<Constraint> all;
    private List<Constraint> any;
    private List<Constraint> none;


    public Constraint(
            @GraphQLName("property") @GraphQLDescription("property constraint") String property,
            @GraphQLName("function") @GraphQLDescription("function (LOWER_CASE, UPPER_CASE, NODE_NAME, NODE_LOCAL_NAME)") QueryFunctions function,
            @GraphQLName("equals") @GraphQLDescription("equals search term") String equals,
            @GraphQLName("notEquals")  @GraphQLDescription("not equals search term") String notEquals,
            @GraphQLName("it") @GraphQLDescription("less than") String lt,
            @GraphQLName("gt") @GraphQLDescription("greater than") String gt,
            @GraphQLName("lte") @GraphQLDescription("less than or equal") String lte,
            @GraphQLName("gte") @GraphQLDescription("greater than or equal ") String gte,
            @GraphQLName("like") @GraphQLDescription("like") String like,
            @GraphQLName("contains") @GraphQLDescription("contains search term") String contains,
            @GraphQLName("exists") @GraphQLDescription("exists or not") boolean exists,
            @GraphQLName("all") List<Constraint> all,
            @GraphQLName("any") List<Constraint> any,
            @GraphQLName("none") List<Constraint> none) {
        this.property = property;
        this.function = function;
        this.equals = equals;
        this.notEquals = notEquals;
        this.lt = lt;
        this.gt = gt;
        this.lte = lte;
        this.gte = gte;
        this.like = like;
        this.contains = contains;
        this.exists = exists;
        this.all = all;
        this.any = any;
        this.none = none;
    }

    @GraphQLField
    public String getProperty() {
        return property;
    }

    @GraphQLField
    public QueryFunctions getFunction() {
        return function;
    }

    @GraphQLField
    public String getEquals() {
        return equals;
    }

    @GraphQLField
    public String getNotEquals() {
        return notEquals;
    }

    @GraphQLField
    public String getLt() {
        return lt;
    }

    @GraphQLField
    public String getGt() {
        return gt;
    }

    @GraphQLField
    public String getLte() {
        return lte;
    }

    @GraphQLField
    public String getGte() {
        return gte;
    }

    @GraphQLField
    public String getLike() {
        return like;
    }

    @GraphQLField
    public String getContains() {
        return contains;
    }

    @GraphQLField
    public boolean isExists() {
        return exists;
    }

    @GraphQLField
    public List<Constraint> getAll() {
        return all;
    }

    @GraphQLField
    public List<Constraint> getAny() {
        return any;
    }

    @GraphQLField
    public List<Constraint> getNone() {
        return none;
    }
}
