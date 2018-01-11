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
import org.jahia.services.content.*;
import org.jahia.services.query.QueryWrapper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.*;

import static org.jahia.modules.graphql.provider.dxm.node.NodeHelper.getNodeInLanguage;

/**
 * GraphQL root object for JCR related queries
 */
@GraphQLName("JCRMutation")
@GraphQLDescription("JCR Mutations")
public class GqlJcrMutation {

    private String workspace;

    public GqlJcrMutation(String workspace) throws RepositoryException {
        this.workspace = workspace;
    }

    @GraphQLField
    @GraphQLDescription("Creates a new JCR node under the specified parent")
    public GqlJcrNodeAddResult addNode(@GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the parent node") String parentPathOrId,
                                       @GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the node to create")  String name,
                                       @GraphQLName("primaryNodeType") @GraphQLNonNull @GraphQLDescription("The primary node type of the node to create") String primaryNodeType) throws BaseGqlClientException {
        GqlJcrNode result = null;
        try {
            GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, null, null, null);
            result = SpecializedTypesHandler.getNode(internalAddNode(getNodeFromPathOrId(getSession(), parentPathOrId), node));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return new GqlJcrNodeAddResult(result);
    }

    @GraphQLField
    @GraphQLDescription("Batch creates a list of new JCR nodes under the specified parent")
    public List<GqlJcrNodeAddResult> addNodesBatch(@GraphQLName("nodes") @GraphQLNonNull @GraphQLDescription("The list of nodes to create") List<GqlJcrNodeWithParentInput> nodes) throws BaseGqlClientException {
        List<GqlJcrNodeAddResult> result = null;
        try {
            result = new ArrayList<>();
            for (GqlJcrNodeWithParentInput inputNode : nodes) {
                GqlJcrNode jcrNode = SpecializedTypesHandler.getNode(internalAddNode(getNodeFromPathOrId(getSession(), inputNode.parentPathOrId), inputNode));
                result.add(new GqlJcrNodeAddResult(jcrNode));
            }
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return result;
    }

    @GraphQLField
    @GraphQLDescription("Mutates an existing node, based on path or id")
    public GqlJcrMutationNode mutateNode(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to mutate") String pathOrId) throws RepositoryException {
        return new GqlJcrMutationNode(getNodeFromPathOrId(getSession(), pathOrId));
    }

    @GraphQLField
    @GraphQLDescription("Mutates a set of existing nodes, based on path or id")
    public List<GqlJcrMutationNode> mutateNodes(@GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("The paths or id ofs the nodes to mutate") List<String> pathsOrIds) throws RepositoryException {
        List<GqlJcrMutationNode> result = new ArrayList<>();
        for (String pathOrId : pathsOrIds) {
            result.add(new GqlJcrMutationNode(getNodeFromPathOrId(getSession(), pathOrId)));
        }
        return result;
    }

    @GraphQLField
    @GraphQLDescription("Mutates a set of existing nodes, based on query execution")
    public List<GqlJcrMutationNode> mutateNodesByQuery(@GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
                                                       @GraphQLName("queryLanguage") @GraphQLDefaultValue(GqlJcrQuery.QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") GqlJcrQuery.QueryLanguage queryLanguage) throws RepositoryException {
        List<GqlJcrMutationNode> result = new LinkedList<>();
        QueryManagerWrapper queryManager = getSession().getWorkspace().getQueryManager();
        QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
        JCRNodeIteratorWrapper nodes = q.execute().getNodes();
        while (nodes.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
            result.add(new GqlJcrMutationNode(node));
        }
        return result;
    }

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
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    @GraphQLField
    public boolean undeleteNode(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the node to undelete") String pathOrId) throws BaseGqlClientException {
        try {
            getNodeFromPathOrId(getSession(), pathOrId).unmarkForDeletion();
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    public void save() throws BaseGqlClientException {
        try {
            getSession().save();
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
    }


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

    public static void internalSetProperties(JCRNodeWrapper node, List<GqlJcrPropertyInput> properties) throws RepositoryException {
        for (GqlJcrPropertyInput property : properties) {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, property.language);
            JCRSessionWrapper session = localizedNode.getSession();

            int type = property.type != null ? property.type.getValue() : PropertyType.STRING;
            if (property.value != null) {
                Value v = session.getValueFactory().createValue(property.value, type);
                localizedNode.setProperty(property.name, v);
            } else if (property.values != null) {
                List<Value> values = new ArrayList<>();
                for (String value : property.values) {
                    values.add(session.getValueFactory().createValue(value, type));
                }
                localizedNode.setProperty(property.name, values.toArray(new Value[values.size()]));
            }
        }
    }

    public static JCRNodeWrapper getNodeFromPathOrId(JCRSessionWrapper session, String pathOrId) throws RepositoryException {
        if (pathOrId.startsWith("/")) {
            return session.getNode(pathOrId);
        } else {
            return session.getNodeByIdentifier(pathOrId);
        }
    }


}
