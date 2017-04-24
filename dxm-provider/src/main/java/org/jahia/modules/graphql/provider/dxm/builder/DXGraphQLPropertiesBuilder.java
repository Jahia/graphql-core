package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.Arrays;
import java.util.List;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;


@Component(service = DXGraphQLPropertiesBuilder.class)
public class DXGraphQLPropertiesBuilder extends DXGraphQLBuilder {
    @Override
    public String getName() {
        return "Property";
    }

    @Override
    protected List<GraphQLFieldDefinition> getFields() {
        return Arrays.asList(
                newFieldDefinition()
                        .name("key")
                        .type(GraphQLString)
                        .build(),
                newFieldDefinition()
                        .name("value")
                        .type(GraphQLString)
                        .build(),
                newFieldDefinition()
                        .name("values")
                        .type(new GraphQLList(GraphQLString))
                        .build());
    }
}
