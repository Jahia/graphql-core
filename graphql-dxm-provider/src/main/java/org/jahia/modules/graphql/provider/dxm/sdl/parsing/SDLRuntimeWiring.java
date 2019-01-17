package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.schema.idl.RuntimeWiring;
import org.jahia.modules.graphql.provider.dxm.sdl.types.GraphQLDate;
import org.jahia.modules.graphql.provider.dxm.sdl.types.GraphQLMetadata;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class SDLRuntimeWiring {

    public static RuntimeWiring runtimeWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").build())
                .directive(SDLSchemaService.MAPPING_DIRECTIVE, new MappingDirectiveWiring())
                .scalar(new GraphQLDate()).scalar(new GraphQLMetadata())
                .build();
    }

}
