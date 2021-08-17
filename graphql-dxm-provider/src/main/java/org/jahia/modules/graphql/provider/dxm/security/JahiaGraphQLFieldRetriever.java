package org.jahia.modules.graphql.provider.dxm.security;

import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.annotations.processor.exceptions.GraphQLAnnotationsException;
import graphql.annotations.processor.retrievers.GraphQLFieldRetriever;
import graphql.schema.AsyncDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static graphql.schema.FieldCoordinates.coordinates;
/**
 * Retrieve permission associated with field and fill security configuration
 */
public class JahiaGraphQLFieldRetriever extends GraphQLFieldRetriever {
    private static Logger logger = LoggerFactory.getLogger(JahiaGraphQLFieldRetriever.class);

    private DXGraphQLConfig dxGraphQLConfig;
    private GraphQLFieldRetriever graphQLFieldRetriever;

    private Executor executor;

    public JahiaGraphQLFieldRetriever(DXGraphQLConfig dxGraphQLConfig, GraphQLFieldRetriever graphQLFieldRetriever, Executor executor) {
        this.dxGraphQLConfig = dxGraphQLConfig;
        this.graphQLFieldRetriever = graphQLFieldRetriever;
        this.executor = executor;
    }

    @Override
    public GraphQLFieldDefinition getField(String parentName, Method method, ProcessingElementsContainer container) throws GraphQLAnnotationsException {
        // TODO: This is a hack (not related to permissions) to fix the way the OutputObjectBuilder is handling the extensions on interfaces.
        // The parentname is the name of the interface, where it should be the name of the type.
        // This result in fieldRetriever not correctly registering the dataFetcher in the codeRegistry.
        String realParentName = container.getProcessing().peek();
        GraphQLFieldDefinition definition = graphQLFieldRetriever.getField(realParentName, method, container);

        wrap(method, parentName, container, definition);

        return definition;
    }

    @Override
    public GraphQLFieldDefinition getField(String parentName, Field field, ProcessingElementsContainer container) throws GraphQLAnnotationsException {
        GraphQLFieldDefinition definition = graphQLFieldRetriever.getField(parentName, field, container);

        wrap(field, parentName, container, definition);

        return definition;
    }

    private void wrap(AnnotatedElement element, String parentName, ProcessingElementsContainer container, GraphQLFieldDefinition definition) {
        GraphQLRequiresPermission ann = element.getAnnotation(GraphQLRequiresPermission.class);
        if (ann != null) {
            String key = container.getProcessing().peek() + "." + definition.getName();
            logger.debug("Adding permission : {} = {}", key, ann.value());
            dxGraphQLConfig.getPermissions().put(key, ann.value());
        }

        GraphQLAsync async = element.getAnnotation(GraphQLAsync.class);
        if (async != null) {
            DataFetcher<?> dataFetcher = container.getCodeRegistryBuilder().getDataFetcher(coordinates(parentName, definition.getName()), definition);
            AsyncDataFetcher<?> asyncDataFetcher = AsyncDataFetcher.async(dataFetcher, executor);
            logger.debug("Creating async DataFetcher : {} {}", parentName, definition.getName());
            container.getCodeRegistryBuilder().dataFetcher(coordinates(parentName, definition.getName()), asyncDataFetcher);
        }
    }

    @Override
    public GraphQLInputObjectField getInputField(Method method, ProcessingElementsContainer container, String parentName) throws GraphQLAnnotationsException {
        return graphQLFieldRetriever.getInputField(method, container, parentName);
    }

    @Override
    public GraphQLInputObjectField getInputField(Field field, ProcessingElementsContainer container, String parentName) throws GraphQLAnnotationsException {
        return graphQLFieldRetriever.getInputField(field, container, parentName);
    }
}
