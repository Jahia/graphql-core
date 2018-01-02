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


import graphql.ErrorType;
import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.*;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.query.QueryWrapper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.*;
import java.util.stream.Collectors;

import static org.jahia.modules.graphql.provider.dxm.node.NodeHelper.getNodeInLanguage;

/**
 * GraphQL root object for JCR related queries
 */
@GraphQLName("JCRMutation")
@GraphQLDescription("JCR Mutations")
public class GqlJcrMutation {

    private String workspace;

    private Set<JCRSessionWrapper> sessions = new LinkedHashSet<>();

    public GqlJcrMutation(String workspace) throws RepositoryException {
        this.workspace = workspace;
    }

    @GraphQLField
    public GraphQLMutationNode addNode(@GraphQLName("parentPathOrId") @GraphQLNonNull String parentPathOrId, @GraphQLName("name") @GraphQLNonNull String name, @GraphQLName("primaryNodeType") @GraphQLNonNull String primaryNodeType, @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) throws BaseGqlClientException {
        GqlJcrNode result = null;
        try {
            GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, null, null);
            result = SpecializedTypesHandler.getNode(internalAddNode(getNodeFromPathOrId(getSession(workspace), parentPathOrId), node));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return new GraphQLMutationNode(result);
    }

    @GraphQLField
    public List<GraphQLMutationNode> addNodes(@GraphQLName("parentPathOrId") @GraphQLNonNull String parentPathOrId, @GraphQLName("nodes") @GraphQLNonNull List<GqlJcrNodeInput> nodes, @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) throws BaseGqlClientException {
        List<GraphQLMutationNode> result = null;
        try {
            result = new ArrayList<>();
            for (GqlJcrNodeInput inputNode : nodes) {
                GqlJcrNode jcrNode = SpecializedTypesHandler.getNode(internalAddNode(getNodeFromPathOrId(getSession(workspace), parentPathOrId), inputNode));
                result.add(new GraphQLMutationNode(jcrNode));
            }
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return result;
    }

    @GraphQLField
    public GraphQLMutationNode updateNode(@GraphQLName("pathOrId") @GraphQLNonNull String pathOrId, @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) throws RepositoryException {
        return new GraphQLMutationNode(SpecializedTypesHandler.getNode(getNodeFromPathOrId(getSession(workspace), pathOrId)));
    }

    @GraphQLField
    public List<GraphQLMutationNode> updateNodes(@GraphQLName("pathsOrIds") @GraphQLNonNull List<String> pathsOrIds, @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) throws RepositoryException {
        List<GraphQLMutationNode> result = new ArrayList<>();
        for (String pathOrId : pathsOrIds) {
            result.add(new GraphQLMutationNode(SpecializedTypesHandler.getNode(getNodeFromPathOrId(getSession(workspace), pathOrId))));
        }
        return result;
    }

    @GraphQLField
    public List<GraphQLMutationNode> updateNodesByQuery(@GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
                                                        @GraphQLName("queryLanguage") @GraphQLDefaultValue(GqlJcrQuery.QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") GqlJcrQuery.QueryLanguage queryLanguage, @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) throws RepositoryException {
        List<GraphQLMutationNode> result = new LinkedList<>();
        QueryManagerWrapper queryManager = getSession(workspace).getWorkspace().getQueryManager();
        QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
        JCRNodeIteratorWrapper nodes = q.execute().getNodes();
        while (nodes.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
            result.add(new GraphQLMutationNode(SpecializedTypesHandler.getNode(node)));
        }
        return result;
    }

    @GraphQLField
    public boolean deleteNode(@GraphQLName("pathOrId") @GraphQLNonNull String pathOrId, @GraphQLName("markForDeletion") Boolean markForDeletion, @GraphQLName("markForDeletionComment") String markForDeletionComment, @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) throws BaseGqlClientException {
        try {
            if (markForDeletion != null && markForDeletion) {
                getNodeFromPathOrId(getSession(workspace), pathOrId).markForDeletion(markForDeletionComment);
            } else {
                getNodeFromPathOrId(getSession(workspace), pathOrId).remove();
            }
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    @GraphQLField
    public boolean undeleteNode(@GraphQLName("pathOrId") @GraphQLNonNull String pathOrId, @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) throws BaseGqlClientException {
        try {
            getNodeFromPathOrId(getSession(workspace), pathOrId).unmarkForDeletion();
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    public void save() throws BaseGqlClientException {
        try {
            for (JCRSessionWrapper session : sessions) {
                session.save();
            }
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    private JCRSessionWrapper getSession(String workspace) throws RepositoryException {
        if (workspace == null) {
            workspace = this.workspace;
        }
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
        sessions.add(session);
        return session;
    }



    @GraphQLName("MutationOnJcrNode")
    public static class GraphQLMutationNode extends DXGraphQLProvider.Mutation {

        public GqlJcrNode node;

        public GraphQLMutationNode(GqlJcrNode node) {
            this.node = node;
        }

        @GraphQLField
        public GqlJcrNode getNode() {
            return node;
        }

        @GraphQLField
        public GraphQLMutationNode addNode(@GraphQLName("name") @GraphQLNonNull String name, @GraphQLName("primaryNodeType") @GraphQLNonNull String primaryNodeType) throws BaseGqlClientException {
            GqlJcrNode result = null;
            try {
                GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, null, null);
                result = SpecializedTypesHandler.getNode(internalAddNode(getNode().getNode(), node));
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
            return new GraphQLMutationNode(result);
        }

        @GraphQLField
        public List<GraphQLMutationNode> addNodes(@GraphQLName("nodes") @GraphQLNonNull List<GqlJcrNodeInput> nodes) throws BaseGqlClientException {
            List<GraphQLMutationNode> result = null;
            try {
                result = new ArrayList<>();
                for (GqlJcrNodeInput inputNode : nodes) {
                    GqlJcrNode jcrNode = SpecializedTypesHandler.getNode(internalAddNode(getNode().getNode(), inputNode));
                    result.add(new GraphQLMutationNode(jcrNode));
                }
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
            return result;
        }

        @GraphQLField
        @GraphQLName("setProperties")
        public List<String> setProperties(@GraphQLName("properties") @GraphQLNonNull List<GqlJcrPropertyInput> properties) throws BaseGqlClientException {
            try {
                List<String> names = new ArrayList<>();
                List<JCRPropertyWrapper> propertiesOutput = internalSetProperties(this.node.getNode(), properties);
                for (JCRPropertyWrapper property : propertiesOutput) {
                    names.add(property.getName());
                }
                return names;
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
        }

        @GraphQLField
        public List<String> addMixins(@GraphQLName("mixins") @GraphQLNonNull List<String> names) throws BaseGqlClientException {
            try {
                for (String name : names) {
                    this.node.getNode().addMixin(name);
                }
                return Arrays.stream(node.getNode().getMixinNodeTypes()).map(ExtendedNodeType::getName).collect(Collectors.toList());
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
        }

        @GraphQLField
        public List<String> removeMixins(@GraphQLName("mixins") @GraphQLNonNull List<String> names) throws BaseGqlClientException {
            try {
                for (String name : names) {
                    this.node.getNode().removeMixin(name);
                }
                return Arrays.stream(node.getNode().getMixinNodeTypes()).map(ExtendedNodeType::getName).collect(Collectors.toList());
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
        }

        @GraphQLField
        public String rename(@GraphQLName("name") @GraphQLNonNull String newName) throws BaseGqlClientException {
            try {
                JCRNodeWrapper node = getNode().getNode();
                node.rename(newName);
                return node.getPath();
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
        }

        @GraphQLField
        public String move(@GraphQLName("parentPathOrId") @GraphQLNonNull String parentPathOrId) throws BaseGqlClientException {
            try {
                JCRNodeWrapper node = getNode().getNode();
                JCRNodeWrapper parentDest = getNodeFromPathOrId(node.getSession(), parentPathOrId);
                node.getSession().move(node.getPath(), parentDest.getPath() + "/" + node.getName());
                return node.getPath();
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
        }

        @GraphQLField
        public boolean delete(@GraphQLName("markForDeletion") Boolean markForDeletion, @GraphQLName("markForDeletionComment") String markForDeletionComment) throws BaseGqlClientException {
            try {
                if (markForDeletion != null && markForDeletion) {
                    node.getNode().markForDeletion(markForDeletionComment);
                } else {
                    node.getNode().remove();
                }
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
            return true;
        }

        @GraphQLField
        public boolean undelete() throws BaseGqlClientException {
            try {
                node.getNode().unmarkForDeletion();
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
            return true;
        }


    }

    private static JCRNodeWrapper internalAddNode(JCRNodeWrapper parent, GqlJcrNodeInput node) throws RepositoryException {
        JCRNodeWrapper n = parent.addNode(node.name, node.primaryNodeType);
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

    private static List<JCRPropertyWrapper> internalSetProperties(JCRNodeWrapper node, List<GqlJcrPropertyInput> properties) throws RepositoryException {
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

    private static JCRNodeWrapper getNodeFromPathOrId(JCRSessionWrapper session, String pathOrId) throws RepositoryException {
        if (pathOrId.startsWith("/")) {
            return session.getNode(pathOrId);
        } else {
            return session.getNodeByIdentifier(pathOrId);
        }
    }


}
