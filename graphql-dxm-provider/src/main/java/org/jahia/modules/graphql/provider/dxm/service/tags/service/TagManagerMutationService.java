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
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.GqlTagMutationResult;
import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.GqlTagWorkspaceMutationResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRObservationManager;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.tags.TaggingService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * GraphQL-facing OSGi service that orchestrates tag mutation operations (rename and delete)
 * for Jahia sites.
 *
 * <p>This service is a thin orchestration layer between the GraphQL mutation resolvers and
 * Jahia's {@link TaggingService}. It is <em>not</em> a replacement for {@code TaggingService};
 * it adds the following concerns specific to the GraphQL API context:
 * <ul>
 *   <li><strong>Dual-workspace propagation</strong> – every mutation is applied to both the
 *       {@code default} (edit) and {@code live} workspaces in sequence, keeping them in sync
 *       without requiring a separate publish step for tag metadata.</li>
 *   <li><strong>Observation suppression</strong> – {@link JCRObservationManager} event
 *       listeners are disabled for the duration of each mutation to prevent spurious
 *       cache-invalidation and re-indexing events during batch processing; they are always
 *       restored in a {@code finally} block.</li>
 *   <li><strong>Partial-failure semantics</strong> – site-wide bulk operations capture
 *       per-node errors and continue processing; callers receive a structured result describing
 *       which nodes succeeded and which failed, without a full transaction rollback.</li>
 *   <li><strong>Authorization</strong> – every operation checks that the current user holds the
 *       {@code tagManager} permission on the target site node before performing any write.</li>
 * </ul>
 *
 * <p><strong>Threading model:</strong> this is a singleton OSGi component. Each public method
 * acquires a JCR session for the calling thread via {@code JCRSessionFactory} and must
 * therefore be invoked within a thread carrying a valid Jahia request context (i.e. a GraphQL
 * request thread). Sessions are not shared or cached between calls.
 *
 * @see TagManagerReadService
 * @see TaggingService
 */
@Component(service = TagManagerMutationService.class, immediate = true)
public class TagManagerMutationService extends TagManagerServiceSupport {
    private static final List<String> WORKSPACES = Arrays.asList(Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE);

    @Reference
    private TaggingService taggingService;

