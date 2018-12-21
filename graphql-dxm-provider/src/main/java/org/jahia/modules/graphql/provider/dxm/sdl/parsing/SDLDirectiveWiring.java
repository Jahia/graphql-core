package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.Directive;
import graphql.language.NodeParentTree;
import graphql.language.ObjectTypeDefinition;
import graphql.language.StringValue;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import graphql.schema.idl.SchemaDirectiveWiringEnvironmentImpl;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.Field;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.PropertiesDataFetcher;

import static graphql.Scalars.GraphQLString;

public class SDLDirectiveWiring implements SchemaDirectiveWiring {

    @Override
    public GraphQLObjectType onObject(SchemaDirectiveWiringEnvironment<GraphQLObjectType> environment) {
        return environment.getElement();
    }

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        //Todo add error/syntax check
        ObjectTypeDefinition parentType = (ObjectTypeDefinition) ((NodeParentTree) ((SchemaDirectiveWiringEnvironmentImpl) environment).getNodeParentTree().getParentInfo().get()).getNode();
        Directive parentTypeDirective = parentType.getDirective("mapping");
        String nodeType = ((StringValue) parentTypeDirective.getArgument("node").getValue()).getValue();
        Field field = new Field(environment.getElement().getName());
        field.setProperty(environment.getDirective().getArgument("property").getValue().toString());
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(environment.getElement().getName())
                .dataFetcher(new PropertiesDataFetcher(field))
                .argument(GraphQLArgument.newArgument().name("language").type(GraphQLString).build())
                .type(GraphQLString)
                .build();
    }
}
