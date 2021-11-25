/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.annotations.processor.GraphQLAnnotationsComponent;
import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.annotations.processor.retrievers.*;
import graphql.annotations.processor.searchAlgorithms.SearchAlgorithm;
import graphql.annotations.processor.typeFunctions.TypeFunction;
import graphql.kickstart.servlet.osgi.*;
import graphql.schema.*;
import org.jahia.bin.filters.jcr.JcrSessionFilter;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.node.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXConnection;
import org.jahia.modules.graphql.provider.dxm.relay.DXFieldAggregation;
import org.jahia.modules.graphql.provider.dxm.relay.DXRelay;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.SDLSchemaService;
import org.jahia.modules.graphql.provider.dxm.security.JahiaGraphQLFieldRetriever;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.securityfilter.PermissionService;
import org.jahia.services.securityfilter.ScopeDefinition;
import org.jahia.services.usermanager.JahiaUser;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Component(service = GraphQLProvider.class, immediate = true)
public class DXGraphQLProvider implements GraphQLTypesProvider, GraphQLQueryProvider, GraphQLMutationProvider, GraphQLCodeRegistryProvider, DXGraphQLExtensionsProvider, GraphQLSubscriptionProvider {
    private static Logger logger = LoggerFactory.getLogger(DXGraphQLProvider.class);

    private static DXGraphQLProvider instance;

    private SpecializedTypesHandler specializedTypesHandler;

    private GraphQLAnnotationsComponent graphQLAnnotations;
    private GraphQLTypeRetriever graphQLTypeRetriever;
    private GraphQLObjectInfoRetriever graphQLObjectInfoRetriever;
    private GraphQLInterfaceRetriever graphQLInterfaceRetriever;
    private GraphQLFieldRetriever graphQLFieldRetriever;
    private SearchAlgorithm fieldSearchAlgorithm;
    private SearchAlgorithm methodSearchAlgorithm;
    private GraphQLExtensionsHandler extensionsHandler;
    private DXGraphQLConfig dxGraphQLConfig;

    private ProcessingElementsContainer container;

    private static Map<String, URL> sdlResources = new ConcurrentHashMap<>();

    private Collection<DXGraphQLExtensionsProvider> extensionsProviders = new HashSet<>();


    private GraphQLObjectType queryType;
    private GraphQLObjectType mutationType;
    private GraphQLObjectType subscriptionType;
    private GraphQLCodeRegistry codeRegistry;

    private DXRelay relay;

    private Map<String, Class<? extends DXConnection<?>>> connectionTypes = new HashMap<>();
    private SDLSchemaService sdlSchemaService;

    private PermissionService permissionService;

    private Executor executor;
    private ExecutorService pool;

