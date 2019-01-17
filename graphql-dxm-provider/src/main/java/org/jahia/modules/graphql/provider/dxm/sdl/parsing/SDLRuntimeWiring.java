package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaDirectiveWiring;
import org.jahia.modules.graphql.provider.dxm.sdl.types.GraphQLDate;
import org.jahia.modules.graphql.provider.dxm.sdl.types.GraphQLMetadata;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class SDLRuntimeWiring {

    public static RuntimeWiring runtimeWiring(SchemaDirectiveWiring directiveWiring) {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").build())
                .directive(SDLSchemaService.MAPPING_DIRECTIVE, directiveWiring)
                .directive(SDLSchemaService.DESCRIPTION_DIRECTIVE, directiveWiring)
                .scalar(new GraphQLDate()).scalar(new GraphQLMetadata())
                .build();
    }

}
