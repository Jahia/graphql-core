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
import org.jahia.modules.graphql.provider.dxm.predicate.SorterHelper;

/**
 * defines an object with the field we want to sort and the sort direction/type
 *
 * @author yousria
 */
@GraphQLDescription("object with fieldName and sort direction (ASC/DESC)")
public class FieldSorterInput {

    private String fieldName;
    private Boolean ignoreCase;
    private SorterHelper.SortType sortType;

    public FieldSorterInput(@GraphQLName("fieldName") @GraphQLNonNull @GraphQLDescription("fieldName to sort") String fieldName,
            @GraphQLName("sortType") @GraphQLNonNull @GraphQLDescription("type of the sort") SorterHelper.SortType sortType,
            @GraphQLName("ignoreCase") @GraphQLDescription("ignore case when sorting") Boolean ignoreCase) {
        this.fieldName = fieldName;
        this.ignoreCase = ignoreCase;
        this.sortType = sortType;
    }

    @GraphQLField
    @GraphQLName("fieldName")
    @GraphQLDescription("fieldName to sort")
    public String getFieldName() {
        return fieldName;
    }

    @GraphQLField
    @GraphQLName("sortType")
    @GraphQLDescription("direction of the sort")
    public SorterHelper.SortType getSortType() {
        return sortType;
    }

    @GraphQLField
    @GraphQLName("ignoreCase")
    @GraphQLDescription("ignore case when sorting")
    public Boolean isIgnoreCase() {
        return ignoreCase;
    }
}
