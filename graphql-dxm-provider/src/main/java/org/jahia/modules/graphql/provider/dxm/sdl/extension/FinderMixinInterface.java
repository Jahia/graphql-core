package org.jahia.modules.graphql.provider.dxm.sdl.extension;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.Finder;

import java.util.List;

public interface FinderMixinInterface {
    default boolean applyOnFinder(Finder finder) {
        return false;
    }

    List<GraphQLArgument> getArguments();

    GqlJcrNode resolveNode(GqlJcrNode gqlJcrNode, DataFetchingEnvironment environment);
}
