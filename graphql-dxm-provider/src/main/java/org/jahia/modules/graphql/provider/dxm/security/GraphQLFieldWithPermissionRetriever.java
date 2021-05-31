package org.jahia.modules.graphql.provider.dxm.security;

import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.annotations.processor.exceptions.GraphQLAnnotationsException;
import graphql.annotations.processor.retrievers.GraphQLFieldRetriever;
import graphql.schema.GraphQLFieldDefinition;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Retrieve permission associated with field and fill security configuration
 */
public class GraphQLFieldWithPermissionRetriever extends GraphQLFieldRetriever {
    private static Logger logger = LoggerFactory.getLogger(GraphQLFieldWithPermissionRetriever.class);

    private DXGraphQLConfig dxGraphQLConfig;
    private GraphQLFieldRetriever graphQLFieldRetriever;

    public GraphQLFieldWithPermissionRetriever(DXGraphQLConfig dxGraphQLConfig, GraphQLFieldRetriever graphQLFieldRetriever) {
        this.dxGraphQLConfig = dxGraphQLConfig;
        this.graphQLFieldRetriever = graphQLFieldRetriever;
    }

    @Override
    public GraphQLFieldDefinition getField(Method method, ProcessingElementsContainer container) throws GraphQLAnnotationsException {
        GraphQLRequiresPermission ann = method.getAnnotation(GraphQLRequiresPermission.class);
        GraphQLFieldDefinition definition = graphQLFieldRetriever.getField(method, container);
        if (ann != null) {
            String key = container.getProcessing().peek() + "." + definition.getName();
            logger.debug("Adding permission : {} = {}", key, ann.value());
            dxGraphQLConfig.getPermissions().put(key, ann.value());
        }
        return definition;
    }
}
