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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.*;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLFieldCompleter;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.GqlLimitExceededException;
import org.jahia.modules.graphql.provider.dxm.config.GraphQLLimits;
import org.jahia.services.content.*;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.importexport.DocumentViewImportHandler;
import org.jahia.services.importexport.ReferencesHelper;
import org.jahia.services.query.QueryWrapper;
import org.jahia.settings.SettingsBean;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * GraphQL root object for JCR related mutations.
 */
@GraphQLName("JCRMutation")
@GraphQLDescription("JCR Mutations")
public class GqlJcrMutation extends GqlJcrMutationSupport implements DXGraphQLFieldCompleter {

    private final NodeQueryExtensions.Workspace workspace;
    private boolean save = true;

    /**
     * Initializes an instance of this class with the specified JCR workspace name.
     *
     * @param workspace the name of the JCR workspace
     * @deprecated use {@link #GqlJcrMutation(NodeQueryExtensions.Workspace, boolean)} instead
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public GqlJcrMutation(String workspace, boolean save) {
        this.workspace = NodeQueryExtensions.Workspace.fromName(workspace);
        this.save = save;
    }

    /**
     * Initializes an instance of this class with the specified JCR workspace.
     *
     * @param workspace the JCR workspace
     */
    public GqlJcrMutation(NodeQueryExtensions.Workspace workspace, boolean save) {
        this.workspace = workspace;
        this.save = save;
    }

