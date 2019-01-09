package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.GraphQLFieldDefinition;

public class FinderFetchersFactory {

    public enum DefaultFetcherNames {
        all,
        ById,
        ByPath
    }

    public enum FetcherTypes {
        ALL,
        ID,
        PATH,
        STRING
    }

    public static FinderDataFetcher getFetcher(GraphQLFieldDefinition fieldDefinition, String nodeType) {
        String queryName = fieldDefinition.getName();
        Finder finder = new Finder(queryName);
        finder.setType(nodeType);
        if (queryName.startsWith(DefaultFetcherNames.all.name())) {
            return getFetcherType(finder, FetcherTypes.ALL);
        }
        else if (queryName.endsWith(DefaultFetcherNames.ById.name())) {
            return getFetcherType(finder, FetcherTypes.ID);
        }
        else if (queryName.endsWith(DefaultFetcherNames.ByPath.name())) {
            return getFetcherType(finder, FetcherTypes.PATH);
        }
        else {
            finder.setProperty(getMappedProperty(fieldDefinition));
            return getFetcherType(finder, FetcherTypes.STRING);
        }
    }

    public static FinderDataFetcher getFetcherType(final Finder finder, final FetcherTypes type) {
        switch(type) {
            case ALL : return new AllFinderDataFetcher(finder);
            case ID : return new ByIdFinderDataFetcher(finder);
            case PATH : return new ByPathFinderDataFetcher(finder);
            case STRING : new StringFinderDataFetcher(finder);
            default: return null;
        }
    }

    public static String getMappedProperty(GraphQLFieldDefinition fieldDefinition) {
        return fieldDefinition.getDirective("mapping").getArgument("property").getValue().toString();
    }
}
