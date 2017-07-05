package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.GraphQLAnnotations;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import graphql.servlet.*;
import org.jahia.modules.graphql.provider.dxm.node.NodeMutations;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueries;
import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNodeImpl;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.nodetype.NodeExtensions;
import org.jahia.modules.graphql.provider.dxm.nodetype.NodeTypeQueries;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = GraphQLProvider.class, immediate = true)
public class DXGraphQLProvider implements GraphQLQueryProvider, GraphQLMutationProvider, GraphQLTypesProvider, GraphQLAnnotatedClassProvider {
    private static Logger logger = LoggerFactory.getLogger(GraphQLQueryProvider.class);

    private SpecializedTypesHandler specializedTypesHandler;

    @Override
    public Collection<GraphQLFieldDefinition> getMutations() {
        return Collections.emptyList();
    }

    @Override
    public Collection<GraphQLFieldDefinition> getQueries() {
        return Collections.emptyList();
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        specializedTypesHandler = new SpecializedTypesHandler();
        List<GraphQLType> types = new ArrayList<>();
        types.add(GraphQLAnnotations.getInstance().getObject(DXGraphQLJCRNodeImpl.class));
        types.addAll(SpecializedTypesHandler.getInstance().getKnownTypes().values());
        return types;
    }

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.<Class<?>>asList(NodeQueries.class, NodeTypeQueries.class, NodeExtensions.class, NodeMutations.class);
    }
}
