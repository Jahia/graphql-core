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
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;

@GraphQLName("JahiaTagManager")
@GraphQLDescription("Tag manager queries for a site")
public class GqlTagManagerQuery {
    private final String siteKey;

    public GqlTagManagerQuery(String siteKey) {
        this.siteKey = siteKey;
    }

    @GraphQLField
    @GraphQLDescription("List tags used under the site")
    public GqlManagedTagConnection tags(@GraphQLName("sortBy") @GraphQLDescription("The field to sort by") TagManagerSortBy sortBy,
                                        @GraphQLName("sortOrder") @GraphQLDescription("The sort order") TagManagerSortOrder sortOrder,
                                        @GraphQLName("limit") @GraphQLDescription("Max number of tags to return") Integer limit,
                                        @GraphQLName("offset") @GraphQLDescription("Offset within the tag list") Integer offset) {
        return TagManagerService.getInstance().getTags(siteKey, sortBy, sortOrder, limit, offset);
    }

    @GraphQLField
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("List content items using a specific tag")
    public DXPaginatedData<GqlJcrNode> taggedContent(@GraphQLName("tag") @GraphQLDescription("The tag to inspect") @GraphQLNonNull String tag,
                                                     DataFetchingEnvironment environment) {
        return TagManagerService.getInstance().getTaggedContent(siteKey, tag, environment);
    }
}
