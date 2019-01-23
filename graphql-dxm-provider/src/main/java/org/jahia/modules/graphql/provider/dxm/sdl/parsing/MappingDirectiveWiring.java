package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.Field;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.PropertiesDataFetcherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingDirectiveWiring implements SchemaDirectiveWiring {

    private static Logger logger = LoggerFactory.getLogger(MappingDirectiveWiring.class);

    @Override
    public GraphQLObjectType onObject(SchemaDirectiveWiringEnvironment<GraphQLObjectType> environment) {
        // Mapping on object definition -> do nothing
        return environment.getElement();
    }

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        // Mapping on field

        GraphQLFieldDefinition def = environment.getElement();

        //TODO add arguments from factory as well based on type

        GraphQLDirective directive = environment.getDirective();

        Field field = new Field(def.getName());
        field.setProperty(directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY).getValue().toString());
        field.setType(def.getType().getName());

        logger.debug("field name {} ", field.getName());
        logger.debug("field type {} ", field.getType());

        return GraphQLFieldDefinition.newFieldDefinition(def)
                .dataFetcher(PropertiesDataFetcherFactory.getFetcher(def, field))
                .build();
    }
}
