package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.errors.SchemaProblem;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.AllFinderDataFetcher;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatus;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLSchemaInfo;
import org.jahia.modules.graphql.provider.dxm.sdl.registration.SDLRegistrationService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component(service = SDLSchemaService.class, immediate = true)
public class SDLSchemaService {
    private static Logger logger = LoggerFactory.getLogger(SDLSchemaService.class);

    private GraphQLSchema graphQLSchema;
    private SDLRegistrationService sdlRegistrationService;
    private Map<String, SDLSchemaInfo> bundlesSDLSchemaStatus = new LinkedHashMap<>();
    private Map<String, SDLDefinitionStatus> sdlDefinitionStatusMap = new LinkedHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setSdlRegistrationService(SDLRegistrationService sdlRegistrationService) {
        this.sdlRegistrationService = sdlRegistrationService;
    }

    public void generateSchema() {
        if (sdlRegistrationService != null && sdlRegistrationService.getSDLResources().size() > 0) {
            bundlesSDLSchemaStatus = new LinkedHashMap<>();
            SchemaParser schemaParser = new SchemaParser();
            TypeDefinitionRegistry typeDefinitionRegistry = prepareTypeRegistryDefinition();
            Map<String, TypeDefinitionRegistry> bundleTypeDefinitionRegistry = new LinkedHashMap<>();
            for (Map.Entry<String, URL> entry : sdlRegistrationService.getSDLResources().entrySet()) {
                String sdlResourceName = entry.getKey();
                try {
                    TypeDefinitionRegistry parsedRegistry = schemaParser.parse(new InputStreamReader(entry.getValue().openStream()));
                    AtomicBoolean parsingOK = new AtomicBoolean(true);
                    parsedRegistry.objectTypeExtensions().forEach((key, value) -> {
                        if (parsingOK.get()) {
                            parsingOK.set(value.stream().noneMatch((def) -> {
                                        if (def.getFieldDefinitions().isEmpty()) {
                                            logger.warn("Failed to merge schema from bundle [{}] there is missing fields definition in type {}", sdlResourceName, def.getName());
                                            bundlesSDLSchemaStatus.put(sdlResourceName, new SDLSchemaInfo(sdlResourceName, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, MessageFormat.format("Definition {0} is missing fields", def.getName())));
                                            return true;
                                        }
                                        return false;
                                    }
                            ));
                        }
                    });
                    parsedRegistry.types().forEach((key, value) -> {
                        if (parsingOK.get() && value instanceof ObjectTypeDefinition) {
                            if (((ObjectTypeDefinition) value).getFieldDefinitions().isEmpty()) {
                                logger.warn("Failed to merge schema from bundle [{}] there is missing fields definition in type {}", sdlResourceName, value.getName());
                                bundlesSDLSchemaStatus.put(sdlResourceName, new SDLSchemaInfo(sdlResourceName, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, MessageFormat.format("Definition {0} is missing fields", value.getName())));
                                parsingOK.set(false);
                            }
                        }
                    });
                    if (parsingOK.get()) {
                        typeDefinitionRegistry.merge(parsedRegistry);
                        bundleTypeDefinitionRegistry.put(sdlResourceName,parsedRegistry);
                        bundlesSDLSchemaStatus.put(sdlResourceName, new SDLSchemaInfo(sdlResourceName));
                    }
                } catch (IOException ex) {
                    logger.error("Failed to read sdl resource.", ex);
                } catch (SchemaProblem ex) {
                    logger.warn("Failed to merge schema from bundle [{}]: {}", sdlResourceName, ex.getMessage());
                    bundlesSDLSchemaStatus.put(sdlResourceName, new SDLSchemaInfo(sdlResourceName, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, ex.getMessage()));
                }
            }

            //Verify that types which are being extended exist, remove those extensions that do not extend an existing type
            HashMap<String, ObjectTypeDefinition> types = (HashMap<String, ObjectTypeDefinition>) typeDefinitionRegistry.getTypesMap(ObjectTypeDefinition.class);
            boolean typeErrorsOccurred = false;
            for (Map.Entry<String, List<ObjectTypeExtensionDefinition>> entry : typeDefinitionRegistry.objectTypeExtensions().entrySet()) {
                if (!types.containsKey(entry.getKey())) {
                    typeDefinitionRegistry.remove(entry.getValue().get(0));
                    typeErrorsOccurred = true;
                    bundleTypeDefinitionRegistry.forEach((key, value) -> {
                        if(value.objectTypeExtensions().containsKey(entry.getKey())) {
                            logger.warn("Extension of type [{}]" + " does not exist... removing from type registry. It is declared in {}",entry.getKey(),key);
                            bundlesSDLSchemaStatus.put(key, new SDLSchemaInfo(key, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, "Extension of type [" + entry.getKey() + "]" + " does not exist... removing from type registry."));
                        }
                    });
                }
            }
            if (typeErrorsOccurred) {
                TypeDefinitionRegistry reconstructedTypeDefinitionRegistry = new TypeDefinitionRegistry();
                //Recreate type definition registry
                typeDefinitionRegistry.objectTypeExtensions().forEach((key, value) -> {
                    if (value.size() > 0) {
                        value.forEach(objectTypeExtensionDefinition -> reconstructedTypeDefinitionRegistry.add(objectTypeExtensionDefinition));
                    }
                });
                typeDefinitionRegistry.types().forEach((key, value) -> reconstructedTypeDefinitionRegistry.add(value));
                typeDefinitionRegistry.getDirectiveDefinitions().forEach((key, value) -> reconstructedTypeDefinitionRegistry.add(value));
                typeDefinitionRegistry = reconstructedTypeDefinitionRegistry;
            }

            sdlDefinitionStatusMap = SDLJCRTypeChecker.checkForConsistencyWithJCR(typeDefinitionRegistry);
            SchemaGenerator schemaGenerator = new SchemaGenerator();

            try {
                graphQLSchema = schemaGenerator.makeExecutableSchema(
                        SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false),
                        typeDefinitionRegistry,
                        SDLRuntimeWiring.runtimeWiring(new SDLDirectiveWiring())
                );
            } catch (Exception e) {
                logger.warn("Invalid type definition(s) detected during schema generation: " + e.getMessage());
            }
        }
    }

    public List<GraphQLFieldDefinition> getSDLQueries() {
        List<GraphQLFieldDefinition> defs = new ArrayList<>();
        if (graphQLSchema != null) {
            List<GraphQLFieldDefinition> fieldDefinitions = graphQLSchema.getQueryType().getFieldDefinitions();
            for (GraphQLFieldDefinition fieldDefinition : fieldDefinitions) {
                AllFinderDataFetcher dataFetcher = new AllFinderDataFetcher(fieldDefinition.getType() instanceof GraphQLList ?
                        ((GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType()).getDirective("mapping").getArgument("node").getValue().toString() :
                        ((GraphQLObjectType) fieldDefinition.getType()).getDirective("mapping").getArgument("node").getValue().toString());
                GraphQLFieldDefinition sdlDef = GraphQLFieldDefinition.newFieldDefinition()
                        .name(fieldDefinition.getName())
                        .dataFetcher(dataFetcher)
                        .argument(dataFetcher.getArguments())
                        .type(fieldDefinition.getType()) // todo return a connection to type if finder is multiple
                        .build();
                defs.add(sdlDef);
            }
        }
        return defs;
    }

    private TypeDefinitionRegistry prepareTypeRegistryDefinition() {
        TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        typeDefinitionRegistry.add(new ObjectTypeDefinition("Query"));
        typeDefinitionRegistry.add(DirectiveDefinition.newDirectiveDefinition()
                .name("mapping")
                .directiveLocations(Arrays.asList(DirectiveLocation.newDirectiveLocation().name("OBJECT").build(),
                        DirectiveLocation.newDirectiveLocation().name("FIELD_DEFINITION").build()))
                .inputValueDefinitions(Arrays.asList(
                        InputValueDefinition.newInputValueDefinition().name("node").type(TypeName.newTypeName("String").build()).build(),
                        InputValueDefinition.newInputValueDefinition().name("property").type(TypeName.newTypeName("String").build()).build()))
                .build());
        return typeDefinitionRegistry;
    }

    ;

    public List<GraphQLType> getSDLTypes() {
        List<GraphQLType> types = new ArrayList<>();
        if (graphQLSchema != null) {
            List<String> reservedType = Arrays.asList("Query", "Mutation", "Subscription");
            for (Map.Entry<String, GraphQLType> gqlTypeEntry : graphQLSchema.getTypeMap().entrySet()) {
                if (!gqlTypeEntry.getKey().startsWith("__") && !reservedType.contains(gqlTypeEntry.getKey()) && !(gqlTypeEntry.getValue() instanceof GraphQLScalarType)) {
                    types.add(gqlTypeEntry.getValue());
                }
            }
        }
        return types;
    }

    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }

    public Map<String, SDLDefinitionStatus> getSdlDefinitionStatusMap() {
        return sdlDefinitionStatusMap;
    }

    public Map<String, SDLSchemaInfo> getBundlesSDLSchemaStatus() {
        return bundlesSDLSchemaStatus;
    }
}
