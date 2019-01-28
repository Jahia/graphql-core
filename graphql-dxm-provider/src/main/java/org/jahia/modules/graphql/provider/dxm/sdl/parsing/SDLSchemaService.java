package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.GraphQLException;
import graphql.annotations.processor.GraphQLAnnotationsComponent;
import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.jahia.modules.graphql.provider.dxm.node.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.relay.DXRelay;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.*;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatus;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatusType;
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

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

@Component(service = SDLSchemaService.class, immediate = true)
public class SDLSchemaService {

    private static Logger logger = LoggerFactory.getLogger(SDLSchemaService.class);

    private DXRelay relay;
    private GraphQLSchema graphQLSchema;
    private SDLRegistrationService sdlRegistrationService;
    private Map<String, List<SDLSchemaInfo>> bundlesSDLSchemaStatus = new TreeMap<>();
    private Map<String, SDLDefinitionStatus> sdlDefinitionStatusMap = new TreeMap<>();
    private Map<String, GraphQLInputType> sdlSpecialInputTypes = new HashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setSdlRegistrationService(SDLRegistrationService sdlRegistrationService) {
        this.sdlRegistrationService = sdlRegistrationService;
    }

    public void generateSchema() {
        if (sdlRegistrationService != null && sdlRegistrationService.getSDLResources().size() > 0) {
            graphQLSchema = null;
            bundlesSDLSchemaStatus.clear();
            sdlDefinitionStatusMap.clear();

            TypeDefinitionRegistry typeDefinitionRegistry = prepareTypeRegistryDefinition();
            Map<TypeDefinition, String> sources = new HashMap<>();

            parseResources(typeDefinitionRegistry, sources);

            Set<TypeDefinition> invalidTypes = new HashSet<>();
            do {
                invalidTypes.clear();
                sources.forEach((type, bundle) -> {
                    SDLDefinitionStatus status = SDLTypeChecker.checkType(type, typeDefinitionRegistry);
                    sdlDefinitionStatusMap.put(type.getName(), status);
                    if (status.getStatus() != SDLDefinitionStatusType.OK) {
                        invalidTypes.add(type);
                        getOrCreateBundleSDLSchemaList(bundle).add(new SDLSchemaInfo(bundle, SDLSchemaInfo.SDLSchemaStatus.DEFINITION_ERROR, MessageFormat.format("Definition {0} : {1}", type.getName(), status.getStatusString())));
                    }
                });
                sources.keySet().removeAll(invalidTypes);
                invalidTypes.forEach(typeDefinitionRegistry::remove);
            } while (!invalidTypes.isEmpty());
            SDLTypeChecker.printStatuses(sdlDefinitionStatusMap);
            SchemaGenerator schemaGenerator = new SchemaGenerator();

            TypeDefinitionRegistry cleanedTypeRegistry = new TypeDefinitionRegistry();
            typeDefinitionRegistry.types().forEach((k, t) -> cleanedTypeRegistry.add(t));
            typeDefinitionRegistry.objectTypeExtensions().forEach((k, t) -> t.forEach(cleanedTypeRegistry::add));
            typeDefinitionRegistry.scalars().forEach((k, t) -> cleanedTypeRegistry.add(t));
            typeDefinitionRegistry.getDirectiveDefinitions().forEach((k, t) -> cleanedTypeRegistry.add(t));
            try {
                graphQLSchema = schemaGenerator.makeExecutableSchema(
                        SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false),
                        cleanedTypeRegistry,
                        SDLRuntimeWiring.runtimeWiring()
                );
            } catch (Exception e) {
                logger.warn("Invalid type definition(s) detected during schema generation {} ", e.getMessage());
            }
        }
    }

    private void parseResources(TypeDefinitionRegistry typeDefinitionRegistry, Map<TypeDefinition, String> sources) {
        SchemaParser schemaParser = new SchemaParser();
        for (Map.Entry<String, URL> entry : sdlRegistrationService.getSDLResources().entrySet()) {
            if (entry.getKey().equals("graphql-core")) continue;

            String bundle = entry.getKey();
            try {
                TypeDefinitionRegistry parsedRegistry = schemaParser.parse(new InputStreamReader(entry.getValue().openStream()));
                parsedRegistry.types().forEach((key, type) -> sources.put(type, bundle));
                parsedRegistry.objectTypeExtensions().forEach((type, list) -> list.forEach(ext -> sources.put(ext, bundle)));
                typeDefinitionRegistry.merge(parsedRegistry);
                getOrCreateBundleSDLSchemaList(bundle);
            } catch (IOException ex) {
                logger.error("Failed to read sdl resource.", ex);
            } catch (GraphQLException ex) {
                logger.warn("Failed to merge schema from bundle [{}]: {}", bundle, ex.getMessage());
                getOrCreateBundleSDLSchemaList(bundle).add(new SDLSchemaInfo(bundle, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, ex.getMessage()));
            }
        }
    }

    public List<GraphQLFieldDefinition> getSDLQueries() {
        List<GraphQLFieldDefinition> defs = new ArrayList<>();
        if (graphQLSchema != null) {

            /** implicit data fetcher for all customer types  **/
            applyDefaultFetchers(defs);

            List<GraphQLFieldDefinition> fieldDefinitions = graphQLSchema.getQueryType().getFieldDefinitions();
            for (GraphQLFieldDefinition fieldDefinition : fieldDefinitions) {
                GraphQLObjectType objectType = fieldDefinition.getType() instanceof GraphQLList ?
                        (GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType() : (GraphQLObjectType) fieldDefinition.getType();

                GraphQLDirective directive = objectType.getDirective(SDLConstants.MAPPING_DIRECTIVE);

                if (directive != null) {
                    String nodeType = directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE).getValue().toString();

                    //Handle connections
                    if (fieldDefinition.getName().contains(SDLConstants.CONNECTION_QUERY_SUFFIX)) {
                        String typeName = fieldDefinition.getName().replace(SDLConstants.CONNECTION_QUERY_SUFFIX, "");

                        //Process compatible queries and report ones that are not compatible
                        if (!typeName.endsWith(FinderFetchersFactory.FetcherType.PATH.getSuffix()) && !typeName.endsWith(FinderFetchersFactory.FetcherType.ID.getSuffix())) {
                            GraphQLOutputType node = (GraphQLOutputType) ((GraphQLList)fieldDefinition.getType()).getWrappedType();
                            GraphQLObjectType connectionType = relay.connectionType(
                                    typeName,
                                    relay.edgeType(node.getName(), node, null, Collections.emptyList()),
                                    Collections.emptyList());

                            FinderDataFetcher typeFetcher = FinderFetchersFactory.getFetcher(fieldDefinition, nodeType);
                            List<GraphQLArgument> args = relay.getConnectionFieldArguments();
                            args.addAll(typeFetcher.getArguments());
                            SDLPaginatedDataConnectionFetcher fetcher = new SDLPaginatedDataConnectionFetcher(typeFetcher);
                            GraphQLFieldDefinition sdlDef = GraphQLFieldDefinition.newFieldDefinition(fieldDefinition)
                                    .dataFetcher(fetcher)
                                    .type(connectionType)
                                    .argument(args)
                                    .build();
                            defs.add(sdlDef);
                        }
                        else {
                            //TODO report this query
                            logger.error("You cannot use this type of query as connection {}", fieldDefinition.getName());
                        }
                    }
                    else {
                        FinderDataFetcher fetcher = FinderFetchersFactory.getFetcher(fieldDefinition, nodeType);
                        GraphQLFieldDefinition sdlDef = GraphQLFieldDefinition.newFieldDefinition(fieldDefinition)
                                .dataFetcher(fetcher)
                                .argument(fetcher.getArguments())
                                .build();
                        defs.add(sdlDef);
                    }
                }
            }

        }
        return defs;
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
        if (!bundlesSDLSchemaStatus.containsKey(bundle)) {
            bundlesSDLSchemaStatus.put(bundle, new LinkedList<>());
        }

        return bundlesSDLSchemaStatus.get(bundle);
    }

    private void applyDefaultFetchers(List<GraphQLFieldDefinition> defs) {
        for (GraphQLType type : graphQLSchema.getAllTypesAsList()) {
            if (type instanceof GraphQLObjectType) {
                GraphQLDirective directive = ((GraphQLObjectType) type).getDirective(SDLConstants.MAPPING_DIRECTIVE);
                if (directive != null) {
                    applyDefaultFetcher(defs, directive, new GraphQLList(type), FinderFetchersFactory.FetcherType.ALL);
                    applyDefaultFetcher(defs, directive, (GraphQLOutputType) type, FinderFetchersFactory.FetcherType.ID);
                    applyDefaultFetcher(defs, directive, (GraphQLOutputType) type, FinderFetchersFactory.FetcherType.PATH);
                }
            }
        }
    }

    /**
     * @param defs
     * @param defaultFinder
     */
    private void applyDefaultFetcher(final List<GraphQLFieldDefinition> defs, final GraphQLDirective directive,
                                     GraphQLOutputType type, final FinderFetchersFactory.FetcherType defaultFinder) {
        boolean shouldIgnoreDefaultQueries = false;
        if (directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_IGNORE_DEFAULT_QUERIES).getValue() != null) {
            shouldIgnoreDefaultQueries = (Boolean) directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_IGNORE_DEFAULT_QUERIES).getValue();
        }

        if (!shouldIgnoreDefaultQueries) {
            //when the type is defined in extending Query, it is type of GraphQLList like [MyType]
            GraphQLOutputType baseType = type;
            if (type instanceof GraphQLList) {
                baseType = (GraphQLOutputType) ((GraphQLList) type).getWrappedType();
            }

            GraphQLArgument argument = ((GraphQLObjectType) baseType).getDirective(SDLConstants.MAPPING_DIRECTIVE).getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE);

            if (argument != null) {
                final String finderName = defaultFinder.getName(baseType.getName());

                Finder finder = new Finder();
                finder.setType(argument.getValue().toString());
                FinderDataFetcher dataFetcher = FinderFetchersFactory.getFetcherType(finder, defaultFinder);
                defs.add(GraphQLFieldDefinition.newFieldDefinition()
                        .name(finderName)
                        .description("default finder for " + finderName)
                        .dataFetcher(dataFetcher)
                        .argument(dataFetcher.getArguments())
                        .type(type)
                        .build());
            }

        }
    }

    private TypeDefinitionRegistry prepareTypeRegistryDefinition() {
        TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        typeDefinitionRegistry.add(new ObjectTypeDefinition("Query"));
        typeDefinitionRegistry.add(new ScalarTypeDefinition("Date"));
        typeDefinitionRegistry.add(DirectiveDefinition.newDirectiveDefinition()
                .name(SDLConstants.MAPPING_DIRECTIVE)
                .directiveLocations(Arrays.asList(
                        DirectiveLocation.newDirectiveLocation().name("OBJECT").build(),
                        DirectiveLocation.newDirectiveLocation().name("FIELD_DEFINITION").build()))
                .inputValueDefinitions(Arrays.asList(
                        InputValueDefinition.newInputValueDefinition().name(SDLConstants.MAPPING_DIRECTIVE_NODE).type(TypeName.newTypeName(GraphQLString.getName()).build()).build(),
                        InputValueDefinition.newInputValueDefinition().name(SDLConstants.MAPPING_DIRECTIVE_PROPERTY).type(TypeName.newTypeName(GraphQLString.getName()).build()).build(),
                        InputValueDefinition.newInputValueDefinition().name(SDLConstants.MAPPING_DIRECTIVE_IGNORE_DEFAULT_QUERIES).type(TypeName.newTypeName(GraphQLBoolean.getName()).build()).build()))
                .build());

        return typeDefinitionRegistry;
    }

    public void setRelay(DXRelay relay) {
        this.relay = relay;
    }

    /**
     * Inject
     *
     * @param graphQLAnnotations
     * @param container
     */
    public void setSdlSpecialInputTypes(GraphQLAnnotationsComponent graphQLAnnotations, ProcessingElementsContainer container){
        this.sdlSpecialInputTypes.clear();
        this.sdlSpecialInputTypes.put("FieldSorterInput", graphQLAnnotations.getInputTypeProcessor().getInputTypeOrRef(FieldSorterInput.class, container));
    }

    /**
     * Getter for the special input types map
     *
     * @param name
     * @return
     */
    public GraphQLInputType getSdlSpecialInputType(String name){
        return this.sdlSpecialInputTypes.get(name);
    }

}
