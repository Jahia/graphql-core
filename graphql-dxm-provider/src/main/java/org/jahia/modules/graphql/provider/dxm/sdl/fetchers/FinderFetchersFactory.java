package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.language.FieldDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.apache.commons.lang.StringUtils;

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
            finder.setProperty(getMappedProperty(queryName, fieldDefinition));
            return getFetcherType(finder, FetcherTypes.STRING);
        }
    }

    public static FinderDataFetcher getFetcherType(final Finder finder, final FetcherTypes type) {
        switch(type) {
            case ALL : return new AllFinderDataFetcher(finder);
            case ID : return new ByIdFinderDataFetcher(finder);
            case PATH : return new ByPathFinderDataFetcher(finder);
            case STRING : return new StringFinderDataFetcher(finder);
            default: return null;
        }
    }

    public static String getMappedProperty(String queryName, GraphQLFieldDefinition fieldDefinition) {
        String afterBy = StringUtils.uncapitalize(StringUtils.substringAfterLast(queryName, "By"));
        GraphQLObjectType type = (GraphQLObjectType)((GraphQLList)fieldDefinition.getType()).getWrappedType();
        GraphQLFieldDefinition fd = type.getFieldDefinition(afterBy);
        return fd.getDirective("mapping").getArgument("property").getValue().toString();
    }
}
