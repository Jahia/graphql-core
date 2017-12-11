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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl.getNodeInLanguage;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
public class NodeMutationExtensions {

    @GraphQLField
    public static GraphQLMutationJCR doInJcrSession(@GraphQLName("workspace") String workspace) throws RepositoryException {
        return new GraphQLMutationJCR(workspace);
    }

    @GraphQLName("MutationOnJcr")
    public static class GraphQLMutationJCR extends DXGraphQLProvider.Mutation {
        private String workspace;
        private JCRSessionWrapper session;

        public GraphQLMutationJCR(String workspace) throws RepositoryException {
            this.workspace = workspace;
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
        }

        @GraphQLField
        public GraphQLMutationNode addNode(@GraphQLName("parentPathOrId") @GraphQLNonNull String parentPathOrId, @GraphQLName("name") @GraphQLNonNull String name, @GraphQLName("primaryNodeType") @GraphQLNonNull String primaryNodeType) throws BaseGqlClientException {
            GqlJcrNode result = null;
            try {
                GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, null, null);
                result = SpecializedTypesHandler.getNode(internalAddNode(getNodeFromPathOrId(session, parentPathOrId), node));
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
            return new GraphQLMutationNode(result);
        }

        @GraphQLField
        public List<GraphQLMutationNode> addNodes(@GraphQLName("parentPathOrId") @GraphQLNonNull String parentPathOrId, @GraphQLName("nodes") @GraphQLNonNull List<GqlJcrNodeInput> nodes) throws BaseGqlClientException {
            List<GraphQLMutationNode> result = null;
            try {
                result = new ArrayList<>();
                for (GqlJcrNodeInput inputNode : nodes) {
                    GqlJcrNode jcrNode = SpecializedTypesHandler.getNode(internalAddNode(getNodeFromPathOrId(session, parentPathOrId), inputNode));
                    result.add(new GraphQLMutationNode(jcrNode));
                }
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
            return result;
        }

        @GraphQLField
        public GraphQLMutationNode updateNode(@GraphQLName("pathOrId") @GraphQLNonNull String pathOrId) throws RepositoryException {
            return new GraphQLMutationNode(SpecializedTypesHandler.getNode(getNodeFromPathOrId(session, pathOrId)));
        }

        @GraphQLField
        public List<GraphQLMutationNode> updateNodes(@GraphQLName("pathsOrIds") @GraphQLNonNull List<String> pathsOrIds) throws RepositoryException {
            List<GraphQLMutationNode> result = new ArrayList<>();
            for (String pathOrId : pathsOrIds) {
                result.add(new GraphQLMutationNode(SpecializedTypesHandler.getNode(getNodeFromPathOrId(session, pathOrId))));
            }
            return result;
        }

        @GraphQLField
        public List<GraphQLMutationNode> updateNodesByQuery(@GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
                                                            @GraphQLName("queryLanguage") @GraphQLDefaultValue(NodeQueryExtensions.QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") NodeQueryExtensions.QueryLanguage queryLanguage) throws RepositoryException {
            List<GraphQLMutationNode> result = new LinkedList<>();
            QueryManagerWrapper queryManager = session.getWorkspace().getQueryManager();
            QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
            JCRNodeIteratorWrapper nodes = q.execute().getNodes();
            while (nodes.hasNext()) {
                JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
                result.add(new GraphQLMutationNode(SpecializedTypesHandler.getNode(node)));
            }
            return result;
        }

        @GraphQLField
        public boolean deleteNode(@GraphQLName("pathOrId") @GraphQLNonNull String pathOrId, @GraphQLName("markForDeletion") Boolean markForDeletion, @GraphQLName("markForDeletionComment") String markForDeletionComment) throws BaseGqlClientException {
            try {
                if (markForDeletion != null && markForDeletion) {
                    getNodeFromPathOrId(session, pathOrId).markForDeletion(markForDeletionComment);
                } else {
                    getNodeFromPathOrId(session, pathOrId).remove();
                }
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
            return true;
        }

        @GraphQLField
        public boolean undeleteNode(@GraphQLName("pathOrId") @GraphQLNonNull String pathOrId) throws BaseGqlClientException {
            try {
                getNodeFromPathOrId(session, pathOrId).unmarkForDeletion();
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
            return true;
        }

        public void save() throws BaseGqlClientException {
            try {
                session.save();
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
        }
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
        public boolean deleteNode(@GraphQLName("markForDeletion") Boolean markForDeletion, @GraphQLName("markForDeletionComment") String markForDeletionComment) throws BaseGqlClientException {
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
        public boolean undeleteNode() throws BaseGqlClientException {
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
