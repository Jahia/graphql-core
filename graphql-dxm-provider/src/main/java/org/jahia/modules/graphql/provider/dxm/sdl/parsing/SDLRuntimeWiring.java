package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.ListType;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.*;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.ListDataFetcher;
import org.jahia.modules.graphql.provider.dxm.sdl.types.GraphQLDate;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class SDLRuntimeWiring {

    private SDLRuntimeWiring(){}

    public static RuntimeWiring runtimeWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").build())
                .directive(SDLConstants.MAPPING_DIRECTIVE, new MappingDirectiveWiring())
                .wiringFactory(new NoopWiringFactory() {
                    @Override
                    public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
                        if (environment.getFieldDefinition().getType() instanceof ListType) {
                            //Handle case when mapping directive is absent i. e. field: [MyType]
                            return new ListDataFetcher(null);
                        }
                        return DataFetchingEnvironment::getSource;
                    }
                })
                .scalar(new GraphQLDate())
                .build();
    }
}
