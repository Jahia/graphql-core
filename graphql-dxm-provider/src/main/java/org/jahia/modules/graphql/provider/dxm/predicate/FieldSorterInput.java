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
