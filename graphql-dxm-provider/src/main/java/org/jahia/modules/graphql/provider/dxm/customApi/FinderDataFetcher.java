package org.jahia.modules.graphql.provider.dxm.customApi;

import graphql.schema.*;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;

public abstract class FinderDataFetcher implements DataFetcher {

    protected CustomApi type;
    protected Finder finder;

    public FinderDataFetcher(CustomApi type, Finder finder) {
        this.type = type;
        this.finder = finder;
    }

    public String getName() {
        return StringUtils.uncapitalise(type.getName()) + StringUtils.capitalise(finder.getName());
    }

    public GraphQLOutputType getObjectType() {
        if (finder.isMultiple()) {
            return new GraphQLList(type.getObjectType());
        } else {
            return type.getObjectType();
        }
    }

    public abstract List<GraphQLArgument> getArguments();

    public abstract Object get(DataFetchingEnvironment environment);
}

