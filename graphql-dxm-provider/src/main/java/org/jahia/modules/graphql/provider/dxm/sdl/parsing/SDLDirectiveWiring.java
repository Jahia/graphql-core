package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.Field;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.PropertiesDataFetcherFactory;

public class SDLDirectiveWiring implements SchemaDirectiveWiring {

    @Override
    public GraphQLObjectType onObject(SchemaDirectiveWiringEnvironment<GraphQLObjectType> environment) {
        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject(environment.getElement());

        GraphQLDirective directive = environment.getDirective();
        if (directive != null && directive.getName().equals("description")) {
            builder.description(environment.getDirective().getArgument("value").getValue().toString());
        }
        return builder.build();
    }

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {

        GraphQLFieldDefinition def = environment.getElement();

        //TODO add arguments from factory as well based on type
        GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition(def);

        GraphQLDirective directive = environment.getDirective();
        if (directive != null && directive.getName().equals("mapping")) {
            Field field = new Field(environment.getElement().getName());
            field.setProperty(environment.getDirective().getArgument("property").getValue().toString());
            builder.dataFetcher(PropertiesDataFetcherFactory.getFetcher(def, field));
        } else if (directive != null && directive.getName().equals("description")) {
            builder.description(environment.getDirective().getArgument("value").getValue().toString());
        }
        return builder.build();
    }
}
