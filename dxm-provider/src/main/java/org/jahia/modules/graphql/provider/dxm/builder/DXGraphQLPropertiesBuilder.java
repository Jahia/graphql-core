package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
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
        return "property";
    }

    @Reference(target = "(graphQLType=property)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC)
    public void bindExtender(DXGraphQLExtender extender) {
        this.extenders.add(extender);
    }

    public void unbindExtender(DXGraphQLExtender extender) {
        this.extenders.remove(extender);
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
                        .build());
    }
}
