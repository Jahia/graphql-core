package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.GraphQLFieldDefinition;

public class FetchersFactory {

    public enum FetcherTypes {
        ALL,
        ID,
        PATH,
        PROPERTY
    }

    public static FinderDataFetcher getFetcher(GraphQLFieldDefinition fieldDefinition, String nodeType) {
        String queryName = fieldDefinition.getName();
        if (queryName.startsWith("all")) {
            return getFetcherType(fieldDefinition, nodeType, FetcherTypes.ALL);
        }
        else if (queryName.endsWith("ById")) {
            return getFetcherType(fieldDefinition, nodeType, FetcherTypes.ID);
        }
        else if (queryName.endsWith("ByPath")) {
            return getFetcherType(fieldDefinition, nodeType, FetcherTypes.PATH);
        }
        else {
            return getFetcherType(fieldDefinition, nodeType, FetcherTypes.PROPERTY);
        }
    }

    public static FinderDataFetcher getFetcherType(GraphQLFieldDefinition fieldDefinition, String nodeType, FetcherTypes type) {
        switch(type) {
            case ALL : return new AllFinderDataFetcher(nodeType);
            case ID :
            case PATH :
            case PROPERTY : //Extract property from fielDefinition name and return fetcher
            default: return null;
        }
    }
}
