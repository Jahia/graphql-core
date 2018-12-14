package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.*;
import graphql.schema.idl.errors.SchemaProblem;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.AllFinderDataFetcher;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component(service = SDLSchemaService.class, immediate = true)
public class SDLSchemaService {
    private static Logger logger = LoggerFactory.getLogger(SDLSchemaService.class);

    private GraphQLSchema graphQLSchema;
    private SDLRegistrationService sdlRegistrationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setSdlRegistrationService(SDLRegistrationService sdlRegistrationService) {
        this.sdlRegistrationService = sdlRegistrationService;
    }

    public void generateSchema() {
        if (sdlRegistrationService != null && sdlRegistrationService.getSDLResources().size() > 0) {
            SchemaParser schemaParser = new SchemaParser();
            TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
            typeDefinitionRegistry.add(new ObjectTypeDefinition("Query"));
            typeDefinitionRegistry.add(DirectiveDefinition.newDirectiveDefinition()
                    .name("mapping")
                    .directiveLocations(Arrays.asList(DirectiveLocation.newDirectiveLocation().name("OBJECT").build(),
                            DirectiveLocation.newDirectiveLocation().name("FIELD_DEFINITION").build(),
                            DirectiveLocation.newDirectiveLocation().name("INTERFACE").build()))
                    .inputValueDefinitions(Arrays.asList(
                            InputValueDefinition.newInputValueDefinition().name("node").type(TypeName.newTypeName("String").build()).build(),
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

            SDLJCRTypeChecker.checkForConsistencyWithJCR(typeDefinitionRegistry);
            SchemaGenerator schemaGenerator = new SchemaGenerator();

            try {
                graphQLSchema = schemaGenerator.makeExecutableSchema(
                        SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false),
                        typeDefinitionRegistry,
                        SDLRuntimeWiring.runtimeWiring(new SDLDirectiveWiring())
                );
            } catch (Exception e) {
                logger.error("Failed to generate GraphQL schema from merged sdl resources.", e);
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

    public List<GraphQLType> getSDLTypes() {
        List<GraphQLType> types = new ArrayList<>();
        if (graphQLSchema != null) {
            List<String> reservedType = Arrays.asList("Query","Mutation","Subscription");
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
}
