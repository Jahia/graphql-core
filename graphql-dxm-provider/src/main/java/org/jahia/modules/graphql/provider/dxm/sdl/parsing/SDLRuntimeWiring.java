/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.Scalars;
import graphql.language.ListType;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.*;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.ListDataFetcher;
import org.jahia.modules.graphql.provider.dxm.sdl.types.CustomScalars;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class SDLRuntimeWiring {

    private SDLRuntimeWiring() {
    }

    public static RuntimeWiring runtimeWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").build())
                .directive(SDLConstants.MAPPING_DIRECTIVE, new MappingDirectiveWiring())
                .directive(SDLConstants.FETCHER_DIRECTIVE, new FetcherDirectiveWiring())
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
                .scalar(CustomScalars.DATE)
                .build();
    }
}
