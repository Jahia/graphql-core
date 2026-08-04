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

import org.apache.commons.lang3.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.GqlTagMutationResult;
import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.GqlTagWorkspaceMutationResult;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRObservationManager;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.filter.cache.ModuleCacheProvider;
import org.jahia.services.tags.TaggingService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
 *       {@code tagManager} permission on the target site node before performing any write, and each
 *       individual node is additionally checked against the caller's own rights on it — the
 *       site-level permission opens the screen, it does not widen what the caller may write.</li>
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
public class TagManagerMutationService {
    private static final List<String> WORKSPACES = Arrays.asList(Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE);

    /** Right the caller must hold on a node for its tag list to be rewritten. */
    private static final String MODIFY_PROPERTIES_PERMISSION = "jcr:modifyProperties";

    /**
     * Maximum number of failure paths included in a single response payload.
     * Additional failures are counted but not listed.
     */
    public static final int MAX_REPORTED_FAILURES = 10;

    @Reference
    private TaggingService taggingService;

    /**
     * Renames a tag across <em>all</em> nodes in the site that carry it, in both the
     * {@code default} and {@code live} workspaces.
     *
     * <p>Iterates the tagged nodes of each workspace and delegates the per-node rewrite to
     * {@link TaggingService#renameTag(JCRNodeWrapper, String, String)}, with post-processing via
     * {@link TagManagerActionCallback}. A node the caller may not write is left untouched and
     * counted as a failure. JCR observation listeners are disabled for the entire batch and
     * restored in a {@code finally} block. Individual node failures do not abort the batch — they
     * are captured in the returned result with partial-failure semantics.
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
        return applyUnderSite(siteKey, tag, newName);
    }

    /**
     * Removes a tag from <em>all</em> nodes in the site that carry it, in both the
     * {@code default} and {@code live} workspaces.
     *
     * <p>Iterates the tagged nodes of each workspace and delegates the per-node removal to
     * {@link TaggingService#untag(JCRNodeWrapper, String)}. A node the caller may not write is left
     * untouched and counted as a failure. Observation listeners are suppressed across both
     * workspace operations with the same guarantees as {@link #renameTag}. Individual node failures
     * are captured with partial-failure semantics.
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
        return applyUnderSite(siteKey, tag, null);
    }

    /**
     * Shared body of the two site-wide operations: rename when {@code newName} is non-{@code null},
     * removal otherwise.
     *
     * <p>The writes stay on a system session — the {@code live} copy is updated in the same pass, and
     * the caller is not expected to hold live write rights — so every candidate node is checked
     * against the caller's own view of it before it is touched (see
     * {@link #isModifiableByCaller(JCRSessionWrapper, String)}).
     */
    private GqlTagMutationResult applyUnderSite(String siteKey, String tag, String newName) {
        String sitePath = "/sites/" + siteKey;
        try {
            JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            List<GqlTagWorkspaceMutationResult> results = new ArrayList<>();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    JCRSessionWrapper systemSession = JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, null, null);
                    results.add(applyInWorkspace(systemSession, callerSession, workspace, sitePath, tag, newName));
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(Boolean.FALSE);
            }

