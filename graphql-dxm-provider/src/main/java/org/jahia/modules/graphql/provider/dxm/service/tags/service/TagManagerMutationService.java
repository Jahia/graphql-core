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

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.GqlTagMutationResult;
import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.GqlTagWorkspaceMutationResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRObservationManager;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.filter.cache.ModuleCacheProvider;
import org.jahia.services.tags.TaggingService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component(service = TagManagerMutationService.class, immediate = true)
public class TagManagerMutationService extends TagManagerServiceSupport {
    private static final List<String> WORKSPACES = Arrays.asList(Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE);

    @Reference
    private TaggingService taggingService;

    public GqlTagMutationResult renameTag(String siteKey, String tag, String newName) {
        ensureMutationTagName(newName);
        return new GqlTagMutationResult(tag, null, executeBulkMutation(siteKey, tag,
                (taggingService, node) -> taggingService.renameTag(node, tag, newName)));
    }

    public GqlTagMutationResult deleteTag(String siteKey, String tag) {
        return new GqlTagMutationResult(tag, null, executeBulkMutation(siteKey, tag,
                (taggingService, node) -> taggingService.untag(node, tag)));
    }

    public GqlTagMutationResult deleteTagOnNode(String siteKey, String tag, String nodeId) {
        try {
            JCRSessionWrapper currentUserSession = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, currentUserSession);
            validateNodeBelongsToSite(currentUserSession, nodeId, siteNode.getPath());
            Locale locale = currentUserSession.getLocale();
            Locale fallbackLocale = currentUserSession.getFallbackLocale();
            List<GqlTagWorkspaceMutationResult> workspaceResults = new ArrayList<>();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    workspaceResults.add(deleteTagOnNodeInWorkspace(workspace, locale, fallbackLocale, tag, nodeId));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return new GqlTagMutationResult(tag, nodeId, workspaceResults);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    public GqlTagMutationResult renameTagOnNode(String siteKey, String tag, String newName, String nodeId) {
        ensureMutationTagName(newName);

        try {
            JCRSessionWrapper currentUserSession = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, currentUserSession);
            validateNodeBelongsToSite(currentUserSession, nodeId, siteNode.getPath());
            Locale locale = currentUserSession.getLocale();
            Locale fallbackLocale = currentUserSession.getFallbackLocale();
            List<GqlTagWorkspaceMutationResult> workspaceResults = new ArrayList<>();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    workspaceResults.add(renameTagOnNodeInWorkspace(workspace, locale, fallbackLocale, tag, newName, nodeId));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return new GqlTagMutationResult(tag, nodeId, workspaceResults);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private List<GqlTagWorkspaceMutationResult> executeBulkMutation(String siteKey, String tag, BulkNodeMutation bulkNodeMutation) {
        try {
            JCRSessionWrapper currentUserSession = getCurrentUserEditSession();
            JCRNodeWrapper siteNode = getAuthorizedSiteNode(siteKey, currentUserSession);
            Locale locale = currentUserSession.getLocale();
            Locale fallbackLocale = currentUserSession.getFallbackLocale();
            List<GqlTagWorkspaceMutationResult> results = new ArrayList<>();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
                    TagManagerActionCallback callback = new TagManagerActionCallback(ModuleCacheProvider.getInstance(), systemSession, workspace);
                    results.add(executeBulkMutationInWorkspace(siteNode.getPath(), tag, systemSession, callback, bulkNodeMutation));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return results;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private GqlTagWorkspaceMutationResult executeBulkMutationInWorkspace(String sitePath, String tag, JCRSessionWrapper session,
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

    private GqlTagWorkspaceMutationResult deleteTagOnNodeInWorkspace(String workspace, Locale locale, Locale fallbackLocale, String tag, String nodeId) throws RepositoryException {
        ModuleCacheProvider cacheProvider = ModuleCacheProvider.getInstance();
        JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
        int processedCount = 0;
        List<GqlJcrNode> updatedNodes = new ArrayList<>();
        List<GqlJcrNode> failedNodes = new ArrayList<>();
        JCRNodeWrapper node = null;

        try {
            node = systemSession.getNodeByIdentifier(nodeId);
            taggingService.untag(node, tag);
            systemSession.save();
            flushNodeCaches(cacheProvider, node.getPath());
            processedCount = 1;
            updatedNodes.add(SpecializedTypesHandler.getNode(node));
        } catch (RepositoryException e) {
            addFailedNode(failedNodes, node);
        }

        return new GqlTagWorkspaceMutationResult(workspace, processedCount, updatedNodes, failedNodes);
    }

    private GqlTagWorkspaceMutationResult renameTagOnNodeInWorkspace(String workspace, Locale locale, Locale fallbackLocale, String tag, String newName, String nodeId) throws RepositoryException {
        ModuleCacheProvider cacheProvider = ModuleCacheProvider.getInstance();
        JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
        int processedCount = 0;
        List<GqlJcrNode> updatedNodes = new ArrayList<>();
        List<GqlJcrNode> failedNodes = new ArrayList<>();
        JCRNodeWrapper node = null;

        try {
            node = systemSession.getNodeByIdentifier(nodeId);
            if (nodeHasTag(node, tag)) {
                taggingService.renameTag(node, tag, newName);
                systemSession.save();
                flushNodeCaches(cacheProvider, node.getPath());
                processedCount = 1;
                updatedNodes.add(SpecializedTypesHandler.getNode(node));
            }
        } catch (RepositoryException e) {
            addFailedNode(failedNodes, node);
        }

        return new GqlTagWorkspaceMutationResult(workspace, processedCount, updatedNodes, failedNodes);
    }

    private void addFailedNode(List<GqlJcrNode> failedNodes, JCRNodeWrapper node) throws RepositoryException {
        if (node != null) {
            failedNodes.add(SpecializedTypesHandler.getNode(node));
        }
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

    private void ensureMutationTagName(String newName) {
        if (StringUtils.isBlank(newName)) {
            throw new GqlJcrWrongInputException("Argument 'newName' can't be empty");
        }
    }

    @FunctionalInterface
    private interface BulkNodeMutation {
        void execute(TaggingService taggingService, JCRNodeWrapper node) throws RepositoryException;
    }
}
