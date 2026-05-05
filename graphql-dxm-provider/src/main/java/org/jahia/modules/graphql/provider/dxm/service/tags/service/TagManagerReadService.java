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
import org.jahia.api.Constants;
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
import org.jahia.services.content.JCRSessionFactory;
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



/**
 * GraphQL-facing OSGi service that provides read (query) operations for tag management.
 *
 * <p>This service is the read-side counterpart of {@link TagManagerMutationService}. It sits
 * between GraphQL query resolvers and Jahia's JCR query infrastructure, adding:
 * <ul>
 *   <li><strong>Tag aggregation</strong> – queries all {@code jmix:tagged} nodes under a site
 *       and aggregates tag values with their occurrence counts using a batched
 *       {@link ScrollableQuery} (batch size: {@value #READ_BATCH_SIZE} nodes) to bound memory
 *       consumption on large repositories.</li>
 *   <li><strong>Flexible sorting</strong> – results can be sorted by tag name or occurrence
 *       count, in ascending or descending order, with stable tie-breaking by name.</li>
 *   <li><strong>Cursor-based pagination</strong> – both public methods return a
 *       {@link DXPaginatedData} wrapper compatible with the GraphQL Relay pagination
 *       specification.</li>
 *   <li><strong>Authorization</strong> – access requires the {@code tagManager} permission on
 *       the target site node, enforced by the shared {@link TagManagerServiceSupport} base
 *       class.</li>
 * </ul>
 *
 * <p><strong>Threading model:</strong> this is a singleton OSGi component. Each public method
 * acquires a JCR session for the calling thread via {@code JCRSessionFactory} and must
 * therefore be invoked within a thread carrying a valid Jahia request context. Sessions are
 * not shared or cached between calls.
 *
 * @see TagManagerMutationService
 * @see TaggingService
 */
@Component(service = TagManagerReadService.class, immediate = true)
public class TagManagerReadService extends TagManagerServiceSupport {
    private static final int READ_BATCH_SIZE = 500;

    /**
     * Returns a paginated list of all tags in use within a site, together with the number of
     * content nodes that carry each tag (occurrence count).
     *
     * <p>Executes a JCR-SQL2 query for all {@code jmix:tagged} descendants of
     * {@code /sites/{siteKey}} that have a non-null {@code j:tagList} property. Results are
     * fetched in batches of {@value #READ_BATCH_SIZE} nodes via a {@link ScrollableQuery} to
     * bound memory usage. Tag values are aggregated into a map, converted to
     * {@link GqlManagedTag} instances, sorted according to the requested criteria, and paginated.
     * Only the {@code default} (edit) workspace is queried.
     *
     * @param siteKey     the Jahia site identifier; must not be {@code null}; the current user
     *                    must hold the {@code tagManager} permission on
     *                    {@code /sites/{siteKey}}
     * @param sortBy      the field to sort results by; if {@code null}, defaults to
     *                    {@link TagManagerSortBy#NAME}
     * @param sortOrder   the sort direction; if {@code null}, defaults to
     *                    {@link SorterHelper.SortType#ASC}
     * @param environment the GraphQL {@link DataFetchingEnvironment} providing Relay pagination
     *                    arguments ({@code first}, {@code after}, {@code last}, {@code before});
     *                    must not be {@code null}
     * @return a {@link DXPaginatedData} of {@link GqlManagedTag} instances, each carrying a tag
     *         name and its occurrence count within the site; empty if no tagged content exists
     * @throws DataFetchingException wrapping a {@link RepositoryException} if session
     *                               acquisition, permission checks, or JCR query execution fails
     */
    public DXPaginatedData<GqlManagedTag> getTags(String siteKey, TagManagerSortBy sortBy, SorterHelper.SortType sortOrder, DataFetchingEnvironment environment) {
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            String sitePath = "/sites/" + siteKey;
            List<GqlManagedTag> allTags = loadManagedTags(sitePath, session, sortBy, sortOrder);
            return PaginationHelper.paginate(allTags, tag -> PaginationHelper.encodeCursor(tag.getName()), PaginationHelper.parseArguments(environment));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Returns a paginated list of all JCR content nodes within a site that are tagged with a
     * specific tag value.
     *
     * <p>Executes a parameterised JCR-SQL2 query ({@code j:tagList = $tag}) using a bound query
     * value to prevent JCR-SQL2 injection; the site path is encoded with
     * {@link JCRContentUtils#sqlEncode(String)}. Matching nodes are sorted by their JCR path
     * for deterministic ordering, then converted to {@link GqlJcrNode} representations via
     * {@link SpecializedTypesHandler} before pagination is applied. Only the {@code default}
     * (edit) workspace is queried.
     *
     * @param siteKey     the Jahia site identifier; must not be {@code null}; the current user
     *                    must hold the {@code tagManager} permission on
     *                    {@code /sites/{siteKey}}
     * @param tag         the exact tag value to match against {@code j:tagList}; must not be
     *                    {@code null} or empty; matching is case-sensitive
     * @param environment the GraphQL {@link DataFetchingEnvironment} providing Relay pagination
     *                    arguments; must not be {@code null}
     * @return a {@link DXPaginatedData} of {@link GqlJcrNode} instances representing the tagged
     *         nodes, ordered by JCR path; empty if no node in the site carries the given tag
     * @throws DataFetchingException wrapping a {@link RepositoryException} if session
     *                               acquisition, permission checks, query execution, or node
     *                               specialization fails
     */
    public DXPaginatedData<GqlJcrNode> getTaggedContent(String siteKey, String tag, DataFetchingEnvironment environment) {
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            String sitePath = "/sites/" + siteKey;
            List<GqlJcrNode> nodes = loadTaggedContent(sitePath, tag, session);
            return PaginationHelper.paginate(nodes, gqlNode -> PaginationHelper.encodeCursor(gqlNode.getUuid()), PaginationHelper.parseArguments(environment));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private List<GqlManagedTag> loadManagedTags(String sitePath, JCRSessionWrapper session, TagManagerSortBy sortBy, SorterHelper.SortType sortOrder) throws RepositoryException {
        String query = "SELECT * FROM [jmix:tagged] AS result WHERE ISDESCENDANTNODE(result, '" +
                JCRContentUtils.sqlEncode(sitePath) + "') AND (result.[j:tagList] IS NOT NULL)";
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

    private List<GqlJcrNode> loadTaggedContent(String sitePath, String tag, JCRSessionWrapper session) throws RepositoryException {
        NodeIterator nodeIterator = findTaggedNodes(sitePath, tag, session);
        List<GqlJcrNode> result = new ArrayList<>();
        while (nodeIterator.hasNext()) {
            result.add(SpecializedTypesHandler.getNode((JCRNodeWrapper) nodeIterator.nextNode()));
        }
        result.sort(Comparator.comparing(gqlNode -> gqlNode.getNode().getPath()));
        return result;
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
