package org.jahia.modules.graphql.provider.dxm.sdl.extension;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.FinderBaseDataFetcher;

import java.util.List;

public class FinderAdapter implements DataFetcher {

    private FinderBaseDataFetcher originalFinder;
    private List<FinderMixinInterface> applicableFinderMixins;

    public FinderAdapter(FinderBaseDataFetcher originalFinder, List<FinderMixinInterface> applicableFinderMixins) {
        this.originalFinder = originalFinder;
        this.applicableFinderMixins = applicableFinderMixins;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        if (originalFinder == null) {
            return null;
        }

        Object originalFinderResult = originalFinder.get(environment);

        for (FinderMixinInterface mixin : applicableFinderMixins) {
            originalFinderResult = mixin.resolveNode((GqlJcrNode) originalFinderResult, environment);
            if (originalFinderResult == null) {
                return null;
            }
        }

        return originalFinderResult;
    }
}
