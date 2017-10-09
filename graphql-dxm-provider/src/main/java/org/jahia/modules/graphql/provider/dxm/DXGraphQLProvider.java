package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.GraphQLAnnotationsProcessor;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import graphql.servlet.*;
import org.jahia.modules.graphql.provider.dxm.node.NodeMutationExtensions;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.nodetype.NodetypeJCRNodeExtensions;
import org.jahia.modules.graphql.provider.dxm.nodetype.NodeTypeQueryExtensions;
import org.jahia.modules.graphql.provider.dxm.render.RenderNodeExtensions;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = GraphQLProvider.class, immediate = true)
public class DXGraphQLProvider implements GraphQLTypesProvider, GraphQLAnnotatedClassProvider {
    private static Logger logger = LoggerFactory.getLogger(GraphQLQueryProvider.class);

    private SpecializedTypesHandler specializedTypesHandler;

    private GraphQLAnnotationsProcessor graphQLAnnotationsProcessor;

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setGraphQLAnnotationsProcessor(GraphQLAnnotationsProcessor graphQLAnnotationsProcessor) {
        this.graphQLAnnotationsProcessor = graphQLAnnotationsProcessor;
        this.specializedTypesHandler = new SpecializedTypesHandler(graphQLAnnotationsProcessor);
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        specializedTypesHandler.initializeTypes();

        List<GraphQLType> types = new ArrayList<>();
        types.add(graphQLAnnotationsProcessor.getOutputType(GqlJcrNodeImpl.class));
        types.addAll(specializedTypesHandler.getKnownTypes().values());
        return types;
    }

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.<Class<?>>asList(
                NodeQueryExtensions.class,
                NodeTypeQueryExtensions.class,
                NodetypeJCRNodeExtensions.class,
                RenderNodeExtensions.class,
                NodeMutationExtensions.class);
    }
}
