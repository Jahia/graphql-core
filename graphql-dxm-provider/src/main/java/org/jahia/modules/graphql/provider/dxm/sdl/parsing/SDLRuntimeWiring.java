package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.schema.idl.*;
import org.jahia.modules.graphql.provider.dxm.sdl.types.GraphQLDate;
import org.jahia.modules.graphql.provider.dxm.sdl.types.GraphQLMetadata;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class SDLRuntimeWiring {

    public static RuntimeWiring runtimeWiring(SchemaDirectiveWiring directiveWiring) {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").build())
                .directive("mapping", directiveWiring)
                .directive("description", directiveWiring)
                .scalar(new GraphQLDate()).scalar(new GraphQLMetadata())
                .build();
    }

}
