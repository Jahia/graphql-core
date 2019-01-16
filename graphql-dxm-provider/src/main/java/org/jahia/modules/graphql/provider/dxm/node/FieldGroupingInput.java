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
