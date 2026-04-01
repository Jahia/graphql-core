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

import java.util.List;

@GraphQLName("ManagedTagConnection")
@GraphQLDescription("A paginated list of managed tags")
public class GqlManagedTagConnection {
    private final List<GqlManagedTag> nodes;
    private final GqlManagedTagPageInfo pageInfo;

    public GqlManagedTagConnection(List<GqlManagedTag> nodes, GqlManagedTagPageInfo pageInfo) {
        this.nodes = nodes;
        this.pageInfo = pageInfo;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("The current page of tags")
    public List<GqlManagedTag> getNodes() {
        return nodes;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Pagination metadata")
    public GqlManagedTagPageInfo getPageInfo() {
        return pageInfo;
    }
}
