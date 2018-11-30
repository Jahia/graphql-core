package org.jahia.modules.graphql.provider.dxm.customApi;

import graphql.schema.*;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;

public abstract class FinderDataFetcher implements DataFetcher {

    protected String type;
    protected Finder finder;

    public FinderDataFetcher(String type, Finder finder) {
        this.type = type;
        this.finder = finder;
    }

    public FinderDataFetcher(String type) {
        this(type, null);
    }

    public abstract List<GraphQLArgument> getArguments();

    public abstract Object get(DataFetchingEnvironment environment);
}

