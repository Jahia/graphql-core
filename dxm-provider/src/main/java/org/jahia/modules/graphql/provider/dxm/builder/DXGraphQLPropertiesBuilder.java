package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLObjectType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

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
    protected GraphQLObjectType.Builder build(GraphQLObjectType.Builder builder) {
        return builder
                .field(newFieldDefinition()
                        .name("key")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("value")
                        .type(GraphQLString)
                        .build());
    }
}
