/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