    /**
     * Renames a tag across <em>all</em> nodes in the site that carry it, in both the
     * {@code default} and {@code live} workspaces.
     *
     * <p>Delegates to {@link TaggingService#renameTagUnderPath} for each workspace, which handles
     * node iteration and per-node post-processing via {@link TagManagerActionCallback}. JCR
     * observation listeners are disabled for the entire batch and restored in a {@code finally}
     * block. Individual node failures do not abort the batch — they are captured in the
     * returned result with partial-failure semantics.
     *
     * @param siteKey the Jahia site identifier (e.g. {@code "digitall"}); must not be
     *                {@code null}; authorization is pre-validated by the GraphQL resolver
     * @param tag     the existing tag value to rename; must not be {@code null} or empty
     * @param newName the replacement tag value; must not be {@code null} or blank — a
     *                {@link GqlJcrWrongInputException} is thrown before any JCR access if this
     *                constraint is violated
     * @return a {@link GqlTagMutationResult} carrying the original tag name, a {@code null} node
     *         identifier (bulk mode), and one {@link GqlTagWorkspaceMutationResult} per workspace
     * @throws DataFetchingException     wrapping a {@link RepositoryException} if session
     *                                   acquisition or query execution fails
     * @throws GqlJcrWrongInputException if {@code newName} is blank
     */
    public GqlTagMutationResult renameTag(String siteKey, String tag, String newName) {
        ensureMutationTagName(newName);
        String sitePath = "/sites/" + siteKey;
        try {
            JCRSessionWrapper userSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            Locale locale = userSession.getLocale();
            Locale fallbackLocale = userSession.getFallbackLocale();
            List<GqlTagWorkspaceMutationResult> results = new ArrayList<>();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
                    results.add(taggingService.renameTagUnderPath(sitePath, systemSession, tag, newName, new TagManagerActionCallback(systemSession, workspace)));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return new GqlTagMutationResult(tag, null, results);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Removes a tag from <em>all</em> nodes in the site that carry it, in both the
     * {@code default} and {@code live} workspaces.
     *
     * <p>Delegates to {@link TaggingService#deleteTagUnderPath} for each workspace. Observation
     * listeners are suppressed across both workspace operations with the same guarantees as
     * {@link #renameTag}. Individual node failures are captured with partial-failure semantics.
     *
     * @param siteKey the Jahia site identifier; must not be {@code null}; authorization is
     *                pre-validated by the GraphQL resolver
     * @param tag     the tag value to remove; must not be {@code null} or empty
     * @return a {@link GqlTagMutationResult} carrying the tag name, a {@code null} node
     *         identifier (bulk mode), and one {@link GqlTagWorkspaceMutationResult} per workspace
     * @throws DataFetchingException wrapping a {@link RepositoryException} if session
     *                               acquisition or query execution fails
     */
    public GqlTagMutationResult deleteTag(String siteKey, String tag) {
        String sitePath = "/sites/" + siteKey;
        try {
            JCRSessionWrapper userSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            Locale locale = userSession.getLocale();
            Locale fallbackLocale = userSession.getFallbackLocale();
            List<GqlTagWorkspaceMutationResult> results = new ArrayList<>();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
                    results.add(taggingService.deleteTagUnderPath(sitePath, systemSession, tag, new TagManagerActionCallback(systemSession, workspace)));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return new GqlTagMutationResult(tag, null, results);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Removes a tag from a single identified JCR node in both the {@code default} and
     * {@code live} workspaces.
     *
     * <p>Before performing any write, the method validates that the node identified by
     * {@code nodeId} is a descendant of (or equal to) the specified site node. This guard
     * prevents cross-site tag removal via a crafted node identifier. JCR observation listeners
     * are disabled across both workspace operations and re-enabled in a {@code finally} block.
     *
     * @param siteKey the Jahia site identifier; must not be {@code null}; authorization is
     *                pre-validated by the GraphQL resolver
     * @param tag     the tag value to remove from the node; must not be {@code null} or empty
     * @param nodeId  the JCR UUID of the target node; must not be {@code null}; the node must
     *                belong to {@code /sites/{siteKey}} or a validation exception is thrown
     * @return a {@link GqlTagMutationResult} carrying the tag name, the target {@code nodeId},
     *         and one {@link GqlTagWorkspaceMutationResult} per workspace; on success the
     *         processed count is 1; on failure failedCount is 1 and failedPaths contains the
     *         node path (or nodeId as fallback)
     * @throws DataFetchingException     wrapping a {@link RepositoryException} if session
     *                                   acquisition fails
     * @throws GqlJcrWrongInputException if the resolved node does not belong to the requested site
     */
    public GqlTagMutationResult deleteTagOnNode(String siteKey, String tag, String nodeId) {
        String sitePath = "/sites/" + siteKey;
        try {
            JCRSessionWrapper userSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            validateNodeBelongsToSite(userSession, nodeId, sitePath);
            Locale locale = userSession.getLocale();
            Locale fallbackLocale = userSession.getFallbackLocale();
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

    /**
     * Renames a tag on a single identified JCR node in both the {@code default} and
     * {@code live} workspaces.
     *
     * <p>After validating {@code newName} and confirming that the node belongs to the specified
     * site, the method iterates both workspaces. In each workspace the node is looked up by
     * identifier in a system session, and the rename is only applied if that workspace copy
     * actually carries the tag (inspected via {@code j:tagList}). A missing tag in one workspace
     * is treated as a no-op for that workspace rather than an error. Observation listeners are
     * suppressed across both workspaces and guaranteed to be restored in a {@code finally} block.
     *
     * @param siteKey the Jahia site identifier; must not be {@code null}; authorization is
     *                pre-validated by the GraphQL resolver
     * @param tag     the existing tag value to rename on the node; must not be {@code null} or
     *                empty
     * @param newName the replacement tag value; must not be {@code null} or blank — a
     *                {@link GqlJcrWrongInputException} is thrown before any JCR access if this
     *                constraint is violated
     * @param nodeId  the JCR UUID of the target node; must not be {@code null}; must belong to
     *                {@code /sites/{siteKey}} or a validation exception is thrown
     * @return a {@link GqlTagMutationResult} carrying the original tag name, the target
     *         {@code nodeId}, and one {@link GqlTagWorkspaceMutationResult} per workspace;
     *         processed count per workspace is 1 if the tag was present and renamed, 0 if the
     *         tag was absent in that workspace
     * @throws DataFetchingException     wrapping a {@link RepositoryException} if session
     *                                   acquisition or infrastructure-level operations fail
     * @throws GqlJcrWrongInputException if {@code newName} is blank or the node does not belong
     *                                   to the requested site
     */
    public GqlTagMutationResult renameTagOnNode(String siteKey, String tag, String newName, String nodeId) {
        ensureMutationTagName(newName);
        String sitePath = "/sites/" + siteKey;
        try {
            JCRSessionWrapper userSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            validateNodeBelongsToSite(userSession, nodeId, sitePath);
            Locale locale = userSession.getLocale();
            Locale fallbackLocale = userSession.getFallbackLocale();
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

    private GqlTagWorkspaceMutationResult deleteTagOnNodeInWorkspace(String workspace, Locale locale, Locale fallbackLocale, String tag, String nodeId) throws RepositoryException {
        JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
        JCRNodeWrapper node = null;
        try {
            node = systemSession.getNodeByIdentifier(nodeId);
            taggingService.untag(node, tag);
            systemSession.save();
            flushNodeCaches(node.getPath());
            return new GqlTagWorkspaceMutationResult(workspace, 1, 0, Collections.emptyList());
        } catch (RepositoryException e) {
            String failedPath = node != null ? node.getPath() : nodeId;
            return new GqlTagWorkspaceMutationResult(workspace, 0, 1, Collections.singletonList(failedPath));
        }
    }

    private GqlTagWorkspaceMutationResult renameTagOnNodeInWorkspace(String workspace, Locale locale, Locale fallbackLocale, String tag, String newName, String nodeId) throws RepositoryException {
        JCRSessionWrapper systemSession = getSystemSession(workspace, locale, fallbackLocale);
        JCRNodeWrapper node = null;
        try {
            node = systemSession.getNodeByIdentifier(nodeId);
            if (nodeHasTag(node, tag)) {
                taggingService.renameTag(node, tag, newName);
                systemSession.save();
                flushNodeCaches(node.getPath());
                return new GqlTagWorkspaceMutationResult(workspace, 1, 0, Collections.emptyList());
            }
            return new GqlTagWorkspaceMutationResult(workspace, 0, 0, Collections.emptyList());
        } catch (RepositoryException e) {
            String failedPath = node != null ? node.getPath() : nodeId;
            return new GqlTagWorkspaceMutationResult(workspace, 0, 1, Collections.singletonList(failedPath));
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
}
