package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;

public class PropertiesDataFetcherFactory {

    private PropertiesDataFetcherFactory(){
        //void
    }

    public static DataFetcher getFetcher(GraphQLFieldDefinition graphQLFieldDefinition, Field field) {
        GraphQLDirective mapping = graphQLFieldDefinition.getDirective(SDLConstants.MAPPING_DIRECTIVE);
        GraphQLArgument property = mapping != null ? mapping.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY) : null;
        String propertyValue = property != null ? property.getValue().toString() : null;

        if (SDLConstants.IDENTIFIER.equalsIgnoreCase(propertyValue)) {
            return environment -> {
                GqlJcrNode node = environment.getSource();
                return node.getUuid();
            };
        } else
            if (SDLConstants.PATH.equalsIgnoreCase(propertyValue)) {
                return environment -> {
                    GqlJcrNode node = environment.getSource();
                    return node.getPath();
                };
        } else
            if (propertyValue != null && propertyValue.startsWith(Constants.JCR_CONTENT) && propertyValue.contains(".")) {
                return new FileContentFetcher(field, propertyValue.split("\\.")[1]);
        } else
            if (graphQLFieldDefinition.getType() instanceof GraphQLObjectType) {
                return new ObjectDataFetcher(field);
        } else
            if (graphQLFieldDefinition.getType() instanceof GraphQLList) {
                return new ListDataFetcher(field);
        }
        return new PropertiesDataFetcher(field);
    }

}
