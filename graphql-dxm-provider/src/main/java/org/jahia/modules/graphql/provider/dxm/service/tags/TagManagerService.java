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

import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRObservationManager;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.query.ScrollableQuery;
import org.jahia.services.query.ScrollableQueryCallback;
import org.jahia.services.render.filter.cache.ModuleCacheProvider;
import org.jahia.services.tags.TaggingService;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TagManagerService {
    private static final List<String> WORKSPACES = Arrays.asList(Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE);
    private static final int READ_BATCH_SIZE = 500;
    private static final TagManagerService INSTANCE = new TagManagerService();

    private TagManagerService() {
    }

    public static TagManagerService getInstance() {
        return INSTANCE;
    }

    public GqlManagedTagConnection getTags(String siteKey, TagManagerSortBy sortBy, TagManagerSortOrder sortOrder, Integer limit, Integer offset) {
        validatePageArgument(limit, "limit");
        validatePageArgument(offset, "offset");

        try {
            JCRSessionWrapper session = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, session);
            List<GqlManagedTag> allTags = loadManagedTags(siteNode, session, sortBy, sortOrder);

            int safeOffset = offset != null ? Math.min(offset, allTags.size()) : 0;
            int end = limit != null ? Math.min(safeOffset + limit, allTags.size()) : allTags.size();
            List<GqlManagedTag> nodes = allTags.subList(safeOffset, end);

            return new GqlManagedTagConnection(
                    nodes,
                    new GqlManagedTagPageInfo(
                            allTags.size(),
                            nodes.size(),
                            safeOffset > 0,
                            end < allTags.size()
                    )
            );
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

    public GqlTagBulkMutationResult renameTag(String siteKey, String tag, String newName) {
        ensureMutationTagName(newName);
        return new GqlTagBulkMutationResult(tag, executeBulkMutation(siteKey, tag,
                (taggingService, node) -> taggingService.renameTag(node, tag, newName)));
    }

    public GqlTagBulkMutationResult deleteTag(String siteKey, String tag) {
        return new GqlTagBulkMutationResult(tag, executeBulkMutation(siteKey, tag,
                (taggingService, node) -> taggingService.untag(node, tag)));
    }

    public GqlTagNodeMutationResult deleteTagOnNode(String siteKey, String tag, String nodeId) {
        try {
            JCRSessionWrapper currentUserSession = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, currentUserSession);
            validateNodeBelongsToSite(currentUserSession, nodeId, siteNode.getPath());
            Locale locale = currentUserSession.getLocale();
            Locale fallbackLocale = currentUserSession.getFallbackLocale();
            List<GqlTagWorkspaceMutationResult> workspaceResults = new ArrayList<>();
            TaggingService taggingService = getTaggingService();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    workspaceResults.add(deleteTagOnNodeInWorkspace(taggingService, workspace, locale, fallbackLocale, tag, nodeId));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return new GqlTagNodeMutationResult(tag, nodeId, workspaceResults);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    public GqlTagNodeMutationResult renameTagOnNode(String siteKey, String tag, String newName, String nodeId) {
        ensureMutationTagName(newName);

        try {
            JCRSessionWrapper currentUserSession = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, currentUserSession);
            validateNodeBelongsToSite(currentUserSession, nodeId, siteNode.getPath());
            Locale locale = currentUserSession.getLocale();
            Locale fallbackLocale = currentUserSession.getFallbackLocale();
            List<GqlTagWorkspaceMutationResult> workspaceResults = new ArrayList<>();
            TaggingService taggingService = getTaggingService();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    workspaceResults.add(renameTagOnNodeInWorkspace(taggingService, workspace, locale, fallbackLocale, tag, newName, nodeId));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return new GqlTagNodeMutationResult(tag, nodeId, workspaceResults);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    static void flushNodeCaches(ModuleCacheProvider moduleCacheProvider, String path) {
        moduleCacheProvider.invalidate(path, true);
        moduleCacheProvider.flushRegexpDependenciesOfPath(path, true);
    }

    private List<GqlManagedTag> loadManagedTags(JCRNodeWrapper siteNode, JCRSessionWrapper session, TagManagerSortBy sortBy, TagManagerSortOrder sortOrder) throws RepositoryException {
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

    private List<GqlTagWorkspaceMutationResult> executeBulkMutation(String siteKey, String tag, BulkNodeMutation bulkNodeMutation) {
        try {
            JCRSessionWrapper currentUserSession = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, currentUserSession);
            Locale locale = currentUserSession.getLocale();
            Locale fallbackLocale = currentUserSession.getFallbackLocale();
            List<GqlTagWorkspaceMutationResult> results = new ArrayList<>();
            TaggingService taggingService = getTaggingService();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
                    TagManagerActionCallback callback = new TagManagerActionCallback(ModuleCacheProvider.getInstance(), systemSession, workspace);
                    results.add(executeBulkMutationInWorkspace(taggingService, siteNode.getPath(), tag, systemSession, callback, bulkNodeMutation));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return results;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private GqlTagWorkspaceMutationResult executeBulkMutationInWorkspace(TaggingService taggingService, String sitePath, String tag, JCRSessionWrapper session,
                                                                         TagManagerActionCallback callback, BulkNodeMutation bulkNodeMutation) throws RepositoryException {
        NodeIterator nodeIterator = findTaggedNodes(sitePath, tag, session);

        while (nodeIterator.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodeIterator.nextNode();
            try {
                bulkNodeMutation.execute(taggingService, node);
                callback.afterTagAction(node);
            } catch (RepositoryException e) {
                callback.onError(node, e);
            }
        }

        return callback.end();
    }

    private GqlTagWorkspaceMutationResult deleteTagOnNodeInWorkspace(TaggingService taggingService, String workspace, Locale locale, Locale fallbackLocale, String tag, String nodeId) throws RepositoryException {
        ModuleCacheProvider cacheProvider = ModuleCacheProvider.getInstance();
        JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
        int processedCount = 0;
        List<GqlTagMutationError> errors = new ArrayList<>();
        JCRNodeWrapper node = null;

        try {
            node = systemSession.getNodeByIdentifier(nodeId);
            taggingService.untag(node, tag);
            systemSession.save();
            flushNodeCaches(cacheProvider, node.getPath());
            processedCount = 1;
        } catch (RepositoryException e) {
            errors.add(buildMutationError(node, nodeId));
        }

        return new GqlTagWorkspaceMutationResult(workspace, processedCount, errors);
    }

    private GqlTagWorkspaceMutationResult renameTagOnNodeInWorkspace(TaggingService taggingService, String workspace, Locale locale, Locale fallbackLocale, String tag, String newName, String nodeId) throws RepositoryException {
        ModuleCacheProvider cacheProvider = ModuleCacheProvider.getInstance();
        JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
        int processedCount = 0;
        List<GqlTagMutationError> errors = new ArrayList<>();
        JCRNodeWrapper node = null;

        try {
            node = systemSession.getNodeByIdentifier(nodeId);
            if (nodeHasTag(node, tag)) {
                taggingService.renameTag(node, tag, newName);
                systemSession.save();
                flushNodeCaches(cacheProvider, node.getPath());
                processedCount = 1;
            }
        } catch (RepositoryException e) {
            errors.add(buildMutationError(node, nodeId));
        }

        return new GqlTagWorkspaceMutationResult(workspace, processedCount, errors);
    }

    private GqlTagMutationError buildMutationError(JCRNodeWrapper node, String fallbackPath) throws RepositoryException {
        if (node == null) {
            return new GqlTagMutationError(fallbackPath, null);
        }

        JCRNodeWrapper pageNode = JCRContentUtils.getParentOfType(node, "jnt:page");
        return new GqlTagMutationError(node.getPath(), pageNode != null ? pageNode.getDisplayableName() : null);
    }

    private boolean nodeHasTag(JCRNodeWrapper node, String tag) throws RepositoryException {
        if (!node.hasProperty(TaggingService.J_TAG_LIST)) {
            return false;
        }

        for (org.jahia.services.content.JCRValueWrapper tagValue : node.getProperty(TaggingService.J_TAG_LIST).getValues()) {
            if (tag.equals(tagValue.getString())) {
                return true;
            }
        }

        return false;
    }

    private NodeIterator findTaggedNodes(String sitePath, String tag, JCRSessionWrapper session) throws RepositoryException {
        String query = "SELECT * FROM [jmix:tagged] AS result WHERE ISDESCENDANTNODE(result, '" +
                JCRContentUtils.sqlEncode(sitePath) + "') AND (result.[j:tagList] = $tag)";
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
        Value tagValue = session.getValueFactory().createValue(tag);
        jcrQuery.bindValue("tag", tagValue);
        return jcrQuery.execute().getNodes();
    }

    private Comparator<GqlManagedTag> getSortComparator(TagManagerSortBy sortBy, TagManagerSortOrder sortOrder) {
        TagManagerSortBy effectiveSortBy = sortBy != null ? sortBy : TagManagerSortBy.NAME;
        TagManagerSortOrder effectiveSortOrder = sortOrder != null ? sortOrder : TagManagerSortOrder.ASC;

        Comparator<GqlManagedTag> comparator;
        if (effectiveSortBy == TagManagerSortBy.OCCURRENCES) {
            comparator = Comparator.comparing(GqlManagedTag::getOccurrences)
                    .thenComparing(GqlManagedTag::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(GqlManagedTag::getName);
        } else {
            comparator = Comparator.comparing(GqlManagedTag::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(GqlManagedTag::getName);
        }

        if (effectiveSortOrder == TagManagerSortOrder.DESC) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    private void validatePageArgument(Integer value, String argumentName) {
        if (value != null && value < 0) {
            throw new GqlJcrWrongInputException("Argument '" + argumentName + "' can't be negative");
        }
    }

    private void ensureMutationTagName(String newName) {
        if (StringUtils.isBlank(newName)) {
            throw new GqlJcrWrongInputException("Argument 'newName' can't be empty");
        }
    }

    private JCRSessionWrapper getCurrentUserEditSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
    }

    private JCRNodeWrapper getAuthorizedSiteNode(String siteKey, JCRSessionWrapper session) throws RepositoryException {
        JCRNodeWrapper siteNode = session.getNode("/sites/" + siteKey);
        if (!siteNode.hasPermission("tagManager")) {
            throw new DataFetchingException("Permission denied");
        }
        return siteNode;
    }

    private void validateNodeBelongsToSite(JCRSessionWrapper session, String nodeId, String sitePath) throws RepositoryException {
        JCRNodeWrapper node = session.getNodeByIdentifier(nodeId);
        String nodePath = node.getPath();
        if (!nodePath.equals(sitePath) && !nodePath.startsWith(sitePath + "/")) {
            throw new GqlJcrWrongInputException("Node does not belong to the requested site");
        }
    }

    private JCRSessionWrapper getSystemSession(String workspace, Locale locale, Locale fallbackLocale) throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, locale, fallbackLocale);
    }

    private TaggingService getTaggingService() {
        return BundleUtils.getOsgiService(TaggingService.class, null);
    }

    @FunctionalInterface
    private interface BulkNodeMutation {
        void execute(TaggingService taggingService, JCRNodeWrapper node) throws RepositoryException;
    }
}