    /**
     * Adds a child node to the specified one and returns the created mutation object.
     *
     * @param parentPathOrId  the path or UUID of the parent node
     * @param name            the name of the child node to be added
     * @param primaryNodeType the child node primary node type
     * @param mixins          collection of mixin types, which should be added to the created node
     * @param properties      collection of properties to be set on the newly created node
     * @param children        collection of child nodes to be added to the newly created node
     * @return the created mutation object
     * @throws BaseGqlClientException in case of JCR related errors during adding of child node
     */
    @GraphQLField
    @GraphQLDescription("Creates a new JCR node under the specified parent")
    public GqlJcrNodeMutation addNode(
            @GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the parent node") String parentPathOrId,
            @GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the node to create") String name,
            @GraphQLName("primaryNodeType") @GraphQLNonNull @GraphQLDescription("The primary node type of the node to create") String primaryNodeType,
            @GraphQLName("useAvailableNodeName") @GraphQLDescription("If true, use the next available name for a node, appending if needed numbers. Default is false") Boolean useAvailableNodeName,
            @GraphQLName("mixins") @GraphQLDescription("The collection of mixin type names") Collection<String> mixins,
            @GraphQLName("properties") Collection<GqlJcrPropertyInput> properties,
            @GraphQLName("children") Collection<GqlJcrNodeInput> children
    ) throws BaseGqlClientException {
        GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, useAvailableNodeName, mixins, properties, children);
        return new GqlJcrNodeMutation(addNode(getNodeFromPathOrId(getSession(), parentPathOrId), node));
    }

    /**
     * Performs multiple add-child node operations for the specified collection of inputs.
     *
     * @param nodes the collection of {@link GqlJcrNodeWithParentInput} objects, representing add-child operation request
     * @return a collection of created mutation objects
     * @throws BaseGqlClientException in case of JCR related errors during adding of child nodes, or if the batch
     *                                exceeds the configured mutation batch limit
     */
    @GraphQLField
    @GraphQLDescription("Batch creates a number of new JCR nodes under the specified parent")
    public Collection<GqlJcrNodeMutation> addNodesBatch(
            @GraphQLName("nodes") @GraphQLNonNull @GraphQLDescription("The collection of nodes to create") Collection<GqlJcrNodeWithParentInput> nodes
    ) throws BaseGqlClientException {
        GraphQLLimits.checkMutationBatchSize(nodes.size());
        List<GqlJcrNodeMutation> result = new ArrayList<>();
        for (GqlJcrNodeWithParentInput node : nodes) {
            result.add(new GqlJcrNodeMutation(addNode(getNodeFromPathOrId(getSession(), node.getParentPathOrId()), node)));
        }
        return result;
    }

    /**
     * Creates mutation object to apply modifications on the specified node.
     *
     * @param pathOrId the path or UUID of the node to apply modifications on
     * @return the mutation object for the specified node
     * @throws BaseGqlClientException in case of node retrieval error
     */
    @GraphQLField
    @GraphQLDescription("Mutates an existing node, based on path or id")
    public GqlJcrNodeMutation mutateNode(
            @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to mutate") String pathOrId
    ) throws BaseGqlClientException {
        return new GqlJcrNodeMutation(getNodeFromPathOrId(getSession(), pathOrId));
    }

    /**
     * Creates a list of mutation objects for the specified nodes.
     *
     * @param pathsOrIds the collection of path or UUIDs of the nodes to be modified
     * @return a collection with mutation objects for the specified nodes
     * @throws BaseGqlClientException in case of node retrieval error, or if the batch exceeds the configured
     *                                mutation batch limit
     */
    @GraphQLField
    @GraphQLDescription("Mutates a set of existing nodes, based on path or id")
    public Collection<GqlJcrNodeMutation> mutateNodes(
            @GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("The paths or id ofs the nodes to mutate") Collection<String> pathsOrIds
    ) throws BaseGqlClientException {
        GraphQLLimits.checkMutationBatchSize(pathsOrIds.size());
        List<GqlJcrNodeMutation> result = new ArrayList<>();
        for (String pathOrId : pathsOrIds) {
            result.add(new GqlJcrNodeMutation(getNodeFromPathOrId(getSession(), pathOrId)));
        }
        return result;
    }

    /**
     * Creates a collection of mutation objects for the nodes, matching the specified query.
     *
     * @param query         the query to retrieve the nodes to be modified
     * @param queryLanguage the query language
     * @param limit         how many of the matching nodes to operate on; a value above the configured mutation
     *                      batch limit is refused, and a page can still be refused when the rest of the request
     *                      claims the room for it
     * @param offset        the start offset of the result set
     * @return a collection of mutation objects
     * @throws BaseGqlClientException in case of node retrieval errors, if the caller asks for more nodes than the
     *                                configured mutation batch limit permits, or if the mutation would operate on more
     *                                than that limit leaves this request
     */
    @GraphQLField
    @GraphQLDescription("Mutates a set of existing nodes, based on query execution")
    public Collection<GqlJcrNodeMutation> mutateNodesByQuery(
            @GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
            @GraphQLName("queryLanguage") @GraphQLDefaultValue(GqlJcrQuery.QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") GqlJcrQuery.QueryLanguage queryLanguage,
            @GraphQLName("limit") @GraphQLDescription("How many of the matching nodes to operate on. A value above the configured mutation batch limit is refused, and a page can still be refused when the rest of the request claims the room for it.") Long limit,
            @GraphQLName("offset") @GraphQLDescription("The start offset of the result set") Long offset,
            DataFetchingEnvironment environment
    ) throws BaseGqlClientException {
        List<GqlJcrNodeMutation> result = new LinkedList<>();
        JCRNodeIteratorWrapper nodes;
        // Draw from what this request has left rather than the whole allowance: how many nodes the statement matches is
        // not knowable before it runs, so the pre-execution guard could not account for it and several aliased calls
        // would otherwise each be entitled to the full limit.
        AtomicInteger remaining = remainingBatchAllowance(environment);
        Long bound = GraphQLLimits.resolveMutationBatchBound(remaining);
        int configured = GraphQLLimits.getMutationBatchLimit();
        Long page = (limit != null && limit.longValue() > 0) ? limit : null;
        // A limit argument states how many nodes the caller wants to operate on, so asking for more than the instance
        // permits is refused here, before the query runs: whether the argument itself is acceptable must not depend on
        // how many nodes happen to match, nor on which other fields share the document. It is compared against the
        // configured limit and not against what the request has left, because the allowance is spent by every batch
        // field in the document - including ones that have not run - so comparing against it would refuse a page the
        // instance does in fact permit.
        if (configured > 0 && page != null && page.longValue() > configured) {
            throw new GqlLimitExceededException(batchBoundExceeded("This mutation asked to operate on " + page
                    + " nodes, more than ", configured, configured));
        }
        // What this field may then actually take is bounded by the allowance, which is a property of the whole request:
        // a page the instance permits still cannot be taken once the rest of the request has claimed the room for it.
        // Whether that happens depends on how many nodes match, so it is settled as the result is read.
        boolean guarded = bound != null && (page == null || page.longValue() > bound.longValue());
        try {
            QueryManagerWrapper queryManager = getSession().getWorkspace().getQueryManager();
            QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
            if (guarded) {
                // One more than the bound, so that reaching it can be told apart from matching it exactly.
                q.setLimit(bound.longValue() + 1);
            } else if (page != null) {
                q.setLimit(page.longValue());
            }
            if (offset != null && offset.longValue() > 0) {
                q.setOffset(offset.longValue());
            }
            nodes = q.execute().getNodes();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        while (nodes.hasNext()) {
            if (guarded && result.size() == bound.longValue()) {
                // Over the bound: fail rather than mutate a subset. Nothing is persisted, because the session is only
                // saved once the whole request completes without errors.
                throw new GqlLimitExceededException(batchBoundExceeded("This mutation matched more nodes than ",
                        bound.longValue(), configured));
            }
            JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
            result.add(new GqlJcrNodeMutation(node));
        }
        if (remaining != null) {
            remaining.addAndGet(-result.size());
        }
        return result;
    }

    /**
     * The bound a query-driven mutation is refused on is what the request has <em>left</em> of its allowance, so the
     * same number needs different advice. With the whole allowance still available it is what the instance permits, and
     * a smaller page is the answer. Once the rest of the request has claimed part of it a smaller page still works, but
     * only up to what is left; once the whole allowance is claimed nothing this field does will fit, and only another
     * request will. {@code configured} is what tells those apart, and is passed in so that one refusal reads a single
     * snapshot of it - a limit reconfigured mid-request can then misname the cause, but never the refusal itself.
     * <p>
     * The messages say "other fields" rather than "earlier fields" deliberately: the allowance is opened at
     * {@code maxBatchSize} minus the batch size of the <em>whole document</em>, measured before execution starts, so
     * the room a field is missing may have been claimed by one that has not run yet.
     *
     * @param lead       the sentence up to the bound, e.g. {@code "This mutation matched more nodes than "}
     * @param bound      how many nodes this field was allowed to operate on
     * @param configured the limit in force for the instance, which tells a partly spent allowance from a full one
     * @return the refusal message
     */
    private static String batchBoundExceeded(String lead, long bound, int configured) {
        if (configured <= 0 || bound >= configured) {
            return lead + "the maximum of " + bound + " it may operate on; operate on " + bound
                    + " or fewer at a time, using the limit/offset arguments.";
        }
        if (bound <= 0) {
            return lead + "what this request has left of its mutation batch allowance of " + configured
                    + ", which other fields in it already claim in full; move this mutation to another request.";
        }
        return lead + "the " + bound + " that other fields in this request leave of its mutation batch allowance of "
                + configured + "; operate on " + bound + " or fewer here, and move the rest to another request.";
    }

    /**
     * @return what is left of this request's mutation batch allowance, or {@code null} when the guard is not in play
     *         (bound disabled, or a caller that reaches this method outside a GraphQL execution)
     */
    private static AtomicInteger remainingBatchAllowance(DataFetchingEnvironment environment) {
        if (environment == null || environment.getGraphQlContext() == null) {
            return null;
        }
        return environment.getGraphQlContext().get(GraphQLLimits.REMAINING_BATCH_ALLOWANCE);
    }


    /**
     * Performs the deletion of the specified node (and all the subtree).
     *
     * @param pathOrId the path or UUID of the node to perform operation on
     * @return the result of the operation
     * @throws BaseGqlClientException in case of errors during the operation
     */
    @GraphQLField
    @GraphQLDescription("Delete an existing node and all its children")
    public boolean deleteNode(
            @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to delete") String pathOrId
    ) throws BaseGqlClientException {
        try {
            getNodeFromPathOrId(getSession(), pathOrId).remove();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    /**
     * Performs node delete or mark for deletion operation on the specified node.
     *
     * @param pathOrId the path or UUID of the node to perform operation on
     * @param comment  the comment, describing the purpose of the operation
     * @return the result of the operation
     * @throws BaseGqlClientException in case of errors during the operation
     */
    @GraphQLField
    @GraphQLDescription("Marks the existing node and all its children for deletion")
    public boolean markNodeForDeletion(
            @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to mark for deletion") String pathOrId,
            @GraphQLName("comment") @GraphQLDescription("Optional deletion comment") String comment
    ) throws BaseGqlClientException {
        try {
            getNodeFromPathOrId(getSession(), pathOrId).markForDeletion(comment);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    /**
     * Performs an unmark for deletion operation for the specified JCR node.
     *
     * @param pathOrId the path or UUID of the node to perform operation on
     * @return the result of the operation
     * @throws BaseGqlClientException in case of errors during undelete operation
     */
    @GraphQLField
    @GraphQLDescription("Unmarks the specified node and all its children for deletion")
    public boolean unmarkNodeForDeletion(
            @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to unmark for deletion") String pathOrId
    ) throws BaseGqlClientException {
        try {
            getNodeFromPathOrId(getSession(), pathOrId).unmarkForDeletion();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    /**
     * Import a file under the specified parent
     *
     * @param parentPathOrId the path or UUID of the parent node
     * @param file           name of the request part that contains desired import file body
     * @param environment    data fetching environment
     * @param rootBehaviour  Specify the behaviour in case of existing content
     * @return always true
     * @throws BaseGqlClientException in case of errors during import operation
     */
    @GraphQLField
    @GraphQLDescription("Import a file under the specified parent")
    public boolean importContent(
            @GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the parent node") String parentPathOrId,
            @GraphQLName("file") @GraphQLNonNull @GraphQLDescription("Name of the request part that contains desired import file body") String file,
            @GraphQLName("rootBehaviour") @GraphQLDefaultValue(DefaultRootBehaviour.class) @GraphQLDescription("Specify the behaviour in case"
                    + " of existing content, possible values are in the DocumentViewImportHandler class") Integer rootBehaviour,
            DataFetchingEnvironment environment
    ) throws BaseGqlClientException {
        importFileUpload(file, getNodeFromPathOrId(getSession(), parentPathOrId), rootBehaviour, environment);
        return true;
    }

    public static class DefaultRootBehaviour implements Supplier<Object> {
        @Override
        public Integer get() {
            return DocumentViewImportHandler.ROOT_BEHAVIOUR_RENAME;
        }
    }

    /**
     * Copy a single node to a different parent node.
     *
     * @param pathOrId           Path or UUID of the node to be copied
     * @param destParentPathOrId Path or UUID of the destination parent node to copy the node to
     * @param destName           The name of the node at the new location or null if its current name should be preserved
     * @return Mutation object representing the copy at the new location
     */
    @GraphQLField
    @GraphQLDescription("Copy a single node to a different parent node")
    public GqlJcrNodeMutation copyNode(
            @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the node to be copied") String pathOrId,
            @GraphQLName("destParentPathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the destination parent node to copy the node to") String destParentPathOrId,
            @GraphQLName("destName") @GraphQLDescription("The name of the node at the new location or null if its current name should be preserved") String destName,
            @GraphQLName("childNodeTypesToSkip") @GraphQLDescription("The child node types that should be skipped during copy") List<String> childNodeTypesToSkip
    ) throws BaseGqlClientException {

        JCRNodeWrapper destParentNode = getNodeFromPathOrId(getSession(), destParentPathOrId);
        JCRNodeWrapper node = getNodeFromPathOrId(getSession(), pathOrId);
        if (destName == null) {
            destName = node.getName();
        }

        verifyNodeReproductionTarget(node, destParentNode);

        JCRNodeWrapper destNode;
        try {
            if (childNodeTypesToSkip == null || childNodeTypesToSkip.isEmpty()) {
                if (!node.copy(destParentNode, destName, true, JCRNodeWrapper.NodeNamingConflictResolutionStrategy.FAIL)) {
                    throw new DataFetchingException("Error copying node '" + node.getPath() + "' to '" + destParentNode.getPath() + "'");
                }
            } else {
                JCRNodeWrapper newNode = destParentNode.addNode(destName, node.getPrimaryNodeTypeName());
                for (ExtendedNodeType mixin : node.getMixinNodeTypes()) {
                    if (!Constants.forbiddenMixinToCopy.contains(mixin.getName())) {
                        newNode.addMixin(mixin.getName());
                    }
                }
                Map<String, List<String>> references = new HashMap<>();
                node.copyProperties(newNode, references);
                ReferencesHelper.resolveCrossReferences(node.getSession(), references, false);

                Set<String> ignoreNodeTypes = new HashSet<>(childNodeTypesToSkip);
                // add default child node to skip to specified ones.
                ignoreNodeTypes.addAll(Constants.forbiddenChildNodeTypesToCopy);
                for (JCRNodeWrapper childNode : node.getNodes()) {
                    childNode.copy(newNode, childNode.getName(), true, new ArrayList<>(ignoreNodeTypes), SettingsBean.getInstance().getImportMaxBatch());
                }
                ReferencesHelper.resolveCrossReferences(getSession(), references, false);
            }

            destNode = destParentNode.getNode(destName);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }

        return new GqlJcrNodeMutation(destNode);
    }

    /**
     * Move a single node to a different parent node.
     *
     * @param pathOrId           Path or UUID of the node to be moved
     * @param destParentPathOrId Path or UUID of the destination parent node to move the node to
     * @param destName           The name of the node at the new location or null if its current name should be preserved
     * @return Mutation object representing the node at the new location
     */
    @GraphQLField
    @GraphQLDescription("Move a single node to a different parent node")
    public GqlJcrNodeMutation moveNode(
            @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the node to be moved") String pathOrId,
            @GraphQLName("destParentPathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the destination parent node to move the node to") String destParentPathOrId,
            @GraphQLName("destName") @GraphQLDescription("The name of the node at the new location or null if its current name should be preserved") String destName
    ) throws BaseGqlClientException {

        JCRNodeWrapper destParentNode = getNodeFromPathOrId(getSession(), destParentPathOrId);
        JCRNodeWrapper node = getNodeFromPathOrId(getSession(), pathOrId);
        if (destName == null) {
            destName = node.getName();
        }

        verifyNodeReproductionTarget(node, destParentNode);

        JCRNodeWrapper destNode;
        try {
            String destPath = destParentNode.getPath() + '/' + destName;
            getSession().move(node.getPath(), destPath);
            destNode = destParentNode.getNode(destName);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }

        return new GqlJcrNodeMutation(destNode);
    }

    /**
     * Copy multiple nodes to different parent node(s).
     *
     * @param nodes Info about nodes to copy and their new parent node(s)
     * @return A collection of mutation objects representing copied nodes at their new location(s)
     */
    @GraphQLField
    @GraphQLDescription("Copy multiple nodes to different parent node(s)")
    public Collection<GqlJcrNodeMutation> copyNodes(
            @GraphQLName("nodes") @GraphQLNonNull Collection<@GraphQLNonNull GqlJcrReproducibleNodeInput> nodes,
            @GraphQLName("childNodeTypesToSkip") @GraphQLDescription("The child node types that should be skipped during copy") List<String> childNodeTypesToSkip
    ) throws BaseGqlClientException {

        return reproduceNodes(nodes, new NodeReproducer() {

            @Override
            public GqlJcrNodeMutation reproduce(GqlJcrReproducibleNodeInput node) {
                return copyNode(node.getPathOrId(), node.getDestParentPathOrId(), node.getDestName(), childNodeTypesToSkip);
            }

            @Override
            public String getOperationName() {
                return "copying";
            }
        });
    }

    /**
     * Move multiple nodes to different parent node(s).
     *
     * @param nodes Info about nodes to move and their new parent node(s)
     * @return A collection of mutation objects representing moved nodes at their new location(s)
     */
    @GraphQLField
    @GraphQLDescription("Move multiple nodes to different parent node(s)")
    public Collection<GqlJcrNodeMutation> moveNodes(
            @GraphQLName("nodes") @GraphQLNonNull Collection<@GraphQLNonNull GqlJcrReproducibleNodeInput> nodes
    ) throws BaseGqlClientException {

        return reproduceNodes(nodes, new NodeReproducer() {

            @Override
            public GqlJcrNodeMutation reproduce(GqlJcrReproducibleNodeInput node) {
                return moveNode(node.getPathOrId(), node.getDestParentPathOrId(), node.getDestName());
            }

            @Override
            public String getOperationName() {
                return "moving";
            }
        });
    }

    private static void verifyNodeReproductionTarget(JCRNodeWrapper node, JCRNodeWrapper destParentNode) {
        if (destParentNode.equals(node) || destParentNode.getPath().startsWith(node.getPath() + "/")) {
            throw new GqlJcrWrongInputException("Cannot copy or move node '" + node.getPath() + "' to itself or its descendant node");
        }
    }

    private Collection<GqlJcrNodeMutation> reproduceNodes(Collection<GqlJcrReproducibleNodeInput> nodes, NodeReproducer nodeReproducer) throws BaseGqlClientException {

        ArrayList<GqlJcrNodeMutation> result = new ArrayList<>(nodes.size());
        LinkedList<Exception> exceptions = new LinkedList<>();

        for (GqlJcrReproducibleNodeInput node : nodes) {
            try {
                result.add(nodeReproducer.reproduce(node));
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            StringBuilder message = new StringBuilder("Errors " + nodeReproducer.getOperationName() + " nodes:\n");
            for (Exception e : exceptions) {
                message.append(e.getClass().getName()).append(": ").append(e.getMessage()).append('\n');
            }
            throw new DataFetchingException(message.toString());
        }

        return result;
    }

    private interface NodeReproducer {

        GqlJcrNodeMutation reproduce(GqlJcrReproducibleNodeInput node);

        String getOperationName();
    }

    /**
     * Get a collection of nodes that were modified by current GraphQL request.
     *
     * @return A collection of nodes that were modified by current GraphQL request
     */
    @GraphQLField
    @GraphQLDescription("Get a collection of nodes that were modified by current GraphQL request")
    public Collection<GqlJcrNode> getModifiedNodes() {
        return getSession().getChangedNodes().stream().map((node) -> {
            try {
                return SpecializedTypesHandler.getNode(node);
            } catch (RepositoryException e) {
                throw new JahiaRuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public JCRSessionWrapper getSession() {
        try {
            return JCRSessionFactory.getInstance().getCurrentUserSession(getWorkspace());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    public String getWorkspace() {
        return workspace == null ? null : workspace.getValue();
    }

    /**
     * Saves the changes in the current JCR session.
     */
    @Override
    public void completeField() {
        try {
            if (save) {
                // Validate all i18n sessions
                Set<JCRSessionWrapper> sessions = JCRSessionFactory.getInstance().getAllOpenUserSessions();
                for (JCRSessionWrapper session : sessions) {
                    session.validate();
                }

                getSession().save();
            }
        } catch (RepositoryException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
    }
}
