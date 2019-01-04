package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.schema.*;
import graphql.schema.idl.*;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class SDLRuntimeWiring {

    public static RuntimeWiring runtimeWiring(SchemaDirectiveWiring directiveWiring) {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").build())
                .directive("mapping", directiveWiring)
                .wiringFactory(new NoopWiringFactory(){
                    @Override
                    public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
                        return DataFetchingEnvironment::getSource;
                    }
                })
                .directive("description", directiveWiring)
                .build();
    }
}
