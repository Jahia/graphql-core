package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNode;
import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNodeImpl;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLTypeExtension(DXGraphQLJCRNode.class)
public class NodeExtensions {

    @GraphQLField
    public static DXGraphQLNodeType getPrimaryNodeType(DataFetchingEnvironment env) {
        try {
            DXGraphQLJCRNode node = env.getSource();
            return new DXGraphQLNodeType(node.getNode().getPrimaryNodeType());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    public static List<DXGraphQLNodeType> getMixinTypes(DataFetchingEnvironment env) {
        try {
            DXGraphQLJCRNode node = env.getSource();
            return Arrays.asList(node.getNode().getMixinNodeTypes()).stream().map(DXGraphQLNodeType::new).collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
