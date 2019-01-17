package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;

public class PropertiesDataFetcherFactory {

    public static DataFetcher getFetcher(GraphQLFieldDefinition graphQLFieldDefinition, Field field) {
        GraphQLDirective mapping = graphQLFieldDefinition.getDirective(SDLConstants.MAPPING_DIRECTIVE);
        if (mapping != null) {
            GraphQLArgument property = mapping.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY);
            if (property != null) {
                String propertyValue = property.getValue().toString();
                if (SDLConstants.IDENTIFIER.equalsIgnoreCase(propertyValue)) {
                    return environment -> {
                        GqlJcrNode node = environment.getSource();
                        return node.getUuid();
                    };
                } else if (SDLConstants.PATH.equalsIgnoreCase(propertyValue)) {
                    return environment -> {
                        GqlJcrNode node = environment.getSource();
                        return node.getPath();
                    };
                } else if (propertyValue.startsWith(Constants.JCR_CONTENT) && propertyValue.contains(".")) {
                    return new FileContentFetcher(field, propertyValue.split("\\.")[1]);
                }
            }
        }
        return new PropertiesDataFetcher(field);
    }
}
