package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import graphql.schema.idl.SchemaDirectiveWiringEnvironmentImpl;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.Field;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.PropertiesDataFetcher;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.PropertiesDataFetcherFactory;

import static graphql.Scalars.GraphQLString;

public class SDLDirectiveWiring implements SchemaDirectiveWiring {

    @Override
    public GraphQLObjectType onObject(SchemaDirectiveWiringEnvironment<GraphQLObjectType> environment) {
        GraphQLDirective directive = environment.getDirective();
        if (directive != null && directive.getName().equals("description")) {
            String description = directive.getArgument("value").getValue().toString();
            GraphQLObjectType obj = environment.getElement();
            GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                    .name(obj.getName())
                    .description(description)
                    .fields(obj.getFieldDefinitions())
                    .definition(obj.getDefinition());

            for (GraphQLDirective d : obj.getDirectives()) {
                builder.withDirective(d);
            }
            for (GraphQLOutputType i : obj.getInterfaces()) {
                builder.withInterface((GraphQLInterfaceType) i);
            }

            return builder.build();
        }
        return environment.getElement();
    }

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        Field field = new Field(environment.getElement().getName());

        GraphQLFieldDefinition def = environment.getElement();

        //TODO add arguments from factory as well based on type
        GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition()
                .name(def.getName())
                .dataFetcher(PropertiesDataFetcherFactory.getFetcher(def, field))
                .argument(GraphQLArgument.newArgument().name("language").type(GraphQLString).build())
                .withDirectives(def.getDirectives().toArray(new GraphQLDirective[0]))
                .type(def.getType());

        GraphQLDirective directive = environment.getDirective();
        if (directive != null && directive.getName().equals("mapping")) {
            field.setProperty(environment.getDirective().getArgument("property").getValue().toString());
        }
        else if (directive != null && directive.getName().equals("description")) {
            builder.description(environment.getDirective().getArgument("value").getValue().toString());
        }
        return builder.build();
    }
}
