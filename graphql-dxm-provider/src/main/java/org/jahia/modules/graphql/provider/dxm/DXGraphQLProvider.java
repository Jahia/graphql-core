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
import graphql.annotations.processor.GraphQLAnnotations;
import graphql.annotations.processor.GraphQLAnnotationsComponent;
import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.annotations.processor.retrievers.GraphQLExtensionsHandler;
import graphql.annotations.processor.retrievers.GraphQLObjectHandler;
import graphql.annotations.processor.typeFunctions.DefaultTypeFunction;
import graphql.annotations.processor.typeFunctions.TypeFunction;
import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.*;
import graphql.schema.idl.errors.SchemaProblem;
import graphql.servlet.*;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.customApi.*;
import org.jahia.modules.graphql.provider.dxm.customApi.Field;
import org.jahia.modules.graphql.provider.dxm.node.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXConnection;
import org.jahia.modules.graphql.provider.dxm.relay.DXRelay;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component(service = GraphQLProvider.class, enabled = false)
public class DXGraphQLProvider implements GraphQLTypesProvider, GraphQLQueryProvider, GraphQLMutationProvider,
        DXGraphQLExtensionsProvider, TypeFunction, GraphQLSubscriptionProvider {

    private static Logger logger = LoggerFactory.getLogger(DXGraphQLProvider.class);

    private static DXGraphQLProvider instance;

    private SpecializedTypesHandler specializedTypesHandler;

    private GraphQLAnnotationsComponent graphQLAnnotations;
    private GraphQLObjectHandler graphQLObjectHandler;

    private ProcessingElementsContainer container;

    private static Map<String, URL> sdlResources = new ConcurrentHashMap<>();
    private GraphQLSchema graphQLSchema;
    private Map<String, CustomApi> customApis = new HashMap<>();

    private Collection<DXGraphQLExtensionsProvider> extensionsProviders = new HashSet<>();


    private GraphQLObjectType queryType;
    private GraphQLObjectType mutationType;
    private GraphQLObjectType subscriptionType;
    private DXGraphQLConfig dxGraphQLConfig;

    private DXRelay relay;

    private Map<String, Class<? extends DXConnection<?>>> connectionTypes = new HashMap<>();
    private SDLRegistrationService sdlRegistrationService;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setSDLRegistrationService(SDLRegistrationService sdlRegistrationService) {
        this.sdlRegistrationService = sdlRegistrationService;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void bindGraphQLObjectHandler(GraphQLObjectHandler graphQLObjectHandler) {
        this.graphQLObjectHandler = graphQLObjectHandler;
    }

    @Activate
    public void activate() {
        instance = this;
        graphQLObjectHandler.getTypeRetriever().getGraphQLFieldRetriever().setAlwaysPrettify(true);
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
        generateGraphQLSchema();
//        processGeneratedDefinitions();
        specializedTypesHandler.initializeTypes();
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        List<GraphQLType> types = new ArrayList<>();

        types.add(graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNodeImpl.class, container));
        types.addAll(specializedTypesHandler.getKnownTypes().values());
        List<String> reservedType = Arrays.asList("Query","Mutation","Subscription");
        if (graphQLSchema != null) {
            for (Map.Entry<String, GraphQLType> gqlTypeEntry : graphQLSchema.getTypeMap().entrySet()) {
                if (!gqlTypeEntry.getKey().startsWith("__") && !reservedType.contains(gqlTypeEntry.getKey()) && !(gqlTypeEntry.getValue() instanceof GraphQLScalarType)) {
                    types.add(gqlTypeEntry.getValue());
                }
            }
        }
        return types;
    }

    @Override
    public Collection<GraphQLFieldDefinition> getQueries() {
        List<GraphQLFieldDefinition> defs = new ArrayList<>(queryType.getFieldDefinitions());
//        for (CustomApi apiType : customApis.values()) {
//            defs.addAll(apiType.getQueryFields());
//        }
        if (graphQLSchema != null) {
            List<GraphQLFieldDefinition> fieldDefinitions = graphQLSchema.getQueryType().getFieldDefinitions();
            for (GraphQLFieldDefinition fieldDefinition : fieldDefinitions) {
                AllFinderDataFetcher dataFetcher = new AllFinderDataFetcher(fieldDefinition.getType() instanceof GraphQLList ?
                        ((GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType()).getDirective("mapping").getArgument("node").getValue().toString() :
                        ((GraphQLObjectType) fieldDefinition.getType()).getDirective("mapping").getArgument("node").getValue().toString());
                GraphQLFieldDefinition customApiFieldDef = GraphQLFieldDefinition.newFieldDefinition()
                        .name(fieldDefinition.getName())
                        .dataFetcher(dataFetcher)
                        .argument(dataFetcher.getArguments())
                        .type(fieldDefinition.getType()) // todo return a connection to type if finder is multiple
                        .build();
                defs.add(customApiFieldDef);
            }

        }
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

    private void generateGraphQLSchema() {
        if (sdlRegistrationService == null || sdlRegistrationService.getSDLResources().size() == 0) {
            return;
        }
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        typeDefinitionRegistry.add(new ObjectTypeDefinition("Query"));
        typeDefinitionRegistry.add(DirectiveDefinition.newDirectiveDefinition()
                .name("mapping")
                .directiveLocations(Arrays.asList(DirectiveLocation.newDirectiveLocation().name("OBJECT").build(),
                        DirectiveLocation.newDirectiveLocation().name("FIELD_DEFINITION").build(),
                        DirectiveLocation.newDirectiveLocation().name("INTERFACE").build()))
                .inputValueDefinitions(Arrays.asList(InputValueDefinition.newInputValueDefinition().name("node").type(TypeName.newTypeName("String").build()).build(),
                        InputValueDefinition.newInputValueDefinition().name("property").type(TypeName.newTypeName("String").build()).build()))
                .build());
        for (Map.Entry<String, URL> entry : sdlRegistrationService.getSDLResources().entrySet()) {
            try {
                typeDefinitionRegistry.merge(schemaParser.parse(new InputStreamReader(entry.getValue().openStream())));
            } catch (IOException ex) {
                logger.error("Failed to read sdl resource.", ex);
            } catch (SchemaProblem ex) {
                logger.warn("Failed to merge schema from bundle [" + entry.getKey() + "]:", ex.getMessage());
            }
        }
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").build())
                .directive("mapping", new SchemaDirectiveWiring() {
                    @Override
                    public GraphQLObjectType onObject(SchemaDirectiveWiringEnvironment<GraphQLObjectType> environment) {
                        return environment.getElement();
                    }

                    @Override
                    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
                        try {
                            //Todo add error/syntax check
                            ObjectTypeDefinition parentType = (ObjectTypeDefinition) ((NodeParentTree) ((SchemaDirectiveWiringEnvironmentImpl) environment).getNodeParentTree().getParentInfo().get()).getNode();
                            Directive parentTypeDirective = parentType.getDirective("mapping");
                            String nodeType = ((StringValue) parentTypeDirective.getArgument("node").getValue()).getValue();
                            ExtendedNodeType type = NodeTypeRegistry.getInstance().getNodeType(nodeType);
                            Map<String, ExtendedPropertyDefinition> extendedPropertyDefinitionMap = type.getPropertyDefinitionsAsMap();
                            Field field = new Field(environment.getElement().getName());
                            field.setProperty(environment.getDirective().getArgument("property").getValue().toString());
                            return GraphQLFieldDefinition.newFieldDefinition()
                                    .name(environment.getElement().getName())
                                    .dataFetcher(new PropertiesDataFetcher(null, field))
                                    .argument(GraphQLArgument.newArgument().name("language").type(new GraphQLNonNull(GraphQLString)).build())
                                    .type(GraphQLString)
                                    .build();
                        } catch (NoSuchNodeTypeException e) {
                            logger.error("Mapping is not referring to existing type ", e);
                        }
                        return null;
                    }
                })
                .wiringFactory(new NoopWiringFactory(){
                    @Override
                    public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
                        return DataFetchingEnvironment::getSource;
                    }
                })
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        try {
            graphQLSchema = schemaGenerator.makeExecutableSchema(SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false), typeDefinitionRegistry, runtimeWiring);
        } catch (Exception e) {
            logger.error("Failed to generate GraphQL schema from merged sdl resources.", e);
        }
    }

