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
package org.jahia.modules.graphql.provider.dxm.service.tags;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

@GraphQLName("ManagedTagPageInfo")
@GraphQLDescription("Pagination metadata for managed tags")
public class GqlManagedTagPageInfo {
    private final int totalCount;
    private final int nodesCount;
    private final boolean hasPreviousPage;
    private final boolean hasNextPage;

    public GqlManagedTagPageInfo(int totalCount, int nodesCount, boolean hasPreviousPage, boolean hasNextPage) {
        this.totalCount = totalCount;
        this.nodesCount = nodesCount;
        this.hasPreviousPage = hasPreviousPage;
        this.hasNextPage = hasNextPage;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Total number of available tags")
    public Integer getTotalCount() {
        return totalCount;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Number of tags in the current page")
    public Integer getNodesCount() {
        return nodesCount;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Whether a previous page exists")
    public Boolean getHasPreviousPage() {
        return hasPreviousPage;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Whether a next page exists")
    public Boolean getHasNextPage() {
        return hasNextPage;
    }
}
