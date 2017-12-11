package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;

import javax.jcr.nodetype.NoSuchNodeTypeException;

@GraphQLTypeExtension(GqlJcrQuery.class)
public class NodeTypeJCRQueryExtensions {

    private GqlJcrQuery source;

    public NodeTypeJCRQueryExtensions(GqlJcrQuery source) {
        this.source = source;
    }

    @GraphQLField
    public static GqlJcrNodeType getNodeTypeByName(@GraphQLNonNull @GraphQLName("name") String name) {
        try {
            return new GqlJcrNodeType(NodeTypeRegistry.getInstance().getNodeType(name));
        } catch (NoSuchNodeTypeException e) {
            throw new RuntimeException(e);
        }
    }

}
