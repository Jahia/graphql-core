package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNode;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLTypeExtension(DXGraphQLJCRNode.class)
public class NodetypeJCRNodeExtensions {

    @GraphQLField
    public static DXGraphQLNodeType getPrimaryNodeType(DataFetchingEnvironment env) {
        try {
            DXGraphQLJCRNode node = env.getSource();
            return new DXGraphQLNodeType(node.getNode().getPrimaryNodeType());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField()
    public static boolean getIsNodeType(DataFetchingEnvironment env, @GraphQLName("anyType") Collection<String> anyType) {
        try {
            DXGraphQLJCRNode node = env.getSource();
            for (String type : anyType) {
                if (node.getNode().isNodeType(type)) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return false;
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
