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
import java.util.stream.IntStream;

@Component(service = SDLSchemaService.class, immediate = true)
public class SDLSchemaService {
    private static Logger logger = LoggerFactory.getLogger(SDLSchemaService.class);

    private GraphQLSchema graphQLSchema;
    private SDLRegistrationService sdlRegistrationService;
    private Map<String, List<SDLSchemaInfo>> bundlesSDLSchemaStatus = new LinkedHashMap<>();
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
            LinkedHashMap<String, TypeCheck> possibleMissingTypes = new LinkedHashMap<>();
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
                                            bundlesSDLSchemaStatus.put(sdlResourceName, new LinkedList<>(Arrays.asList(new SDLSchemaInfo(sdlResourceName, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, MessageFormat.format("Definition {0} is missing fields", def.getName())))));
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
                                bundlesSDLSchemaStatus.put(sdlResourceName, new LinkedList<>(Arrays.asList(new SDLSchemaInfo(sdlResourceName, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, MessageFormat.format("Definition {0} is missing fields", value.getName())))));
                                parsingOK.set(false);
                            } else {
                                //Collect field definitions which are defining types that are non existing (or not yet registered)
                                ((ObjectTypeDefinition) value).getFieldDefinitions().forEach(fieldDefinition -> {
                                    if (!parsedRegistry.getType(fieldDefinition.getType()).isPresent()) {
                                        TypeCheck typeCheck;
                                        if (possibleMissingTypes.containsKey(value.getName())) {
                                            typeCheck = possibleMissingTypes.get(value.getName());
                                        } else {
                                            typeCheck = new TypeCheck(value);
                                        }
                                        //Add SDLSchemaInfo for each field that could possibly be invalid
                                        typeCheck.addSDLSchemaInfo(new SDLSchemaInfo(sdlResourceName, SDLSchemaInfo.SDLSchemaStatus.MISSING_TYPE, MessageFormat.format("The field type {0} for field {1} is not present when resolving type {2}", fieldDefinition.getType(), fieldDefinition.getName(), value.getName())));
                                        //Add field definition that is possibly invalid
                                        typeCheck.addField(fieldDefinition);
                                        possibleMissingTypes.put(value.getName(), typeCheck);
                                    }
                                });
                            }
                        }
                    });
                    if (parsingOK.get()) {
                        typeDefinitionRegistry.merge(parsedRegistry);
                        bundleTypeDefinitionRegistry.put(sdlResourceName,parsedRegistry);
                        bundlesSDLSchemaStatus.put(sdlResourceName, new LinkedList<>());
                    }
                } catch (IOException ex) {
                    logger.error("Failed to read sdl resource.", ex);
                } catch (SchemaProblem ex) {
                    logger.warn("Failed to merge schema from bundle [{}]: {}", sdlResourceName, ex.getMessage());
                    bundlesSDLSchemaStatus.put(sdlResourceName, new LinkedList<>(Arrays.asList(new SDLSchemaInfo(sdlResourceName, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, ex.getMessage()))));
                }
            }

            final AtomicBoolean typeErrorsOccurred = new AtomicBoolean(false);
            for (Map.Entry<String, TypeCheck> entry : possibleMissingTypes.entrySet()) {
                TypeDefinitionRegistry currentTypeRegistry = typeDefinitionRegistry;
                TypeCheck typeCheck = entry.getValue();
                List<FieldDefinition> fieldDefinitions = typeCheck.getFields();
                IntStream.range(0, fieldDefinitions.size()).forEach(i -> {
                    if (currentTypeRegistry.getType(fieldDefinitions.get(i).getType()).isPresent()) {
                        //remove field definition types that have been registered through SDL files provided from other bundles
                        typeCheck.getFields().remove(i);
                        typeCheck.getInfos().remove(i);
                        return;
                    }
                    typeErrorsOccurred.set(true);
                });
            }

            LinkedHashMap<ObjectTypeExtensionDefinition, List<FieldDefinition>> possibleMissingQueryExtensionsDefinitions = verifyExtendedTypesExist(typeErrorsOccurred, typeDefinitionRegistry, possibleMissingTypes, bundleTypeDefinitionRegistry);
            TypeDefinitionRegistry newRegistry = rebuildTypeDefinitionRegistry(typeErrorsOccurred, typeDefinitionRegistry, possibleMissingTypes, possibleMissingQueryExtensionsDefinitions, bundleTypeDefinitionRegistry);

            if (newRegistry != null) typeDefinitionRegistry = newRegistry;

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
                        .description(fieldDefinition.getDescription())
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
        typeDefinitionRegistry.add(DirectiveDefinition.newDirectiveDefinition()
                .name("description")
                .directiveLocations(Arrays.asList(
                        DirectiveLocation.newDirectiveLocation().name("OBJECT").build(),
                        DirectiveLocation.newDirectiveLocation().name("FIELD_DEFINITION").build()))
                .inputValueDefinitions(Arrays.asList(
                        InputValueDefinition.newInputValueDefinition().name("value").type(TypeName.newTypeName("String").build()).build()))
                .build());

        return typeDefinitionRegistry;
    }

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

    public Map<String, List<SDLSchemaInfo>> getBundlesSDLSchemaStatus() {
        return bundlesSDLSchemaStatus;
    }

    private List<SDLSchemaInfo> getOrCreateBundleSDLSchemaList(String bundle) {
        if (bundlesSDLSchemaStatus.containsKey(bundle)) {
            return bundlesSDLSchemaStatus.get(bundle);
        }
        return new LinkedList<>();
    }

    private class TypeCheck {
        private TypeDefinition type;
        private List<FieldDefinition> fields = new LinkedList<>();
        private List<SDLSchemaInfo> infos = new LinkedList<>();;

        protected TypeCheck(TypeDefinition type) {
            this.type = type;
        }

        protected void addField(FieldDefinition fieldDefinition) {
            this.fields.add(fieldDefinition);
        }

        protected void addSDLSchemaInfo(SDLSchemaInfo sdlSchemaInfo) {
            this.infos.add(sdlSchemaInfo);
        }

        public TypeDefinition getType() {
            return type;
        }

        public List<FieldDefinition> getFields() {
            return fields;
        }

        public List<SDLSchemaInfo> getInfos() {
            return infos;
        }
    }

    private LinkedHashMap<ObjectTypeExtensionDefinition, List<FieldDefinition>> verifyExtendedTypesExist(AtomicBoolean typeErrorsOccurred,
                                                                                                         TypeDefinitionRegistry typeDefinitionRegistry,
                                                                                                         LinkedHashMap<String, TypeCheck> possibleMissingTypes,
                                                                                                         Map<String, TypeDefinitionRegistry> bundleTypeDefinitionRegistry) {
        //Verify that types which are being extended exist, flag extensions that may not extend an existing type
        HashMap<String, ObjectTypeDefinition> types = (HashMap<String, ObjectTypeDefinition>) typeDefinitionRegistry.getTypesMap(ObjectTypeDefinition.class);
        LinkedHashMap<ObjectTypeExtensionDefinition, List<FieldDefinition>> possibleMissingQueryExtensionsDefinitions = new LinkedHashMap<>();
        for (Map.Entry<String, List<ObjectTypeExtensionDefinition>> entry : typeDefinitionRegistry.objectTypeExtensions().entrySet()) {
            //If this type extension is Query we will check to make sure all Field Definition Types exists
            if (entry.getKey().equals("Query")) {
                //Verify that extensions in Query exist
                entry.getValue().forEach(objectTypeExtensionDefinition -> {
                    List<FieldDefinition> possibleMissingFieldDefinitions = new LinkedList<>();
                    objectTypeExtensionDefinition.getFieldDefinitions().forEach(fieldDefinition -> {
                        String type = ((TypeName) ((ListType) fieldDefinition.getType()).getType()).getName();
                        if (!typeDefinitionRegistry.getType(type).isPresent() || possibleMissingTypes.containsKey(type)) {
                            //Flag field with potentially non existing type
                            possibleMissingFieldDefinitions.add(fieldDefinition);
                        }
                    });
                    if (possibleMissingFieldDefinitions.size() > 0) {
                        possibleMissingQueryExtensionsDefinitions.put(objectTypeExtensionDefinition, possibleMissingFieldDefinitions);
                    }
                });
                if (possibleMissingQueryExtensionsDefinitions.size() > 0) {
                    typeErrorsOccurred.set(true);
                }
                continue;
            }

            if (!types.containsKey(entry.getKey())) {
                typeDefinitionRegistry.remove(entry.getValue().get(0));
                typeErrorsOccurred.set(true);
                bundleTypeDefinitionRegistry.forEach((key, value) -> {
                    if(value.objectTypeExtensions().containsKey(entry.getKey())) {
                        logger.warn("Extension of type [{}]" + " does not exist... removing from type registry. It is declared in {}",entry.getKey(),key);
                        List<SDLSchemaInfo> bundleSdlSchemaInfoList = getOrCreateBundleSDLSchemaList(key);
                        bundleSdlSchemaInfoList.add(new SDLSchemaInfo(key, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, "Extension of type [" + entry.getKey() + "]" + " does not exist... removing from type registry."));
                        bundlesSDLSchemaStatus.put(key, bundleSdlSchemaInfoList);
                    }
                });
            }
        }

        return possibleMissingQueryExtensionsDefinitions;
    }

    private TypeDefinitionRegistry rebuildTypeDefinitionRegistry(AtomicBoolean typeErrorsOccurred,
                                                                 TypeDefinitionRegistry typeDefinitionRegistry,
                                                                 LinkedHashMap<String, TypeCheck> possibleMissingTypes,
                                                                 LinkedHashMap<ObjectTypeExtensionDefinition, List<FieldDefinition>> possibleMissingQueryExtensionsDefinitions,
                                                                 Map<String, TypeDefinitionRegistry> bundleTypeDefinitionRegistry) {
        if (typeErrorsOccurred.get()) {
            TypeDefinitionRegistry reconstructedTypeDefinitionRegistry = new TypeDefinitionRegistry();
            typeDefinitionRegistry.types().forEach((key, value) -> {
                //If there are types that are missing, then remove the field definitions
                //from the types which define these fields.
                //This excludes QUERY type
                if (!key.equals("Query") && possibleMissingTypes.size() > 0) {
                    possibleMissingTypes.forEach((type, typeCheck) -> {
                        if (key.equals(type)) {
                            typeCheck.getFields().forEach(fieldDefinition -> ((ObjectTypeDefinition)value).getFieldDefinitions().remove(fieldDefinition));
                            typeCheck.getInfos().forEach(sdlSchemaInfo -> {
                                //Add the SDL Schema Info for each bundle
                                List<SDLSchemaInfo> bundleSdlSchemaInfoList = getOrCreateBundleSDLSchemaList(sdlSchemaInfo.getBundle());
                                bundleSdlSchemaInfoList.add(sdlSchemaInfo);
                                bundlesSDLSchemaStatus.put(sdlSchemaInfo.getBundle(), bundleSdlSchemaInfoList);
                            });
                        }
                    });
                    //Skip registration of types that have no fields defined due to erroneous definitions
                    if (((ObjectTypeDefinition)value).getFieldDefinitions().size() == 0) {
                        return;
                    }
                }
                reconstructedTypeDefinitionRegistry.add(value);
            });
            //Recreate type definition registry
            typeDefinitionRegistry.objectTypeExtensions().forEach((key, value) -> {
                if (possibleMissingQueryExtensionsDefinitions.size() > 0 && key.equals("Query")) {
                    //Verify that the collected extensions are referring to existing types
                    value.forEach(queryObjectExtension -> {
                        //If this extension is not in the collection, register it directly
                        if (!possibleMissingQueryExtensionsDefinitions.containsKey(queryObjectExtension)) {
                            reconstructedTypeDefinitionRegistry.add(queryObjectExtension);
                            return;
                        }
                        List<FieldDefinition> fieldDefinitionsToRemove = new LinkedList<>();
                        possibleMissingQueryExtensionsDefinitions.get(queryObjectExtension).forEach(fieldDefinition -> {
                            String type = ((TypeName) ((ListType) fieldDefinition.getType()).getType()).getName();
                            //Verify that each field is referencing a valid type
                            if (!reconstructedTypeDefinitionRegistry.getType(type).isPresent()) {
                                //Add invalid field definitions to list and report them.
                                fieldDefinitionsToRemove.add(fieldDefinition);
                                bundleTypeDefinitionRegistry.forEach((bundle, typeRegistry) -> {
                                    List<ObjectTypeExtensionDefinition> extensions = typeRegistry.objectTypeExtensions().get("Query");
                                    if (extensions != null) {
                                        Optional<ObjectTypeExtensionDefinition> found = extensions.stream().filter(extension -> extension.getFieldDefinitions().contains(fieldDefinition)).findFirst();
                                        //Locate the bundle which defined this extension for logging and reporting purposes.
                                        if (found.isPresent()) {
                                            List<SDLSchemaInfo> bundleSdlSchemaInfoList = getOrCreateBundleSDLSchemaList(bundle);
                                            bundleSdlSchemaInfoList.add(new SDLSchemaInfo(bundle, SDLSchemaInfo.SDLSchemaStatus.MISSING_TYPE, "Query extension field [" + fieldDefinition.getName() + "] of type [" + type + "]" + " does not exist... removing from type registry."));
                                            bundlesSDLSchemaStatus.put(bundle, bundleSdlSchemaInfoList);
                                        }
                                    }
                                });
                            }
                        });
                        queryObjectExtension.getFieldDefinitions().removeAll(fieldDefinitionsToRemove);
                        //Register extensions that contain at least 1 field definition.
                        if (queryObjectExtension.getFieldDefinitions().size() > 0) {
                            reconstructedTypeDefinitionRegistry.add(queryObjectExtension);
                        }
                    });
                    return;
                }
                if (value.size() > 0) {
                    value.forEach(objectTypeExtensionDefinition -> reconstructedTypeDefinitionRegistry.add(objectTypeExtensionDefinition));
                }
            });
            typeDefinitionRegistry.getDirectiveDefinitions().forEach((key, value) -> reconstructedTypeDefinitionRegistry.add(value));
            return reconstructedTypeDefinitionRegistry;
        }
        return null;
    }
}
