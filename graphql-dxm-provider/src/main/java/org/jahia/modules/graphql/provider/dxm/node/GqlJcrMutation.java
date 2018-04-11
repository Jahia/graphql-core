/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.*;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLFieldCompleter;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.*;
import org.jahia.services.query.QueryWrapper;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GraphQL root object for JCR related mutations.
 */
@GraphQLName("JCRMutation")
@GraphQLDescription("JCR Mutations")
public class GqlJcrMutation extends GqlJcrMutationSupport implements DXGraphQLFieldCompleter {

    private String workspace;

    /**
     * Initializes an instance of this class with the specified JCR workspace name.
     * @param workspace the name of the JCR workspace
     */
    public GqlJcrMutation(String workspace) {
        this.workspace = workspace;
    }

    /**
     * Adds a child node to the specified one and returns the created mutation object.
     *
     * @param parentPathOrId the path or UUID of the parent node
     * @param name the name of the child node to be added
     * @param primaryNodeType the child node primary node type
     * @param mixins collection of mixin types, which should be added to the created node
     * @param properties collection of properties to be set on the newly created node
     * @param children collection of child nodes to be added to the newly created node
     * @return the created mutation object
     * @throws BaseGqlClientException in case of JCR related errors during adding of child node
     */
    @GraphQLField
    @GraphQLDescription("Creates a new JCR node under the specified parent")
    public GqlJcrNodeMutation addNode(@GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the parent node") String parentPathOrId,
                                      @GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the node to create")  String name,
                                      @GraphQLName("primaryNodeType") @GraphQLNonNull @GraphQLDescription("The primary node type of the node to create") String primaryNodeType,
                                      @GraphQLName("mixins") @GraphQLDescription("The collection of mixin type names") Collection<String> mixins,
                                      @GraphQLName("properties") Collection<GqlJcrPropertyInput> properties,
                                      @GraphQLName("children") Collection<GqlJcrNodeInput> children)
    throws BaseGqlClientException {
        GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, mixins, properties, children);
        return new GqlJcrNodeMutation(addNode(getNodeFromPathOrId(getSession(), parentPathOrId), node));
    }

    /**
     * Performs multiple add-child node operations for the specified collection of inputs.
     *
     * @param nodes the collection of {@link GqlJcrNodeWithParentInput} objects, representing add-child operation request
     *
     * @return a collection of created mutation objects
     * @throws BaseGqlClientException in case of JCR related errors during adding of child nodes
     */
    @GraphQLField
    @GraphQLDescription("Batch creates a number of new JCR nodes under the specified parent")
    public Collection<GqlJcrNodeMutation> addNodesBatch(@GraphQLName("nodes") @GraphQLNonNull @GraphQLDescription("The collection of nodes to create") Collection<GqlJcrNodeWithParentInput> nodes) throws BaseGqlClientException {
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
    public GqlJcrNodeMutation mutateNode(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to mutate") String pathOrId) throws BaseGqlClientException {
        return new GqlJcrNodeMutation(getNodeFromPathOrId(getSession(), pathOrId));
    }

    /**
     * Creates a list of mutation objects for the specified nodes.
     *
     * @param pathsOrIds the collection of path or UUIDs of the nodes to be modified
     * @return a collection with mutation objects for the specified nodes
     * @throws BaseGqlClientException in case of node retrieval error
     */
    @GraphQLField
    @GraphQLDescription("Mutates a set of existing nodes, based on path or id")
    public Collection<GqlJcrNodeMutation> mutateNodes(@GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("The paths or id ofs the nodes to mutate") Collection<String> pathsOrIds) throws BaseGqlClientException {
        List<GqlJcrNodeMutation> result = new ArrayList<>();
        for (String pathOrId : pathsOrIds) {
            result.add(new GqlJcrNodeMutation(getNodeFromPathOrId(getSession(), pathOrId)));
        }
        return result;
    }

    /**
     * Creates a collection of mutation objects for the nodes, matching the specified query.
     *
     * @param query the query to retrieve the nodes to be modified
     * @param queryLanguage the query language
     * @param limit the maximum size of the result set
     * @param offset the start offset of the result set
     * @return a collection of mutation objects
     * @throws BaseGqlClientException in case of node retrieval errors
     */
    @GraphQLField
    @GraphQLDescription("Mutates a set of existing nodes, based on query execution")
    public Collection<GqlJcrNodeMutation> mutateNodesByQuery(@GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
                                                             @GraphQLName("queryLanguage") @GraphQLDefaultValue(GqlJcrQuery.QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") GqlJcrQuery.QueryLanguage queryLanguage,
                                                             @GraphQLName("limit") @GraphQLDescription("The maximum size of the result set") Long limit,
                                                             @GraphQLName("offset") @GraphQLDescription("The start offset of the result set") Long offset)
    throws BaseGqlClientException {
        List<GqlJcrNodeMutation> result = new LinkedList<>();
        JCRNodeIteratorWrapper nodes;
        try {
            QueryManagerWrapper queryManager = getSession().getWorkspace().getQueryManager();
            QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
            if (limit != null && limit.longValue() > 0) {
                q.setLimit(limit.longValue());
            }
            if (offset != null && offset.longValue() > 0) {
                q.setOffset(offset.longValue());
            }
            nodes = q.execute().getNodes();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        while (nodes.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
            result.add(new GqlJcrNodeMutation(node));
        }
        return result;
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
    public boolean deleteNode(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to delete") String pathOrId) throws BaseGqlClientException {
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
     * @param comment the comment, describing the purpose of the operation
     * @return the result of the operation
     * @throws BaseGqlClientException in case of errors during the operation
     */
    @GraphQLField
    @GraphQLDescription("Marks the existing node and all its children for deletion")
    public boolean markNodeForDeletion(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to mark for deletion") String pathOrId,
                                       @GraphQLName("comment") @GraphQLDescription("Optional deletion comment") String comment)
    throws BaseGqlClientException {
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
    public boolean unmarkNodeForDeletion(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to unmark for deletion") String pathOrId) throws BaseGqlClientException {
        try {
            getNodeFromPathOrId(getSession(), pathOrId).unmarkForDeletion();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    /**
     * Get the list of all nodes that have been modified in this session since last save.
     *
     * @return the result of the operation
     */
    @GraphQLField
    @GraphQLDescription("Get the list of all nodes that have been modified in this session since last save")
    public List<GqlJcrNode> getModifiedNodes() {
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
            return JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Saves the changes in the current JCR session.
     */
    @Override
    public void completeField() {
        try {
            getSession().save();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
