package org.jahia.modules.graphql.provider.dxm.locking;


import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

@GraphQLTypeExtension(GqlJcrNode.class)
public class LockJCRNodeQueryExtension {

    private GqlJcrNode gqlJcrNode;

    public LockJCRNodeQueryExtension(GqlJcrNode gqlJcrNode) {
        this.gqlJcrNode = gqlJcrNode;
    }

    @GraphQLField
    @GraphQLDescription("Retrieve lock info of the current node")
    public GqlLockInfo getLockInfo() {
        return new GqlLockInfo(gqlJcrNode.getNode());
    }
}
