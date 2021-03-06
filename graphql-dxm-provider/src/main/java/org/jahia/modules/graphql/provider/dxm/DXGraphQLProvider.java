/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.processor.GraphQLAnnotationsComponent;
import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.annotations.processor.retrievers.*;
import graphql.annotations.processor.searchAlgorithms.SearchAlgorithm;
import graphql.annotations.processor.typeFunctions.DefaultTypeFunction;
import graphql.annotations.processor.typeFunctions.TypeFunction;
import graphql.schema.*;
import graphql.servlet.*;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.node.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXConnection;
import org.jahia.modules.graphql.provider.dxm.relay.DXRelay;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.SDLSchemaService;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLFieldWithPermissionRetriever;
import org.osgi.service.component.annotations.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = GraphQLProvider.class, immediate = true)
public class DXGraphQLProvider implements GraphQLTypesProvider, GraphQLQueryProvider, GraphQLMutationProvider, DXGraphQLExtensionsProvider, TypeFunction, GraphQLSubscriptionProvider {
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

    private DXRelay relay;

    private Map<String, Class<? extends DXConnection<?>>> connectionTypes = new HashMap<>();
    private SDLSchemaService sdlSchemaService;

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

    public ProcessingElementsContainer getContainer() {
        return container;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    public void addExtensionProvider(DXGraphQLExtensionsProvider provider) {
        this.extensionsProviders.add(provider);
    }

    public void removeExtensionProvider(DXGraphQLExtensionsProvider provider) {
        this.extensionsProviders.remove(provider);
    }

    @Reference
    public void setDxGraphQLConfig(DXGraphQLConfig dxGraphQLConfig) {
        this.dxGraphQLConfig = dxGraphQLConfig;
    }

    @Activate
    public void activate() {
        instance = this;

        GraphQLFieldWithPermissionRetriever graphQLFieldWithPermissionsRetriever = new GraphQLFieldWithPermissionRetriever(dxGraphQLConfig, graphQLFieldRetriever);

        graphQLTypeRetriever.setGraphQLObjectInfoRetriever(graphQLObjectInfoRetriever);
        graphQLTypeRetriever.setGraphQLInterfaceRetriever(graphQLInterfaceRetriever);
        graphQLTypeRetriever.setGraphQLFieldRetriever(graphQLFieldWithPermissionsRetriever);
        graphQLTypeRetriever.setFieldSearchAlgorithm(fieldSearchAlgorithm);
        graphQLTypeRetriever.setMethodSearchAlgorithm(methodSearchAlgorithm);
        graphQLTypeRetriever.setExtensionsHandler(extensionsHandler);

        graphQLFieldRetriever.setAlwaysPrettify(true);
        container = graphQLAnnotations.createContainer();

        specializedTypesHandler = new SpecializedTypesHandler(graphQLAnnotations, container);
        ((DefaultTypeFunction) defaultTypeFunction).register(this);

        GraphQLExtensionsHandler extensionsHandler = graphQLAnnotations.getExtensionsHandler();
        extensionsHandler.setFieldRetriever(graphQLFieldWithPermissionsRetriever);

        relay = new DXRelay();
        sdlSchemaService.setRelay(relay);
        container.setRelay(relay);

        connectionTypes.put("JCRNodeConnection", GqlJcrNodeConnection.class);

        for (Map.Entry<String, Class<? extends DXConnection<?>>> entry : connectionTypes.entrySet()) {
            relay.addConnectionType(entry.getKey(), (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(entry.getValue(), container));
        }

        extensionsProviders.add(this);

        sdlSchemaService.clearExtensions();
        for (DXGraphQLExtensionsProvider extensionsProvider : extensionsProviders) {
            sdlSchemaService.addExtensions(extensionsProvider);
            for (Class<?> aClass : extensionsProvider.getExtensions()) {
                extensionsHandler.registerTypeExtension(aClass, container);
                if (aClass.isAnnotationPresent(GraphQLDescription.class)) {
                    logger.debug("Registered type extension {}: {}", aClass, aClass.getAnnotation(GraphQLDescription.class).value());
                } else {
                    logger.debug("Registered type extension {}", aClass);
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
        }

        queryType = (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(Query.class, container);
        mutationType = (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(Mutation.class, container);
        subscriptionType = (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(Subscription.class, container);

        for (DXGraphQLExtensionsProvider extensionsProvider : extensionsProviders) {
            for (Class<?> aClass : extensionsProvider.getExtensions()) {
                extensionsHandler.registerTypeExtension(aClass, container);
                logger.debug("Registered type extension {}", aClass);
            }
        }

        //Generate schema from user defined SDL
        sdlSchemaService.refreshSpecialInputTypes(graphQLAnnotations, container);
        sdlSchemaService.generateSchema();
        specializedTypesHandler.initializeTypes();

        PropertyDataFetcher.clearReflectionCache();
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        List<GraphQLType> types = new ArrayList<>();

        types.add(graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNodeImpl.class, container));
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

    @Override
    public boolean canBuildType(Class<?> aClass, AnnotatedType annotatedType) {
        return Publisher.class.isAssignableFrom(aClass);
    }

    @Override
    public GraphQLType buildType(boolean input, Class<?> aClass, AnnotatedType annotatedType, ProcessingElementsContainer container) {
        if (!(annotatedType instanceof AnnotatedParameterizedType)) {
            throw new IllegalArgumentException("List type parameter should be specified");
        }
        AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) annotatedType;
        AnnotatedType arg = parameterizedType.getAnnotatedActualTypeArguments()[0];
        Class<?> klass;
        if (arg.getType() instanceof ParameterizedType) {
            klass = (Class<?>) ((ParameterizedType) (arg.getType())).getRawType();
        } else {
            klass = (Class<?>) arg.getType();
        }
        return defaultTypeFunction.buildType(input, klass, arg, container);
    }
}