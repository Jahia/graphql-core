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
package org.jahia.modules.graphql.provider.dxm.predicate;

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
    @GraphQLNonNull
    public String getFieldName() {
        return fieldName;
    }

    @GraphQLField
    @GraphQLDescription("grouping type")
    @GraphQLNonNull
    public GroupingHelper.GroupingType getGroupingType() {
        return groupingType;
    }

    @GraphQLField
    @GraphQLDescription("specified groups")
    @GraphQLNonNull
    public List<String> getGroups() {
        return groups;
    }
}
