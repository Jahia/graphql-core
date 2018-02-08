/*
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


import graphql.ErrorType;
import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.*;
import org.jahia.services.query.QueryWrapper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.*;

import static org.jahia.modules.graphql.provider.dxm.node.NodeHelper.getNodeInLanguage;

/**
 * GraphQL root object for JCR related mutations.
 */
@GraphQLName("JCRMutation")
@GraphQLDescription("JCR Mutations")
public class GqlJcrMutation {

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
     * @return the created mutation object
     * @throws BaseGqlClientException in case of JCR related errors during adding of child node
     */
    @GraphQLField
    @GraphQLDescription("Creates a new JCR node under the specified parent")
    public GqlJcrNodeMutation addNode(@GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the parent node") String parentPathOrId,
                                       @GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the node to create")  String name,
                                       @GraphQLName("primaryNodeType") @GraphQLNonNull @GraphQLDescription("The primary node type of the node to create") String primaryNodeType) throws BaseGqlClientException {
        try {
            GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, null, null, null);
            return new GqlJcrNodeMutation(internalAddNode(getNodeFromPathOrId(getSession(), parentPathOrId), node));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Performs multiple add-child node operations for the specified list of inputs.
     * 
     * @param nodes the list of {@link GqlJcrNodeWithParentInput} objects, representing add-child operation request
     * 
     * @return the list of created mutation objects
     * @throws BaseGqlClientException in case of JCR related errors during adding of child nodes
     */
    @GraphQLField
    @GraphQLDescription("Batch creates a list of new JCR nodes under the specified parent")
    public List<GqlJcrNodeMutation> addNodesBatch(@GraphQLName("nodes") @GraphQLNonNull @GraphQLDescription("The list of nodes to create") List<GqlJcrNodeWithParentInput> nodes) throws BaseGqlClientException {
        List<GqlJcrNodeMutation> result = null;
        try {
            result = new ArrayList<>();
            for (GqlJcrNodeWithParentInput inputNode : nodes) {
                result.add(new GqlJcrNodeMutation(internalAddNode(getNodeFromPathOrId(getSession(), inputNode.parentPathOrId), inputNode)));
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return result;
    }

    /**
     * Creates mutation object to apply modifications on the specified node.
     * 
     * @param pathOrId the path or UUID of the node to apply modifications on
     * @return the mutation object for the specified node
     * @throws RepositoryException in case of node retrieval operation
     */
    @GraphQLField
    @GraphQLDescription("Mutates an existing node, based on path or id")
    public GqlJcrNodeMutation mutateNode(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to mutate") String pathOrId) throws RepositoryException {
        return new GqlJcrNodeMutation(getNodeFromPathOrId(getSession(), pathOrId));
    }

    /**
     * Creates a list of mutation objects for the specified nodes.
     * 
     * @param pathsOrIds the list of path or UUIDs of the nodes to be modified
     * @return the list with mutation objects for the specified nodes
     * @throws RepositoryException in case of node retrieval
     */
    @GraphQLField
    @GraphQLDescription("Mutates a set of existing nodes, based on path or id")
    public List<GqlJcrNodeMutation> mutateNodes(@GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("The paths or id ofs the nodes to mutate") List<String> pathsOrIds) throws RepositoryException {
        List<GqlJcrNodeMutation> result = new ArrayList<>();
        for (String pathOrId : pathsOrIds) {
            result.add(new GqlJcrNodeMutation(getNodeFromPathOrId(getSession(), pathOrId)));
        }
        return result;
    }

    /**
     * Creates a list of mutation objects for the nodes, matching the specified query.
     * 
     * @param query the query to retrieve the nodes to be modified
     * @param queryLanguage the query language
     * @return the list with mutation objects
     * @throws RepositoryException in case of node retrieval
     */
    @GraphQLField
    @GraphQLDescription("Mutates a set of existing nodes, based on query execution")
    public List<GqlJcrNodeMutation> mutateNodesByQuery(@GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
                                                       @GraphQLName("queryLanguage") @GraphQLDefaultValue(GqlJcrQuery.QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") GqlJcrQuery.QueryLanguage queryLanguage) throws RepositoryException {
        List<GqlJcrNodeMutation> result = new LinkedList<>();
        QueryManagerWrapper queryManager = getSession().getWorkspace().getQueryManager();
        QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
        JCRNodeIteratorWrapper nodes = q.execute().getNodes();
        while (nodes.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
            result.add(new GqlJcrNodeMutation(node));
        }
        return result;
    }

    /**
     * Performs node delete or mark for deletion operation on the specified node.
     * 
     * @param pathOrId the path or UUID of the node to perform operation on
     * @param markForDeletion <code>true</code> if the node should be marked for deletion; <code>false</code> in case the node should be
     *            directly removed
     * @param markForDeletionComment in case of mark for deletion operation, specified the comment, describing the purpose of the operation
     * @return the result of the operation
     * @throws BaseGqlClientException in case of errors during the operation
     */
    @GraphQLField
    @GraphQLDescription("Delete an existing node or mark it for deletion")
    public boolean deleteNode(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to delete") String pathOrId,
                              @GraphQLName("markForDeletion") @GraphQLDescription("If the node should be marked for deletion or completely removed") Boolean markForDeletion,
                              @GraphQLName("markForDeletionComment") @GraphQLDescription("Optional comment if node is marked for deletion") String markForDeletionComment) throws BaseGqlClientException {
        try {
            if (markForDeletion != null && markForDeletion) {
                getNodeFromPathOrId(getSession(), pathOrId).markForDeletion(markForDeletionComment);
            } else {
                getNodeFromPathOrId(getSession(), pathOrId).remove();
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    /**
     * Performs an undelete (unmark for deletion) operation for the specified JCR node.
     * 
     * @param pathOrId the path or UUID of the node to perform operation on
     * @return the result of the operation
     * @throws BaseGqlClientException in case of errors during undelete operation
     */
    @GraphQLField
    public boolean undeleteNode(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to undelete") String pathOrId) throws BaseGqlClientException {
        try {
            getNodeFromPathOrId(getSession(), pathOrId).unmarkForDeletion();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        
        return true;
    }

    /**
     * Saves the changes in the current JCR session.
     * 
     * @throws BaseGqlClientException in case of errors during session save operation
     */
    public void save() throws BaseGqlClientException {
        try {
            getSession().save();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
    }


    /**
     * Adds a child node for the specified one.
     * 
     * @param parent the node to add child for
     * @param node the child node to be added
     * @return the child JCR node that was added
     * @throws RepositoryException in case of a JCR error during add operation
     */
    public static JCRNodeWrapper internalAddNode(JCRNodeWrapper parent, GqlJcrNodeInput node) throws RepositoryException {
        JCRNodeWrapper n = parent.addNode(node.name, node.primaryNodeType);
        if (node.mixins != null) {
            for (String mixin : node.mixins) {
                n.addMixin(mixin);
            }
        }
        if (node.properties != null) {
            internalSetProperties(n, node.properties);
        }
        if (node.children != null) {
            for (GqlJcrNodeInput child : node.children) {
                internalAddNode(n, child);
            }
        }
        return n;
    }

    /**
     * Set the provided properties to the specified node.
     * 
     * @param node the JCR node to set properties on
     * @param properties the collection of properties to be set
     * @return the result of the operation, containing list of modified JCR properties
     * @throws RepositoryException in case of a JCR error during node update
     */
    public static List<JCRPropertyWrapper> internalSetProperties(JCRNodeWrapper node, Collection<GqlJcrPropertyInput> properties) throws RepositoryException {
        List<JCRPropertyWrapper> result = new ArrayList<>();
        for (GqlJcrPropertyInput property : properties) {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, property.language);
            JCRSessionWrapper session = localizedNode.getSession();

            int type = property.type != null ? property.type.getValue() : PropertyType.STRING;
            if (property.value != null) {
                Value v = session.getValueFactory().createValue(property.value, type);
                result.add(localizedNode.setProperty(property.name, v));
            } else if (property.values != null) {
                List<Value> values = new ArrayList<>();
                for (String value : property.values) {
                    values.add(session.getValueFactory().createValue(value, type));
                }
                result.add(localizedNode.setProperty(property.name, values.toArray(new Value[values.size()])));
            }
        }
        return result;
    }

    /**
     * Retrieves the specified JCR node.
     * 
     * @param session the current JCR session
     * @param pathOrId the string with either node UUID or its path
     * @return the requested JCR node
     * @throws RepositoryException in case of node retrieval operation
     */
    public static JCRNodeWrapper getNodeFromPathOrId(JCRSessionWrapper session, String pathOrId)
            throws RepositoryException {
        return '/' == pathOrId.charAt(0) ? session.getNode(pathOrId) : session.getNodeByIdentifier(pathOrId);
    }
}
