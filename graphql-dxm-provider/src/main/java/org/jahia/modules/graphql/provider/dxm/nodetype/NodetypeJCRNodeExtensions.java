package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLTypeExtension(GqlJcrNode.class)
public class NodetypeJCRNodeExtensions {

    private GqlJcrNode node;

    public NodetypeJCRNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    @GraphQLField
    public GqlJcrNodeType getPrimaryNodeType() {
        try {
            return new GqlJcrNodeType(node.getNode().getPrimaryNodeType());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField()
    public boolean getIsNodeType(@GraphQLName("anyType") Collection<String> anyType) {
        try {
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
    public List<GqlJcrNodeType> getMixinTypes() {
        try {
            return Arrays.asList(node.getNode().getMixinNodeTypes()).stream().map(GqlJcrNodeType::new).collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
