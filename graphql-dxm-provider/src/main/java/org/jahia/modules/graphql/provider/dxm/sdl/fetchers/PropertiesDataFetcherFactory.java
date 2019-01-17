package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import org.jahia.api.Constants;

public class PropertiesDataFetcherFactory {

    public static DataFetcher getFetcher(GraphQLFieldDefinition graphQLFieldDefinition, Field field) {
        GraphQLDirective mapping = graphQLFieldDefinition.getDirective("mapping");
        if (mapping != null) {
            GraphQLArgument property = mapping.getArgument("property");
            if (property != null) {
                String propertyValue = property.getValue().toString();
                if (propertyValue.startsWith(Constants.JCR_CONTENT) && propertyValue.contains(".")) {
                    return new FileContentFetcher(field, propertyValue.split("\\.")[1]);
                }
            }
        }
        return new PropertiesDataFetcher(field);
    }
}
