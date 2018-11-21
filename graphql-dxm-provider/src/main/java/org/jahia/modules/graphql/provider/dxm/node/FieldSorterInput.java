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
