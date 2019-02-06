package org.jahia.modules.graphql.provider.dxm.sdl.extension;

import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

import java.util.List;

public interface FinderMixinInterface {
    FinderMixinInterface getInstance();
    List<GraphQLArgument> getArguments();
    GqlJcrNode resolveNode(GqlJcrNode gqlJcrNode);
}