    public static DXGraphQLProvider getInstance() {
        return instance;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setGraphQLAnnotations(GraphQLAnnotationsComponent graphQLAnnotations) {
        this.graphQLAnnotations = graphQLAnnotations;
    }

    public GraphQLAnnotationsComponent getGraphQLAnnotations() {
        return graphQLAnnotations;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setGraphQLTypeRetriever(GraphQLTypeRetriever graphQLTypeRetriever) {
        this.graphQLTypeRetriever = graphQLTypeRetriever;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setGraphQLObjectInfoRetriever(GraphQLObjectInfoRetriever graphQLObjectInfoRetriever) {
        this.graphQLObjectInfoRetriever = graphQLObjectInfoRetriever;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setGraphQLInterfaceRetriever(GraphQLInterfaceRetriever graphQLInterfaceRetriever) {
        this.graphQLInterfaceRetriever = graphQLInterfaceRetriever;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setGraphQLFieldRetriever(GraphQLFieldRetriever graphQLFieldRetriever) {
        this.graphQLFieldRetriever = graphQLFieldRetriever;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, target = "(type=field)", policyOption = ReferencePolicyOption.GREEDY)
    public void setFieldSearchAlgorithm(SearchAlgorithm fieldSearchAlgorithm) {
        this.fieldSearchAlgorithm = fieldSearchAlgorithm;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, target = "(type=method)", policyOption = ReferencePolicyOption.GREEDY)
    public void setMethodSearchAlgorithm(SearchAlgorithm methodSearchAlgorithm) {
        this.methodSearchAlgorithm = methodSearchAlgorithm;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setExtensionsHandler(GraphQLExtensionsHandler extensionsHandler) {
        this.extensionsHandler = extensionsHandler;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setSDLRegistrationService(SDLSchemaService sdlSchemaService) {
        this.sdlSchemaService = sdlSchemaService;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public ProcessingElementsContainer getContainer() {
        return container;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    public void addExtensionProvider(DXGraphQLExtensionsProvider provider) {
        logger.debug("Adding extension : {}", provider.toString());
        this.extensionsProviders.add(provider);
        registerSchema();
    }

    public void removeExtensionProvider(DXGraphQLExtensionsProvider provider) {
        logger.debug("Removing extension : {}", provider.toString());
        this.extensionsProviders.remove(provider);
        registerSchema();
    }

    @Reference
    public void setDxGraphQLConfig(DXGraphQLConfig dxGraphQLConfig) {
        this.dxGraphQLConfig = dxGraphQLConfig;
    }

    @Activate
    public void activate() {
        if (logger.isDebugEnabled()) {
            logger.debug("Activating GraphQL API schema with extensions {}", extensionsProviders.stream().map(dxGraphQLExtensionsProvider -> dxGraphQLExtensionsProvider.getClass().getSimpleName()).collect(Collectors.joining(",")));
        }
        instance = this;

        // Initialize thread pool
        pool = new ForkJoinPool(50);
        executor = command -> {
            JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser();
            Collection<ScopeDefinition> scopes = permissionService.getCurrentScopes();
            pool.execute(() -> {
                JCRSessionFactory.getInstance().setCurrentUser(user);
                permissionService.setCurrentScopes(scopes);
                try {
                    command.run();
                } finally {
                    JcrSessionFilter.endRequest();
                    permissionService.resetScopes();
                }
            });
        };

        JahiaGraphQLFieldRetriever graphQLFieldWithPermissionsRetriever = new JahiaGraphQLFieldRetriever(dxGraphQLConfig, graphQLFieldRetriever, executor);

        graphQLTypeRetriever.setGraphQLObjectInfoRetriever(graphQLObjectInfoRetriever);
        graphQLTypeRetriever.setGraphQLInterfaceRetriever(graphQLInterfaceRetriever);
        graphQLTypeRetriever.setGraphQLFieldRetriever(graphQLFieldWithPermissionsRetriever);
        graphQLTypeRetriever.setFieldSearchAlgorithm(fieldSearchAlgorithm);
        graphQLTypeRetriever.setMethodSearchAlgorithm(methodSearchAlgorithm);
        graphQLTypeRetriever.setExtensionsHandler(extensionsHandler);

        graphQLFieldRetriever.setAlwaysPrettify(true);
        container = graphQLAnnotations.createContainer();

        specializedTypesHandler = new SpecializedTypesHandler(graphQLAnnotations, container);

        ((UnboxingTypeFunction) unboxingTypeFunction).setDefaultTypeFunction(defaultTypeFunction);

        relay = new DXRelay();
        sdlSchemaService.setRelay(relay);
        container.setRelay(relay);

        connectionTypes.put("JCRNodeConnection", GqlJcrNodeConnection.class);

        for (Map.Entry<String, Class<? extends DXConnection<?>>> entry : connectionTypes.entrySet()) {
            relay.addConnectionType(entry.getKey(), (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(entry.getValue(), container));
        }

        extensionsProviders.add(this);

        registerSchema();
    }

    private void registerSchema() {
        if (instance != null && graphQLAnnotations != null && graphQLTypeRetriever != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering GraphQL API schema with extensions {}", extensionsProviders.stream().map(dxGraphQLExtensionsProvider -> dxGraphQLExtensionsProvider.getClass().getSimpleName()).collect(Collectors.joining(",")));
            }
            GraphQLExtensionsHandler extensionsHandler = graphQLAnnotations.getExtensionsHandler();
            extensionsHandler.setFieldRetriever(graphQLTypeRetriever.getGraphQLFieldRetriever());
            registerSchema(extensionsHandler);
        }
    }

    private void registerSchema(GraphQLExtensionsHandler extensionsHandler) {
        sdlSchemaService.clearExtensions();
        for (DXGraphQLExtensionsProvider extensionsProvider : extensionsProviders) {
            try {
                sdlSchemaService.addExtensions(extensionsProvider);
                for (Class<?> aClass : extensionsProvider.getExtensions()) {
                    if (aClass.isAnnotationPresent(GraphQLTypeExtension.class)) {
                        extensionsHandler.registerTypeExtension(aClass, container);
                        if (aClass.isAnnotationPresent(GraphQLDescription.class)) {
                            logger.debug("Registered type extension {}: {}", aClass, aClass.getAnnotation(GraphQLDescription.class).value());
                        } else {
                            logger.debug("Registered type extension {}", aClass);
                        }
                    }
                }
                for (Class<? extends GqlJcrNode> aClass : extensionsProvider.getSpecializedTypes()) {
                    SpecializedType annotation = aClass.getAnnotation(SpecializedType.class);
                    if (annotation != null) {
                        specializedTypesHandler.addType(annotation.value(), aClass);
                    } else {
                        logger.error("No annotation found on class " + aClass);
                    }
                }
            } catch (Throwable e) {
                logger.error("Unable to register extension for provider {} because {}", extensionsProvider.getClass().getName(), e.getCause().toString());
                logger.debug("full error", e);
            }
        }

        queryType = (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(Query.class, container);
        mutationType = (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(Mutation.class, container);
        subscriptionType = (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(Subscription.class, container);
        codeRegistry = container.getCodeRegistryBuilder().build();
        for (DXGraphQLExtensionsProvider extensionsProvider : extensionsProviders) {
            for (Class<?> aClass : extensionsProvider.getExtensions()) {
                if (aClass.isAnnotationPresent(GraphQLTypeExtension.class)) {
                    extensionsHandler.registerTypeExtension(aClass, container);
                    logger.debug("Registered type extension {}", aClass);
                }
            }
        }

        //Generate schema from user defined SDL
        sdlSchemaService.refreshSpecialInputTypes(graphQLAnnotations, container);
        sdlSchemaService.generateSchema();
        specializedTypesHandler.initializeTypes();

        PropertyDataFetcher.clearReflectionCache();
    }

    @Deactivate
    public void deactivate() {
        pool.shutdown();
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        List<GraphQLType> types = new ArrayList<>();

        types.add(graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNodeImpl.class, container));
        types.add(graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(DXFieldAggregation.class, container));
        types.addAll(specializedTypesHandler.getKnownTypes().values());
        types.addAll(sdlSchemaService.getSDLTypes());
        return types;
    }

    @Override
    public Collection<GraphQLFieldDefinition> getQueries() {
        List<GraphQLFieldDefinition> defs = new ArrayList<>(queryType.getFieldDefinitions());
        defs.addAll(sdlSchemaService.getSDLQueries());
        return defs;
    }

    @Override
    public Collection<GraphQLFieldDefinition> getMutations() {
        return mutationType.getFieldDefinitions();
    }

    @Override
    public Collection<GraphQLFieldDefinition> getSubscriptions() {
        return subscriptionType.getFieldDefinitions();
    }

    @Override
    public GraphQLCodeRegistry getCodeRegistry() {
        return codeRegistry;
    }

    public Class<? extends DXConnection<?>> getConnectionType(String connectionName) {
        return connectionTypes.get(connectionName);
    }

    public GraphQLOutputType getOutputType(Class<?> clazz) {
        return graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(clazz, container);
    }

    @GraphQLName("Query")
    public static class Query {
    }

    @GraphQLName("Mutation")
    public static class Mutation {
    }

    @GraphQLName("Subscription")
    public static class Subscription {
    }

    private TypeFunction defaultTypeFunction;

    @Reference(target = "(type=default)", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void setDefaultTypeFunction(TypeFunction defaultTypeFunction) {
        this.defaultTypeFunction = defaultTypeFunction;
    }

    public void unsetDefaultTypeFunction(TypeFunction defaultTypeFunction) {
        this.defaultTypeFunction = null;
    }

    private TypeFunction unboxingTypeFunction;

    @Reference(target = "(type=unboxing)", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void setUnboxingTypeFunction(TypeFunction unboxingTypeFunction) {
        this.unboxingTypeFunction = unboxingTypeFunction;
    }

    public void unsetUnboxingTypeFunction(TypeFunction unboxingTypeFunction) {
        this.defaultTypeFunction = null;
    }

}
