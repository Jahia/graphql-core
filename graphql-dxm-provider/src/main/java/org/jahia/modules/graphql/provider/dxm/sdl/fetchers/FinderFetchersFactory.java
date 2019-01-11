package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.apache.commons.lang.WordUtils;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRContentUtils;

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
        DATE,
        NUMBER,
        BOOLEAN,
        STRING
    }

    public static FinderDataFetcher getFetcher(GraphQLFieldDefinition fieldDefinition, String nodeType) {
        String queryName = fieldDefinition.getName();

        Finder finder = new Finder(queryName);
        finder.setType(nodeType);

        //Handle all, byId and byPath cases
        if (queryName.startsWith(DefaultFetcherNames.all.name())) {
            return getFetcherType(finder, FetcherTypes.ALL);
        }
        else if (queryName.endsWith(DefaultFetcherNames.ById.name())) {
            return getFetcherType(finder, FetcherTypes.ID);
        }
        else if (queryName.endsWith(DefaultFetcherNames.ByPath.name())) {
            return getFetcherType(finder, FetcherTypes.PATH);
        }

        //Handle specialized types
        String definitionPropertyName = getDefinitionProperty(queryName);
        String propertyNameInJcr = getMappedProperty(definitionPropertyName, fieldDefinition);
        String propertyType = getMappedType(definitionPropertyName, fieldDefinition);
        finder.setProperty(propertyNameInJcr != null ? propertyNameInJcr : definitionPropertyName);

        switch(propertyType) {
            case "Date" : return getFetcherType(finder, FetcherTypes.DATE);
            case "Boolean" : return getFetcherType(finder, FetcherTypes.BOOLEAN);
            case "Number" : return getFetcherType(finder, FetcherTypes.NUMBER);
            default : return getFetcherType(finder, FetcherTypes.STRING);
        }
    }

    public static FinderDataFetcher getFetcherType(final Finder finder, final FetcherTypes type) {
        switch(type) {
            case ALL : return new AllFinderDataFetcher(finder);
            case ID : return new ByIdFinderDataFetcher(finder);
            case PATH : return new ByPathFinderDataFetcher(finder);
            case STRING : return new StringFinderDataFetcher(finder);
            case DATE : return new DateRangeDataFetcher(finder);
            case BOOLEAN : return new BooleanFinderDataFetcher(finder);
            case NUMBER : return new NumberFinderDataFetcher(finder);
            default: return null;
        }
    }

    public static String getDefinitionProperty(String queryName) {
        return StringUtils.uncapitalize(StringUtils.substringAfterLast(queryName, "By"));
    }

    public static String getMappedProperty(String definitionPropertyName, GraphQLFieldDefinition fieldDefinition) {
        GraphQLObjectType type = (GraphQLObjectType)((GraphQLList)fieldDefinition.getType()).getWrappedType();
        GraphQLFieldDefinition fd = type.getFieldDefinition(definitionPropertyName);
        if (fd == null) return null;
        GraphQLDirective directive = fd.getDirective("mapping");
        if (directive == null) return null;
        return fd.getDirective("mapping").getArgument("property").getValue().toString();
    }

    public static String getMappedType(String definitionPropertyName, GraphQLFieldDefinition fieldDefinition) {
        GraphQLObjectType graphQLType = (GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType();
        return graphQLType.getFieldDefinition(definitionPropertyName).getType().getName();
    }
}
