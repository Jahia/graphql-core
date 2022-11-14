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
package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;

public class PropertiesDataFetcherFactory {

    private PropertiesDataFetcherFactory() {
        //void
    }

    public static DataFetcher getFetcher(GraphQLFieldDefinition graphQLFieldDefinition, Field field) {
        GraphQLAppliedDirective mapping = graphQLFieldDefinition.getAppliedDirective(SDLConstants.MAPPING_DIRECTIVE);
        GraphQLAppliedDirectiveArgument property = mapping != null ? mapping.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY) : null;
        String propertyValue = property != null ? property.getValue().toString() : null;

        if (SDLConstants.IDENTIFIER.equals(propertyValue)) {
            return environment -> {
                GqlJcrNode node = environment.getSource();
                return node.getUuid();
            };
        } else if (SDLConstants.PATH.equals(propertyValue)) {
            return environment -> {
                GqlJcrNode node = environment.getSource();
                return node.getPath();
            };
        } else if (SDLConstants.URL.equals(propertyValue)) {
            return environment -> {
                GqlJcrNode node = environment.getSource();
                return node.getNode().getUrl();
            };
        } else if (graphQLFieldDefinition.getType() instanceof GraphQLObjectType) {
            return new ObjectDataFetcher(field);
        } else if (graphQLFieldDefinition.getType() instanceof GraphQLList) {
            return new ListDataFetcher(field);
        }
        return new PropertiesDataFetcher(field);
    }

}
