package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLRelayMutation;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.GraphQLContext;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
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
    public static GraphQLMutationNode addNode2(@GraphQLName("node") GqlJcrNodeInput node, @GraphQLName("parentPath") String parentPath, @GraphQLName("workspace") String workspace) {
        try {
            GqlJcrNode result = SpecializedTypesHandler.getNode(addNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(parentPath), node));
            return new GraphQLMutationNode(result);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }


    public static class GraphQLMutationJCR extends DXGraphQLProvider.Mutation {
        public JCRSessionWrapper session;

        public GraphQLMutationJCR(JCRSessionWrapper session) {
            this.session = session;
        }

        @GraphQLField
        public GraphQLMutationNode addNode2(@GraphQLName("node") GqlJcrNodeInput node, @GraphQLName("parentPath") String parentPath) {
            try {
                GqlJcrNode result = SpecializedTypesHandler.getNode(addNode(this.session.getNode(parentPath), node));
                return new GraphQLMutationNode(result);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public static class GraphQLMutationNode extends DXGraphQLProvider.Mutation {

        @GraphQLField
        public GqlJcrNode node;

        public GraphQLMutationNode(GqlJcrNode node) throws RepositoryException {
            this.node = node;
        }

        @GraphQLField
        public GraphQLMutationNode addNode2(@GraphQLName("node") GqlJcrNodeInput node) {
            try {
                GqlJcrNode result = SpecializedTypesHandler.getNode(addNode(this.node.getNode(), node));
                return new GraphQLMutationNode(result);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @GraphQLField
//    @GraphQLRelayMutation
    public static GqlJcrNode addNode(DataFetchingEnvironment env, @GraphQLName("node") GqlJcrNodeInput node, @GraphQLName("parentPath") String parentPath, @GraphQLName("workspace") String workspace) {
        try {
            GraphQLContext context = env.getContext();
            if (parentPath == null && context.getRequest().isPresent()) {
                GqlJcrNode parent = (GqlJcrNode) context.getRequest().get().getAttribute("lastNode");
                parentPath = parent.getPath();
            }

            GqlJcrNode result = SpecializedTypesHandler.getNode(addNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(parentPath), node));

            if (context.getRequest().isPresent()) {
                context.getRequest().get().setAttribute("lastNode", result);
            }

            return result;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private static JCRNodeWrapper addNode(JCRNodeWrapper parent, GqlJcrNodeInput node) throws RepositoryException {
        JCRNodeWrapper n = parent.addNode(node.name, node.primaryNodeType);
        if (node.children != null) {
            for (GqlJcrNodeInput child : node.children) {
                addNode(n, child);
            }
        }
        return n;
    }

}
