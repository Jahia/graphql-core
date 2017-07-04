package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLTypeExtension;
import graphql.servlet.GraphQLQuery;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

@GraphQLTypeExtension(GraphQLQuery.class)
public class BaseQueries {

    @GraphQLField
    public static DXGraphQLJCRNode getNodeById(@GraphQLName("uuid") String uuid,
                                               @GraphQLName("asMixin") String asMixin,
                                               @GraphQLName("workspace") String workspace) {
        try {
            return new DXGraphQLGenericJCRNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNodeByIdentifier(uuid), asMixin);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    public static DXGraphQLJCRNode getNodeByPath(@GraphQLName("path") String path,
                                                 @GraphQLName("asMixin") String asMixin,
                                                 @GraphQLName("workspace") String workspace) {
        try {
            return new DXGraphQLGenericJCRNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(path), asMixin);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    public static DXGraphQLNodeType getNodeTypesByName(@GraphQLName("name") String name) {
        try {
            return new DXGraphQLNodeType(NodeTypeRegistry.getInstance().getNodeType(name));
        } catch (NoSuchNodeTypeException e) {
            throw new RuntimeException(e);
        }
    }

}
