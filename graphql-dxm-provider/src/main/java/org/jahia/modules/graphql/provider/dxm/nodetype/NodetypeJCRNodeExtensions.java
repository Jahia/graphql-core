package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLTypeExtension(GqlJcrNode.class)
public class NodetypeJCRNodeExtensions {

    @GraphQLField
    public static GqlJcrNodeType getPrimaryNodeType(DataFetchingEnvironment env) {
        try {
            GqlJcrNode node = env.getSource();
            return new GqlJcrNodeType(node.getNode().getPrimaryNodeType());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField()
    public static boolean getIsNodeType(DataFetchingEnvironment env, @GraphQLName("anyType") Collection<String> anyType) {
        try {
            GqlJcrNode node = env.getSource();
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
    public static List<GqlJcrNodeType> getMixinTypes(DataFetchingEnvironment env) {
        try {
            GqlJcrNode node = env.getSource();
            return Arrays.asList(node.getNode().getMixinNodeTypes()).stream().map(GqlJcrNodeType::new).collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
