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
package org.jahia.modules.graphql.provider.dxm.service.tags.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.service.tags.service.TagManagerMutationService;
import org.jahia.services.content.JCRSessionFactory;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

@GraphQLName("JahiaTagManagerMutation")
@GraphQLDescription("Tag manager mutations for a site")
public class GqlTagManagerMutation {
    private final String siteKey;

    @Inject
    @GraphQLOsgiService
    private TagManagerMutationService tagManagerMutationService;

    public GqlTagManagerMutation(String siteKey) {
        this.siteKey = siteKey;
        try {
            if (!JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE)
                    .getNode("/sites/" + siteKey).hasPermission("tagManager")) {
                throw new DataFetchingException("Permission denied");
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Rename a tag on all tagged content under the site")
    public GqlTagMutationResult renameTag(@GraphQLName("tag") @GraphQLDescription("The tag to rename") @GraphQLNonNull String tag,
                                          @GraphQLName("newName") @GraphQLDescription("The new tag name") @GraphQLNonNull String newName) {
        return tagManagerMutationService.renameTag(siteKey, tag, newName);
    }

    @GraphQLField
    @GraphQLDescription("Delete a tag from all tagged content under the site")
    public GqlTagMutationResult deleteTag(@GraphQLName("tag") @GraphQLDescription("The tag to delete") @GraphQLNonNull String tag) {
        return tagManagerMutationService.deleteTag(siteKey, tag);
    }

    @GraphQLField
    @GraphQLDescription("Delete a tag from a specific content item")
    public GqlTagMutationResult deleteTagOnNode(@GraphQLName("tag") @GraphQLDescription("The tag to delete") @GraphQLNonNull String tag,
                                                @GraphQLName("nodeId") @GraphQLDescription("The node identifier") @GraphQLNonNull String nodeId) {
        return tagManagerMutationService.deleteTagOnNode(siteKey, tag, nodeId);
    }

    @GraphQLField
    @GraphQLDescription("Rename a tag on a specific content item")
    public GqlTagMutationResult renameTagOnNode(@GraphQLName("tag") @GraphQLDescription("The tag to rename") @GraphQLNonNull String tag,
                                                @GraphQLName("newName") @GraphQLDescription("The new tag name") @GraphQLNonNull String newName,
                                                @GraphQLName("nodeId") @GraphQLDescription("The node identifier") @GraphQLNonNull String nodeId) {
        return tagManagerMutationService.renameTagOnNode(siteKey, tag, newName, nodeId);
    }
}
