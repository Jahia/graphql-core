package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLNonNull;
import graphql.annotations.GraphQLTypeExtension;
import graphql.servlet.GraphQLQuery;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;

import javax.jcr.nodetype.NoSuchNodeTypeException;

@GraphQLTypeExtension(GraphQLQuery.class)
public class NodeTypeQueryExtensions {

    @GraphQLField
    public static DXGraphQLNodeType getNodeTypesByName(@GraphQLNonNull @GraphQLName("name") String name) {
        try {
            return new DXGraphQLNodeType(NodeTypeRegistry.getInstance().getNodeType(name));
        } catch (NoSuchNodeTypeException e) {
            throw new RuntimeException(e);
        }
    }

}
