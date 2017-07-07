package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLRelayMutation;
import graphql.annotations.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.GraphQLContext;
import graphql.servlet.GraphQLMutation;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(GraphQLMutation.class)
public class NodeMutationExtensions {

    @GraphQLField
    @GraphQLRelayMutation
    public static void jcrSessionSave(@GraphQLName("workspace") String workspace) {
        try {
            JCRSessionFactory.getInstance().getCurrentUserSession(workspace).save();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLRelayMutation
    public static GraphQLMutationJCR doInJcrSession(@GraphQLName("workspace") String workspace) {
        try {
            return new GraphQLMutationJCR(JCRSessionFactory.getInstance().getCurrentUserSession(workspace));
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    public static GraphQLMutationNode addNode2(@GraphQLName("node") DXGraphQLJCRNodeInput node, @GraphQLName("parentPath") String parentPath, @GraphQLName("workspace") String workspace) {
        try {
            DXGraphQLJCRNode result = SpecializedTypesHandler.getNode(addNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(parentPath), node));
            return new GraphQLMutationNode(result);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }


    public static class GraphQLMutationJCR extends GraphQLMutation {
        public JCRSessionWrapper session;

        public GraphQLMutationJCR(JCRSessionWrapper session) {
            this.session = session;
        }

        @GraphQLField
        public GraphQLMutationNode addNode2(@GraphQLName("node") DXGraphQLJCRNodeInput node, @GraphQLName("parentPath") String parentPath) {
            try {
                DXGraphQLJCRNode result = SpecializedTypesHandler.getNode(addNode(this.session.getNode(parentPath), node));
                return new GraphQLMutationNode(result);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public static class GraphQLMutationNode extends GraphQLMutation {

        @GraphQLField
        public DXGraphQLJCRNode node;

        public GraphQLMutationNode(DXGraphQLJCRNode node) throws RepositoryException {
            this.node = node;
        }

        @GraphQLField
        public GraphQLMutationNode addNode2(@GraphQLName("node") DXGraphQLJCRNodeInput node) {
            try {
                DXGraphQLJCRNode result = SpecializedTypesHandler.getNode(addNode(this.node.getNode(), node));
                return new GraphQLMutationNode(result);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @GraphQLField
//    @GraphQLRelayMutation
    public static DXGraphQLJCRNode addNode(DataFetchingEnvironment env, @GraphQLName("node") DXGraphQLJCRNodeInput node, @GraphQLName("parentPath") String parentPath, @GraphQLName("workspace") String workspace) {
        try {
            GraphQLContext context = env.getContext();
            if (parentPath == null && context.getRequest().isPresent()) {
                DXGraphQLJCRNode parent = (DXGraphQLJCRNode) context.getRequest().get().getAttribute("lastNode");
                parentPath = parent.getPath();
            }

            DXGraphQLJCRNode result = SpecializedTypesHandler.getNode(addNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(parentPath), node));

            if (context.getRequest().isPresent()) {
                context.getRequest().get().setAttribute("lastNode", result);
            }

            return result;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private static JCRNodeWrapper addNode(JCRNodeWrapper parent, DXGraphQLJCRNodeInput node) throws RepositoryException {
        JCRNodeWrapper n = parent.addNode(node.name, node.primaryNodeType);
        for (DXGraphQLJCRNodeInput child : node.children) {
            addNode(n, child);
        }
        return n;
    }

}
