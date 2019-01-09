package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLScalarType;

public class PropertiesDataFetcherFactory {

    public static DataFetcher getFetcher(GraphQLFieldDefinition graphQLFieldDefinition, Field field) {
        if (graphQLFieldDefinition.getType() instanceof GraphQLScalarType) {
            switch(graphQLFieldDefinition.getType().getName()) {
                case "String" : new StringPropertyDataFetcher(field);
            }
        }
        return new PropertiesDataFetcher(field);
    }
}
