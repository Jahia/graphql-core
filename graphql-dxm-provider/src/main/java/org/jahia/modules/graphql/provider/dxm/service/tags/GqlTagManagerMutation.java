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

@GraphQLName("JahiaTagManagerMutation")
@GraphQLDescription("Tag manager mutations for a site")
public class GqlTagManagerMutation {
    private final String siteKey;

    public GqlTagManagerMutation(String siteKey) {
        this.siteKey = siteKey;
    }

    @GraphQLField
    @GraphQLDescription("Rename a tag on all tagged content under the site")
    public GqlTagBulkMutationResult renameTag(@GraphQLName("tag") @GraphQLDescription("The tag to rename") @GraphQLNonNull String tag,
                                              @GraphQLName("newName") @GraphQLDescription("The new tag name") @GraphQLNonNull String newName) {
        return TagManagerService.getInstance().renameTag(siteKey, tag, newName);
    }

    @GraphQLField
    @GraphQLDescription("Delete a tag from all tagged content under the site")
    public GqlTagBulkMutationResult deleteTag(@GraphQLName("tag") @GraphQLDescription("The tag to delete") @GraphQLNonNull String tag) {
        return TagManagerService.getInstance().deleteTag(siteKey, tag);
    }

    @GraphQLField
    @GraphQLDescription("Delete a tag from a specific content item")
    public GqlTagNodeMutationResult deleteTagOnNode(@GraphQLName("tag") @GraphQLDescription("The tag to delete") @GraphQLNonNull String tag,
                                                    @GraphQLName("nodeId") @GraphQLDescription("The node identifier") @GraphQLNonNull String nodeId) {
        return TagManagerService.getInstance().deleteTagOnNode(siteKey, tag, nodeId);
    }

    @GraphQLField
    @GraphQLDescription("Rename a tag on a specific content item")
    public GqlTagNodeMutationResult renameTagOnNode(@GraphQLName("tag") @GraphQLDescription("The tag to rename") @GraphQLNonNull String tag,
                                                    @GraphQLName("newName") @GraphQLDescription("The new tag name") @GraphQLNonNull String newName,
                                                    @GraphQLName("nodeId") @GraphQLDescription("The node identifier") @GraphQLNonNull String nodeId) {
        return TagManagerService.getInstance().renameTagOnNode(siteKey, tag, newName, nodeId);
    }
}
