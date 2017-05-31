package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.GraphQLType;
import graphql.servlet.GraphQLTypesProvider;
import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLNodeBuilder;
import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLNodeTypeBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component(service = GraphQLTypesProvider.class)
public class DXGraphQLTypesProvider implements GraphQLTypesProvider {
    private DXGraphQLNodeBuilder nodeBuilder;
    private DXGraphQLNodeTypeBuilder nodeTypeBuilder;

    @Reference
    public void setNodeBuilder(DXGraphQLNodeBuilder nodeBuilder) {
        this.nodeBuilder = nodeBuilder;
    }

    @Reference
    public void setTypeNodeBuilder(DXGraphQLNodeTypeBuilder nodeTypeBuilder) {
        this.nodeTypeBuilder = nodeTypeBuilder;
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        List<GraphQLType> types = new ArrayList<>();
        types.add(nodeBuilder.getGenericType());
        types.addAll(nodeBuilder.getKnownTypes().values());
        types.add(nodeBuilder.getListType());
        types.add(nodeTypeBuilder.getListType());
        return types;
    }
}
