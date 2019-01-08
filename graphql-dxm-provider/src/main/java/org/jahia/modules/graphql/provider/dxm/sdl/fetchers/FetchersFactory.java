package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.GraphQLFieldDefinition;

public class FetchersFactory {

    public enum DefaultFetcherNames {
        all,
        ById,
        ByPath
    }

    public enum FetcherTypes {
        ALL,
        ID,
        PATH,
        PROPERTY
    }

    public static FinderDataFetcher getFetcher(GraphQLFieldDefinition fieldDefinition, String nodeType) {
        String queryName = fieldDefinition.getName();
        if (queryName.startsWith(DefaultFetcherNames.all.name())) {
            return getFetcherType(nodeType, FetcherTypes.ALL);
        }
        else if (queryName.endsWith(DefaultFetcherNames.ById.name())) {
            return getFetcherType(nodeType, FetcherTypes.ID);
        }
        else if (queryName.endsWith(DefaultFetcherNames.ByPath.name())) {
            return getFetcherType(nodeType, FetcherTypes.PATH);
        }
        else {
            return getFetcherType(nodeType, FetcherTypes.PROPERTY);
        }
    }

    public static FinderDataFetcher getFetcherType(final String nodeType, final FetcherTypes type) {
        switch(type) {
            case ALL : return new AllFinderDataFetcher(nodeType);
            case ID : return new ByIdFinderDataFetcher(nodeType, null);
            case PATH : return new ByPathFinderDataFetcher(nodeType, null);
            case PROPERTY : //Extract property from fielDefinition name and return fetcher
            default: return null;
        }
    }
}
