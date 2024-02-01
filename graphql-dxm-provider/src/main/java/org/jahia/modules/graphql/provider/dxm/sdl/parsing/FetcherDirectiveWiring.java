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

import graphql.schema.*;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.extension.PropertyFetcherExtensionInterface;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.Field;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.FinderListDataFetcher;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.PropertiesDataFetcherFactory;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.SDLPaginatedDataConnectionFetcher;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class FetcherDirectiveWiring implements SchemaDirectiveWiring {

    private static Logger logger = LoggerFactory.getLogger(FetcherDirectiveWiring.class);

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
        field.setProperty(def.getName());
        field.setType(def.getType().getName());
        DataFetcher typeFetcher = PropertiesDataFetcherFactory.getFetcher(def, field);

        logger.debug("field name {} ", field.getName());
        logger.debug("field type {} ", field.getType());

        SDLSchemaService service = BundleUtils.getOsgiService(SDLSchemaService.class, null);

        if (service != null) {
            Map<String, PropertyFetcherExtensionInterface> fetcherExtensions = service.getPropertyFetcherExtensions();
            String fetcherName = directive.getArgument(SDLConstants.FETCHER_DIRECTIVE_NAME).getValue().toString();
            if (fetcherName != null && fetcherExtensions.containsKey(fetcherName)) {
                typeFetcher = fetcherExtensions.get(fetcherName).getDataFetcher(field);
            }


            String parentType = environment.getNodeParentTree().getParentInfo().get().getNode().getName();
            String key = parentType + "." + def.getName();
            if (service.getConnectionFieldNameToSDLType().containsKey(key)) {
                ConnectionHelper.ConnectionTypeInfo conInfo = service.getConnectionFieldNameToSDLType().get(key);
                GraphQLOutputType node = (GraphQLOutputType) ((GraphQLList) def.getType()).getWrappedType();
                GraphQLObjectType connectionType = ConnectionHelper.getOrCreateConnection(service, node, conInfo.getMappedToType());
                List<GraphQLArgument> args = service.getRelay().getConnectionFieldArguments();
                SDLPaginatedDataConnectionFetcher<GqlJcrNode> fetcher = new SDLPaginatedDataConnectionFetcher<>((FinderListDataFetcher) typeFetcher);

                def.getDirectives().remove(0);

                return GraphQLFieldDefinition.newFieldDefinition(def)
                        .type(connectionType)
                        .dataFetcher(fetcher)
                        .argument(args)
                        .build();
            }
        }

        return GraphQLFieldDefinition.newFieldDefinition(def)
                .dataFetcher(typeFetcher)
                .build();
    }
}
