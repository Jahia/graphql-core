package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.processor.GraphQLAnnotationsComponent;
import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.annotations.processor.retrievers.GraphQLExtensionsHandler;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.servlet.GraphQLMutationProvider;
import graphql.servlet.GraphQLProvider;
import graphql.servlet.GraphQLQueryProvider;
import graphql.servlet.GraphQLTypesProvider;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.node.NodeMutationExtensions;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.nodetype.NodeTypeQueryExtensions;
import org.jahia.modules.graphql.provider.dxm.nodetype.NodetypeJCRNodeExtensions;
import org.jahia.modules.graphql.provider.dxm.render.RenderNodeExtensions;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = GraphQLProvider.class, immediate = true)
public class DXGraphQLProvider implements GraphQLTypesProvider, GraphQLQueryProvider, GraphQLMutationProvider, DXGraphQLExtensionsProvider {
    private static Logger logger = LoggerFactory.getLogger(GraphQLQueryProvider.class);

    private SpecializedTypesHandler specializedTypesHandler;

    private GraphQLAnnotationsComponent graphQLAnnotations;

    private ProcessingElementsContainer container;

    private Collection<DXGraphQLExtensionsProvider> extensionsProviders = new HashSet<>();

    private GraphQLObjectType queryType;
    private GraphQLObjectType mutationType;

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setGraphQLAnnotations(GraphQLAnnotationsComponent graphQLAnnotations) {
        this.graphQLAnnotations = graphQLAnnotations;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    public void addExtensionProvider(DXGraphQLExtensionsProvider provider) {
        this.extensionsProviders.add(provider);
    }

    public void removeExtensionProvider(DXGraphQLExtensionsProvider provider) {
        this.extensionsProviders.remove(provider);
    }

    @Activate
    public void activate() {
        container = graphQLAnnotations.createContainer();

        GraphQLExtensionsHandler extensionsHandler = graphQLAnnotations.getExtensionsHandler();

        extensionsProviders.add(this);

        for (DXGraphQLExtensionsProvider extensionsProvider : extensionsProviders) {
            for (Class<?> aClass : extensionsProvider.getExtensions()) {
                extensionsHandler.registerTypeExtension(aClass, container);
            }
        }

        queryType = (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputType(Query.class, container);
        mutationType = (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputType(Mutation.class, container);

        specializedTypesHandler = new SpecializedTypesHandler(graphQLAnnotations, container);
        specializedTypesHandler.initializeTypes();
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        List<GraphQLType> types = new ArrayList<>();

        types.add(graphQLAnnotations.getOutputTypeProcessor().getOutputType(GqlJcrNodeImpl.class, container));
        types.addAll(specializedTypesHandler.getKnownTypes().values());
        return types;
    }

    @Override
    public Collection<GraphQLFieldDefinition> getQueries() {
        return queryType.getFieldDefinitions();
    }

    @Override
    public Collection<GraphQLFieldDefinition> getMutations() {
        return mutationType.getFieldDefinitions();
    }


    @GraphQLName("Query")
    public static class Query {
    }

    @GraphQLName("Mutation")
    public static class Mutation {
    }

    public Collection<Class<?>> getExtensions() {
        return Arrays.<Class<?>>asList(
                NodeQueryExtensions.class,
                NodeTypeQueryExtensions.class,
                NodetypeJCRNodeExtensions.class,
                RenderNodeExtensions.class,
                NodeMutationExtensions.class);
    }
}
