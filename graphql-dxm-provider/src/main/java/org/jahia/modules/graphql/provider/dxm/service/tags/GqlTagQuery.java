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
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.tags.TaggingService;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GraphQL root object for Tags related queries
 */
@GraphQLName("JCRTags")
@GraphQLDescription("JCR Queries")
public class GqlTagQuery {

    /**
     * Get GraphQL representations of query for tags suggestions.
     *
     * @param prefix The prefix to filter the tags
     * @param limit Max number of tags to return
     * @param startPath The root node to start the search
     * @param minCount Minimal occurences to return a tag
     * @param offset Offset value
     * @param sortByCount Sort the tags by occurences
     * @return GraphQL representations of tags fetched
     * @throws BaseGqlClientException In case of issues fetching nodes
     */
    @GraphQLField
    @GraphQLName("suggest")
    @GraphQLDescription("Handles suggestion tags queries")
    public List<GqlTagNode> suggestTags(
            @GraphQLName("prefix") @GraphQLNonNull @GraphQLDescription("The prefix of the tags") String prefix,
            @GraphQLName("limit") @GraphQLNonNull @GraphQLDescription("Max number of tags to get") Long limit,
            @GraphQLName("startPath") @GraphQLNonNull @GraphQLDescription("The root node to start the search") String startPath,
            @GraphQLName("minCount") @GraphQLDescription("Minimal occurrences to return a tag") Long minCount,
            @GraphQLName("offset") @GraphQLDescription("Offset value") Long offset,
            @GraphQLName("sortByCount") @GraphQLDescription("Sort the tags by occurrences") Boolean sortByCount
    ) throws RepositoryException {

        try {
            JCRSessionWrapper jcrSessionWrapper = getSession();

            TaggingService taggingService = BundleUtils.getOsgiService(TaggingService.class, null);
            Map<String, Long> suggestions = taggingService.getTagsSuggester().suggest(prefix, startPath, minCount, limit, offset,
                    sortByCount != null ? sortByCount : Boolean.FALSE,
                    jcrSessionWrapper);

            return suggestions
                    .entrySet()
                    .stream()
                    .map(GqlTagNodeImpl::new)
                    .collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(NodeQueryExtensions.Workspace.EDIT.getValue());
    }
}
