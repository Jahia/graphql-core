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
import org.jahia.modules.graphql.provider.dxm.predicate.GroupingHelper;

import java.util.List;

/**
 * Input which specifies grouping criteria
 *
 * @author akarmanov
 */
@GraphQLDescription("Group entries according to criteria")
public class FieldGroupingInput {

    private String fieldName;
    private List<String> groups;
    private GroupingHelper.GroupingType groupingType;

    public FieldGroupingInput(@GraphQLName("fieldName") @GraphQLNonNull @GraphQLDescription("fieldName whose value is used to check group membership") String fieldName,
                              @GraphQLName("groups") @GraphQLNonNull @GraphQLDescription("available groups") List<String> groups,
                              @GraphQLName("groupingType") @GraphQLDescription("type of grouping") GroupingHelper.GroupingType groupingType) {
        this.fieldName = fieldName;
        this.groups = groups;
        this.groupingType = groupingType;
    }

    @GraphQLField
    @GraphQLDescription("fieldName to group on")
    public String getFieldName() {
        return fieldName;
    }

    @GraphQLField
    @GraphQLDescription("grouping type")
    public GroupingHelper.GroupingType getGroupingType() {
        return groupingType;
    }

    @GraphQLField
    @GraphQLDescription("specified groups")
    public List<String> getGroups() {
        return groups;
    }
}
