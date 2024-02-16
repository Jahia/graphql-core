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

import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.RuntimeWiring;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.types.CustomScalars;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class SDLRuntimeWiring {

    private SDLRuntimeWiring() {
    }

    public static RuntimeWiring runtimeWiring(GraphQLCodeRegistry codeRegistry) {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").build())
                .directive(SDLConstants.MAPPING_DIRECTIVE, new MappingDirectiveWiring())
                .directive(SDLConstants.FETCHER_DIRECTIVE, new FetcherDirectiveWiring())
                .codeRegistry(codeRegistry).scalar(CustomScalars.DATE)
                .build();
    }

}