//    private void processGeneratedDefinitions() {
//        try {
//            JCRSessionWrapper sessionWrapper = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null);
//            Bundle bundle = BundleUtils.getBundleBySymbolicName("graphql-definition-generator", null);
//            if (bundle != null) {
//                JahiaTemplatesPackage module = BundleUtils.getModule(bundle);
//                if (sessionWrapper.nodeExists("/modules/graphql-definition-generator/" + module.getVersion() + "/templates/contents/definitions")) {
//                    JCRNodeWrapper definitions = sessionWrapper.getNode("/modules/graphql-definition-generator/" + module.getVersion() + "/templates/contents/definitions");
//                    String s = definitions.getPropertyAsString("definitions");
//                    if (StringUtils.isNotEmpty(s)) {
//                        try {
//                            JSONArray definitionsList = new JSONArray(s);
//                            for (int i = 0; i < definitionsList.length(); i++) {
//                                JSONObject definition = definitionsList.getJSONObject(i);
//                                String type = definition.getString("queryName");
//                                ConfigUtil.registerTypeDefinition(type, definition.getString("definitionName"), customApis);
//                                JSONArray properties = definition.getJSONArray("usedProperties");
//                                for (int j = 0; j < properties.length(); j++) {
//                                    JSONObject usedProperty = properties.getJSONObject(j);
//                                    String userDefinedPropertyName = !"null".equals(usedProperty.getString("userDefinedName")) ? usedProperty.getString("userDefinedName") : usedProperty.getString("name");
//                                    ConfigUtil.registerPropertyDefinition(type, usedProperty.getString("name"), userDefinedPropertyName, customApis);
//                                }
//                            }
//                        } catch (JSONException ex) {
//                            logger.error("Failed to parse definitions JSON.", ex);
//                        }
//                    }
//                }
//            }
//        } catch(RepositoryException ex){
//            logger.error("Failed to retrieve session.", ex);
//        }
//    }
}