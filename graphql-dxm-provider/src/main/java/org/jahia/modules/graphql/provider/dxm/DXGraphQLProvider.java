/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
import graphql.annotations.processor.retrievers.GraphQLExtensionsHandler;
import graphql.annotations.processor.typeFunctions.DefaultTypeFunction;
import graphql.annotations.processor.typeFunctions.TypeFunction;
import graphql.schema.*;
import graphql.schema.idl.*;
import graphql.servlet.*;
import org.jahia.modules.graphql.provider.dxm.customApi.CustomApi;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.node.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXConnection;
import org.jahia.modules.graphql.provider.dxm.relay.DXRelay;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.annotations.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = GraphQLProvider.class, enabled = false)
public class DXGraphQLProvider implements GraphQLTypesProvider, GraphQLQueryProvider, GraphQLMutationProvider,
        SynchronousBundleListener, DXGraphQLExtensionsProvider, TypeFunction, GraphQLSubscriptionProvider {

    private static Logger logger = LoggerFactory.getLogger(DXGraphQLProvider.class);

    private static DXGraphQLProvider instance;

    private SpecializedTypesHandler specializedTypesHandler;

    private GraphQLAnnotationsComponent graphQLAnnotations;

    private ProcessingElementsContainer container;

    private BundleContext bundleContext;

    private static Map<String, String> bundleQueryRegistry = new ConcurrentHashMap<>();

    private Collection<DXGraphQLExtensionsProvider> extensionsProviders = new HashSet<>();

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private GraphQLObjectType queryType;
    private GraphQLObjectType mutationType;
    private GraphQLObjectType subscriptionType;

    private DXGraphQLConfig dxGraphQLConfig;

    private DXRelay relay;

    private Map<String, Class<? extends DXConnection<?>>> connectionTypes = new HashMap<>();

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
    public void bindDxGraphQLConfig(DXGraphQLConfig dxGraphQLConfig) {
        this.dxGraphQLConfig = dxGraphQLConfig;
    }

    @Activate
    public void activate(BundleContext context) {
        instance = this;

        setBundleContext(context);
        bundleContext.addBundleListener(instance);

        container = graphQLAnnotations.createContainer();
        specializedTypesHandler = new SpecializedTypesHandler(graphQLAnnotations, container);
        ((DefaultTypeFunction)defaultTypeFunction).register(this);

        GraphQLExtensionsHandler extensionsHandler = graphQLAnnotations.getExtensionsHandler();

        relay = new DXRelay();
        container.setRelay(relay);

        connectionTypes.put("JCRNodeConnection", GqlJcrNodeConnection.class);

        for (Map.Entry<String, Class<? extends DXConnection<?>>> entry : connectionTypes.entrySet()) {
            relay.addConnectionType(entry.getKey(), (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(entry.getValue(), container));
        }

        extensionsProviders.add(this);

        for (DXGraphQLExtensionsProvider extensionsProvider : extensionsProviders) {
            for (Class<?> aClass : extensionsProvider.getExtensions()) {
                extensionsHandler.registerTypeExtension(aClass, container);
                if (aClass.isAnnotationPresent(GraphQLDescription.class)) {
                    logger.info("Registered type extension {}: {}", aClass, aClass.getAnnotation(GraphQLDescription.class).value());
                } else {
                    logger.info("Registered type extension {}", aClass);
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
                logger.info("Registered type extension {}", aClass);
            }
        }

        specializedTypesHandler.initializeTypes();

    }

    @Override
    public Collection<GraphQLType> getTypes() {
        List<GraphQLType> types = new ArrayList<>();

        types.add(graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNodeImpl.class, container));
        types.addAll(specializedTypesHandler.getKnownTypes().values());
        return types;
    }

    @Override
    public Collection<GraphQLFieldDefinition> getQueries() {
        List<GraphQLFieldDefinition> defs = new ArrayList<>(queryType.getFieldDefinitions());

        for (CustomApi apiType : dxGraphQLConfig.getCustomApis().values()) {
            defs.addAll(apiType.getQueryFields());
        }

        for(String sdl : bundleQueryRegistry.values()){
            /** parse schema **/
            final SchemaParser schemaParser = new SchemaParser();
            final TypeDefinitionRegistry typeRegistry = schemaParser.parse(sdl);

            final RuntimeWiring wiring = buildRuntimeWiring();
            final SchemaGenerator schemaGenerator = new SchemaGenerator();
            final GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, wiring);

            defs.addAll(graphQLSchema.getQueryType().getFieldDefinitions());
        }

        return defs;
    }

    /**
     *
     *
     * @return
     */
    //TODO apply data fetcher from GraphQLExtension object
    private static RuntimeWiring buildRuntimeWiring() {
        return RuntimeWiring.newRuntimeWiring().wiringFactory(new EchoingWiringFactory()).build();
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

    /**
     * this method subscribe the bundle event and parse the graphql schema
     * which the bundle contains
     */
    @Override
    public void bundleChanged(BundleEvent bundleEvent) {

        final String name = bundleEvent.getBundle().getSymbolicName();
        if (name != null) {
            logger.debug("bundle name [", name, "] event type [", bundleEvent.getType(), "]");

            switch (bundleEvent.getType()) {
                case BundleEvent.STARTING:
                case BundleEvent.UPDATED:
                    try{
                        //fetch the schema file from bundle
                        URL url = bundleEvent.getBundle().getResource("META-INF/graphql-extension.sdl");
                        if(url != null){
                            logger.debug("get bundle schema " + url.getPath());
                            final StringBuffer sb = new StringBuffer();
                            final BufferedReader br =new BufferedReader(new InputStreamReader(url.openStream()));
                            while(br.ready()){
                                sb.append(br.readLine());
                            }
                            br.close();

                            registerBundle(name, sb.toString());
                        }

                    }catch (IOException e){
                        e.printStackTrace();
                        logger.error("fail to extract schema file from bundle, ignore this step");
                    }finally{
                        break;
                    }
                case BundleEvent.STOPPING:
                    //unregister the schema
                    registerBundle(name, null);

                    break;
                default:
                    logger.debug("nothing is to be changed for the bundle query registry");
                    break;
            }

        }
    }

    /**
     *
     * @param name
     * @param sdlResource
     */
    private void registerBundle(final String name, final String sdlResource){
        if(sdlResource == null){
            bundleQueryRegistry.remove(name);
            logger.debug("remove type registry for " + name);
        }else{

            bundleQueryRegistry.put(name, sdlResource);
            logger.debug("add new type registry for " + name);
        }
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

    @Reference(target = "(type=default)", policy=ReferencePolicy.DYNAMIC, policyOption= ReferencePolicyOption.GREEDY)
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
        return defaultTypeFunction.buildType(input, klass, arg,container);
    }


}