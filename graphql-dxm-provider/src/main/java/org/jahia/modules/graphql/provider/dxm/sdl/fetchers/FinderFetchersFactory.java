package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.apache.commons.lang.WordUtils;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.apache.commons.lang.StringUtils;

public class FinderFetchersFactory {

    public enum DefaultFetcherNames {
        all,
        ById,
        ByPath,
        Date
    }

    public enum FetcherTypes {
        ALL,
        ID,
        PATH,
        DATE,
        PROPERTY,
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
        else if(fieldDefinition.getType() instanceof GraphQLList) {//determine data type of the property from query name
            String propertyName = extractPropertyName(fieldDefinition.getName());
            GraphQLObjectType graphQLType = (GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType();

            //if it is scalar date type, apply date range fetcher
            if (graphQLType.getFieldDefinition(propertyName).getType().getName().equals("Date")) {
                return getWrappedFetcherType(graphQLType, finder, FetcherTypes.DATE);
            }
        }
        else {
            finder.setProperty(getMappedProperty(queryName, fieldDefinition));
            return getFetcherType(finder, FetcherTypes.STRING);
        }

        return getFetcherType(finder, FetcherTypes.PROPERTY);
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

    public static FinderDataFetcher getWrappedFetcherType(GraphQLType wrappedType, final Finder finder, final FetcherTypes type) {
        switch(type) {
            case DATE :
                //set property mapping to the finder
                GraphQLObjectType graphQLType = (GraphQLObjectType)wrappedType;
                GraphQLDirective directive = graphQLType.getFieldDefinition(extractPropertyName(finder.getName())).getDirective("mapping");
                if (directive!=null) {
                    finder.setProperty(directive.getArgument("property").getValue().toString());
                }
                return new DateRangeDataFetcher(finder.getType(), finder);
            default: return null;
        }
    }

    private static String extractPropertyName(String queryName){
        String propertyName = queryName.substring(queryName.lastIndexOf("By") + 2);
        return WordUtils.uncapitalize(propertyName);
    }

    public static String getMappedProperty(String queryName, GraphQLFieldDefinition fieldDefinition) {
        String afterBy = StringUtils.uncapitalize(StringUtils.substringAfterLast(queryName, "By"));
        GraphQLObjectType type = (GraphQLObjectType)((GraphQLList)fieldDefinition.getType()).getWrappedType();
        GraphQLFieldDefinition fd = type.getFieldDefinition(afterBy);
        return fd.getDirective("mapping").getArgument("property").getValue().toString();
    }
}
