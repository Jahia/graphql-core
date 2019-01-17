package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;

import java.util.List;

public abstract class FinderDataFetcher implements DataFetcher {

    protected String type;
    protected Finder finder;

    public FinderDataFetcher(String type, Finder finder) {
        this.type = type.split(",")[0];
        this.finder = finder;
    }

    public FinderDataFetcher(String type) {
        this(type, null);
    }

    public abstract List<GraphQLArgument> getArguments();

    public abstract Object get(DataFetchingEnvironment environment);
}