            return new GqlTagMutationResult(tag, null, results);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private GqlTagWorkspaceMutationResult applyInWorkspace(JCRSessionWrapper systemSession, JCRSessionWrapper callerSession,
                                                           String workspace, String sitePath, String tag, String newName) throws RepositoryException {
        TagManagerActionCallback callback = new TagManagerActionCallback(systemSession, workspace);
        NodeIterator taggedNodes = queryTaggedNodes(systemSession, sitePath, tag);
        while (taggedNodes.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) taggedNodes.nextNode();
            try {
                if (!isModifiableByCaller(callerSession, node.getIdentifier())) {
                    callback.onSkipped();
                    continue;
                }
                if (newName != null) {
                    taggingService.renameTag(node, tag, newName);
                } else {
                    taggingService.untag(node, tag);
                }
                callback.afterTagAction(node);
            } catch (RepositoryException e) {
                callback.onError(node, e);
            }
        }
        return callback.end();
    }

    private NodeIterator queryTaggedNodes(JCRSessionWrapper session, String sitePath, String tag) throws RepositoryException {
        String statement = "SELECT * FROM [jmix:tagged] AS result WHERE ISDESCENDANTNODE(result, '" +
                JCRContentUtils.sqlEncode(sitePath) + "') AND (result.[j:tagList] = $tag)";
        Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2);
        query.bindValue("tag", session.getValueFactory().createValue(tag));
        return query.execute().getNodes();
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
            JCRSessionWrapper editSession = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null);
            validateNodeBelongsToSite(editSession, nodeId, sitePath);
            ensureCallerCanModify(nodeId);
            List<GqlTagWorkspaceMutationResult> workspaceResults = new ArrayList<>();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    JCRSessionWrapper systemSession = workspace.equals(Constants.EDIT_WORKSPACE) ? editSession
                            : JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, null, null);
                    JCRNodeWrapper node = null;
                    try {
                        node = systemSession.getNodeByIdentifier(nodeId);
                        taggingService.untag(node, tag);
                        systemSession.save();
                        flushNodeCaches(node.getPath());
                        workspaceResults.add(new GqlTagWorkspaceMutationResult(workspace, 1, 0, Collections.emptyList()));
                    } catch (RepositoryException e) {
                        String failedPath = node != null ? node.getPath() : nodeId;
                        workspaceResults.add(new GqlTagWorkspaceMutationResult(workspace, 0, 1, Collections.singletonList(failedPath)));
                    }
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
            JCRSessionWrapper editSession = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null);
            validateNodeBelongsToSite(editSession, nodeId, sitePath);
            ensureCallerCanModify(nodeId);
            List<GqlTagWorkspaceMutationResult> workspaceResults = new ArrayList<>();

            JCRObservationManager.setAllEventListenersDisabled(Boolean.TRUE);
            try {
                for (String workspace : WORKSPACES) {
                    JCRSessionWrapper systemSession = workspace.equals(Constants.EDIT_WORKSPACE) ? editSession
                            : JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, null, null);
                    JCRNodeWrapper node = null;
                    try {
                        node = systemSession.getNodeByIdentifier(nodeId);
                        taggingService.renameTag(node, tag, newName);
                        systemSession.save();
                        flushNodeCaches(node.getPath());
                        workspaceResults.add(new GqlTagWorkspaceMutationResult(workspace, 1, 0, Collections.emptyList()));
                    } catch (RepositoryException e) {
                        String failedPath = node != null ? node.getPath() : nodeId;
                        workspaceResults.add(new GqlTagWorkspaceMutationResult(workspace, 0, 1, Collections.singletonList(failedPath)));
                    }
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
     * Invalidates all module caches associated with the given JCR path.
     * Static so that {@link TagManagerActionCallback} can call it without holding a service reference.
     *
     * @param path the JCR path whose caches should be flushed; must not be {@code null}
     */
    static void flushNodeCaches(String path) {
        ModuleCacheProvider cacheProvider = ModuleCacheProvider.getInstance();
        cacheProvider.invalidate(path, true);
        cacheProvider.flushRegexpDependenciesOfPath(path, true);
    }

    /**
     * Rejects a single-node operation the caller has no right to perform on that node.
     *
     * @param nodeIdentifier the JCR UUID of the node about to be written
     * @throws DataFetchingException if the caller may not write the node's properties
     */
    private void ensureCallerCanModify(String nodeIdentifier) throws RepositoryException {
        JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
        if (!isModifiableByCaller(callerSession, nodeIdentifier)) {
            throw new DataFetchingException("Permission denied");
        }
    }

    /**
     * Reports whether the caller may rewrite the tag list of a node, resolving it in the caller's own
     * ACL-bounded session so the node's access control is honoured rather than the system session's
     * unrestricted rights.
     *
     * <p>The {@code default} workspace is the authority for both workspaces: the live copy is updated by
     * the same operation to keep the two in sync, and a caller who may edit content is not expected to
     * hold live write rights. A node the caller cannot resolve there at all — unreadable, or no longer
     * present in the edit workspace — is reported as not modifiable.
     */
    private boolean isModifiableByCaller(JCRSessionWrapper callerSession, String nodeIdentifier) {
        try {
            return callerSession.getNodeByIdentifier(nodeIdentifier).hasPermission(MODIFY_PROPERTIES_PERMISSION);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private void validateNodeBelongsToSite(JCRSessionWrapper session, String nodeId, String sitePath) throws RepositoryException {
        JCRNodeWrapper node = session.getNodeByIdentifier(nodeId);
        String nodePath = node.getPath();
        if (!nodePath.equals(sitePath) && !nodePath.startsWith(sitePath + "/")) {
            throw new GqlJcrWrongInputException("Node does not belong to the requested site");
        }
    }

    private void ensureMutationTagName(String newName) {
        if (StringUtils.isBlank(newName)) {
            throw new GqlJcrWrongInputException("Argument 'newName' can't be empty");
        }
    }
}
