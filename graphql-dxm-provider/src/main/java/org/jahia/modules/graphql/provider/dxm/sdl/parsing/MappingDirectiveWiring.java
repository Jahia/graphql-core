package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.schema.*;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.*;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MappingDirectiveWiring implements SchemaDirectiveWiring {

    private static Logger logger = LoggerFactory.getLogger(MappingDirectiveWiring.class);

    @Override
    public GraphQLObjectType onObject(SchemaDirectiveWiringEnvironment<GraphQLObjectType> environment) {
        // Mapping on object definition -> do nothing
        return environment.getElement();
    }

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        // Mapping on field

        GraphQLFieldDefinition def = environment.getElement();
        GraphQLDirective directive = environment.getDirective();

        Field field = new Field(def.getName());
        field.setProperty(directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY).getValue().toString());
        field.setType(def.getType().getName());

        logger.debug("field name {} ", field.getName());
        logger.debug("field type {} ", field.getType());

        SDLSchemaService service = BundleUtils.getOsgiService(SDLSchemaService.class, null);

        if (service != null) {
            String parentType = environment.getNodeParentTree().getParentInfo().get().getNode().getName();
            String key = parentType + "." + def.getName();
            if (service.getConnectionFieldNameToSDLType().containsKey(key)) {
                ConnectionHelper.ConnectionTypeInfo conInfo = service.getConnectionFieldNameToSDLType().get(key);
                GraphQLOutputType node = (GraphQLOutputType) ((GraphQLList) def.getType()).getWrappedType();
                GraphQLObjectType connectionType = ConnectionHelper.getOrCreateConnection(service, node, conInfo.getMappedToType());
                DataFetcher typeFetcher = PropertiesDataFetcherFactory.getFetcher(def, field);
                List<GraphQLArgument> args = service.getRelay().getConnectionFieldArguments();
                SDLPaginatedDataConnectionFetcher fetcher = new SDLPaginatedDataConnectionFetcher(typeFetcher);

                def.getDirectives().remove(0);

                return GraphQLFieldDefinition.newFieldDefinition(def)
                        .type(connectionType)
                        .dataFetcher(fetcher)
                        .argument(args)
                        .build();
            }
        }

        return GraphQLFieldDefinition.newFieldDefinition(def)
                .dataFetcher(PropertiesDataFetcherFactory.getFetcher(def, field))
                .build();
    }
}
