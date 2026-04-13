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
package org.jahia.modules.graphql.provider.dxm.service.tags.service;

import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.predicate.SorterHelper;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.GqlManagedTag;
import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.TagManagerSortBy;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.query.ScrollableQuery;
import org.jahia.services.query.ScrollableQueryCallback;
import org.jahia.services.tags.TaggingService;
import org.osgi.service.component.annotations.Component;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component(service = TagManagerReadService.class, immediate = true)
public class TagManagerReadService extends TagManagerServiceSupport {
    private static final int READ_BATCH_SIZE = 500;

    public DXPaginatedData<GqlManagedTag> getTags(String siteKey, TagManagerSortBy sortBy, SorterHelper.SortType sortOrder, DataFetchingEnvironment environment) {
        try {
            JCRSessionWrapper session = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, session);
            List<GqlManagedTag> allTags = loadManagedTags(siteNode, session, sortBy, sortOrder);
            return PaginationHelper.paginate(allTags, tag -> PaginationHelper.encodeCursor(tag.getName()), PaginationHelper.parseArguments(environment));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    public DXPaginatedData<GqlJcrNode> getTaggedContent(String siteKey, String tag, DataFetchingEnvironment environment) {
        try {
            JCRSessionWrapper session = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, session);
            List<GqlJcrNode> nodes = loadTaggedContent(siteNode, tag, session);
            return PaginationHelper.paginate(nodes, gqlNode -> PaginationHelper.encodeCursor(gqlNode.getUuid()), PaginationHelper.parseArguments(environment));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private List<GqlManagedTag> loadManagedTags(JCRNodeWrapper siteNode, JCRSessionWrapper session, TagManagerSortBy sortBy, SorterHelper.SortType sortOrder) throws RepositoryException {
        String query = "SELECT * FROM [jmix:tagged] AS result WHERE ISDESCENDANTNODE(result, '" +
                JCRContentUtils.sqlEncode(siteNode.getPath()) + "') AND (result.[j:tagList] IS NOT NULL)";
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
        ScrollableQuery scrollableQuery = new ScrollableQuery(READ_BATCH_SIZE, jcrQuery);

        Map<String, Long> occurrences = scrollableQuery.execute(new ScrollableQueryCallback<Map<String, Long>>() {
            private final Map<String, Long> result = new HashMap<>();

            @Override
            public boolean scroll() throws RepositoryException {
                NodeIterator nodeIterator = stepResult.getNodes();
                while (nodeIterator.hasNext()) {
                    JCRNodeWrapper nodeWrapper = (JCRNodeWrapper) nodeIterator.nextNode();
                    if (!nodeWrapper.hasProperty(TaggingService.J_TAG_LIST)) {
                        continue;
                    }

                    for (org.jahia.services.content.JCRValueWrapper tagValue : nodeWrapper.getProperty(TaggingService.J_TAG_LIST).getValues()) {
                        String value = tagValue.getString();
                        result.put(value, result.getOrDefault(value, 0L) + 1);
                    }
                }

                return true;
            }

            @Override
            protected Map<String, Long> getResult() {
                return result;
            }
        });

        Comparator<GqlManagedTag> comparator = getSortComparator(sortBy, sortOrder);
        return occurrences.entrySet().stream()
                .map(entry -> new GqlManagedTag(entry.getKey(), entry.getValue()))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private List<GqlJcrNode> loadTaggedContent(JCRNodeWrapper siteNode, String tag, JCRSessionWrapper session) throws RepositoryException {
        List<JCRNodeWrapper> nodes = new ArrayList<>();
        NodeIterator nodeIterator = findTaggedNodes(siteNode.getPath(), tag, session);
        while (nodeIterator.hasNext()) {
            nodes.add((JCRNodeWrapper) nodeIterator.nextNode());
        }

        nodes.sort(Comparator.comparing(JCRNodeWrapper::getPath));

        return nodes.stream()
                .map(node -> {
                    try {
                        return SpecializedTypesHandler.getNode(node);
                    } catch (RepositoryException e) {
                        throw new DataFetchingException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private Comparator<GqlManagedTag> getSortComparator(TagManagerSortBy sortBy, SorterHelper.SortType sortOrder) {
        TagManagerSortBy effectiveSortBy = sortBy != null ? sortBy : TagManagerSortBy.NAME;
        SorterHelper.SortType effectiveSortOrder = sortOrder != null ? sortOrder : SorterHelper.SortType.ASC;

        Comparator<GqlManagedTag> comparator;
        if (effectiveSortBy == TagManagerSortBy.OCCURRENCES) {
            comparator = Comparator.comparing(GqlManagedTag::getOccurrences)
                    .thenComparing(GqlManagedTag::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(GqlManagedTag::getName);
        } else {
            comparator = Comparator.comparing(GqlManagedTag::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(GqlManagedTag::getName);
        }

        if (effectiveSortOrder == SorterHelper.SortType.DESC) {
            comparator = comparator.reversed();
        }

        return comparator;
    }
}
