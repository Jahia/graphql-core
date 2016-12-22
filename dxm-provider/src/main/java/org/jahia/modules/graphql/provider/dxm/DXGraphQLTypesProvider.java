package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.GraphQLType;
import graphql.servlet.GraphQLTypesProvider;
import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLNodeBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component(service = GraphQLTypesProvider.class, immediate = true)
public class DXGraphQLTypesProvider implements GraphQLTypesProvider {
    private DXGraphQLNodeBuilder nodeBuilder;

    @Reference
    public void setNodeBuilder(DXGraphQLNodeBuilder nodeBuilder) {
        this.nodeBuilder = nodeBuilder;
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        List<GraphQLType> types = new ArrayList<>();
        types.addAll(nodeBuilder.getKnownTypes().values());
        return types;
    }
}
