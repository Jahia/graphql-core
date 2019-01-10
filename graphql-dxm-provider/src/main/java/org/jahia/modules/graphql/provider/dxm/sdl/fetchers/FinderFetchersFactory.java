package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.apache.commons.lang.WordUtils;

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
        PROPERTY
    }

    public static FinderDataFetcher getFetcher(GraphQLFieldDefinition fieldDefinition, String nodeType) {
        String queryName = fieldDefinition.getName();

        if (queryName.startsWith(DefaultFetcherNames.all.name())) {
            return getFetcherType(fieldDefinition, nodeType, FetcherTypes.ALL);
        }
        else if (queryName.endsWith(DefaultFetcherNames.ById.name())) {
            return getFetcherType(fieldDefinition, nodeType, FetcherTypes.ID);
        }
        else if (queryName.endsWith(DefaultFetcherNames.ByPath.name())) {
            return getFetcherType(fieldDefinition, nodeType, FetcherTypes.PATH);
        }
        else if(fieldDefinition.getType() instanceof GraphQLList){//determine data type of the property from query name
            String propertyName = extractPropertyName(fieldDefinition.getName());
            GraphQLObjectType graphQLType = (GraphQLObjectType)((GraphQLList) fieldDefinition.getType()).getWrappedType();

            //if it is scalar date type, apply date range fetcher
            if(graphQLType.getFieldDefinition(propertyName).getType().getName().equals("Date")){
                return getFetcherType(fieldDefinition, nodeType, FetcherTypes.DATE);
            }
        }

        return getFetcherType(fieldDefinition, nodeType, FetcherTypes.PROPERTY);
    }

    public static FinderDataFetcher getFetcherType(final GraphQLFieldDefinition fieldDefinition, final String nodeType, final FetcherTypes type) {
        switch(type) {
            case ALL : return new AllFinderDataFetcher(nodeType);
            case ID : return new ByIdFinderDataFetcher(nodeType);
            case PATH : return new ByPathFinderDataFetcher(nodeType);
            case DATE :
                if(fieldDefinition==null) return null;
                Finder finder = new Finder(fieldDefinition.getName());
                //set property mapping to the finder
                GraphQLObjectType graphQLType = (GraphQLObjectType)((GraphQLList) fieldDefinition.getType()).getWrappedType();
                GraphQLDirective directive = graphQLType.getFieldDefinition(extractPropertyName(fieldDefinition.getName())).getDirective("mapping");
                if (directive!=null) {
                    finder.setProperty(directive.getArgument("property").getValue().toString());
                }
                return new DateRangeDataFetcher(nodeType, finder);
            case PROPERTY : //Extract property from field Definition name and return fetcher
            default: return null;
        }
    }

    private static String extractPropertyName(String queryName){
        String propertyName = queryName.substring(queryName.lastIndexOf("By") + 2);
        return WordUtils.uncapitalize(propertyName);
    }

}
