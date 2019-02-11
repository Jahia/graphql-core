package org.jahia.modules.graphql.provider.dxm.sdl.extension;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.FinderBaseDataFetcher;

public class FinderAdapter implements DataFetcher {

    private FinderBaseDataFetcher originalFinder;
    private FinderMixinInterface mixinForFinder;

    public FinderAdapter(FinderBaseDataFetcher originalFinder, FinderMixinInterface mixinForFinder) {
        this.originalFinder = originalFinder;
        this.mixinForFinder = mixinForFinder;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        Object originalFinderResult = originalFinder.get(environment);

        if (mixinForFinder == null) return originalFinderResult;

        if (originalFinderResult instanceof GqlJcrNode) {
            return mixinForFinder.resolveNode((GqlJcrNode) originalFinderResult, environment);
        }

        throw new DataFetchingException(String.format("Unsupported type in adapter: %s", originalFinderResult.getClass().toString()));
    }
}
