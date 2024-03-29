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
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.*;
import org.jahia.modules.graphql.provider.dxm.util.GqlTypeUtil;
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
        GraphQLAppliedDirective directive = environment.getAppliedDirective();

        Field field = new Field(def.getName());
        field.setProperty(directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY).getValue().toString());
        field.setType(GqlTypeUtil.getTypeName(def.getType()));

        logger.debug("field name {} ", field.getName());
        logger.debug("field type {} ", field.getType());

        SDLSchemaService service = BundleUtils.getOsgiService(SDLSchemaService.class, null);

        if (service != null) {
            String parentType = environment.getNodeParentTree().getParentInfo().get().getNode().getName();
            String key = parentType + "." + def.getName();
            if (service.getConnectionFieldNameToSDLType().containsKey(key)) {
                ConnectionHelper.ConnectionTypeInfo conInfo = service.getConnectionFieldNameToSDLType().get(key);
                GraphQLNamedOutputType node = (GraphQLNamedOutputType) GqlTypeUtil.unwrapType(def.getType());
                GraphQLObjectType connectionType = ConnectionHelper.getOrCreateConnection(service, node, conInfo.getMappedToType());
                DataFetcher typeFetcher = PropertiesDataFetcherFactory.getFetcher(def, field);
                List<GraphQLArgument> args = service.getRelay().getConnectionFieldArguments();
                SDLPaginatedDataConnectionFetcher<GqlJcrNode> fetcher = new SDLPaginatedDataConnectionFetcher<>((FinderListDataFetcher) typeFetcher);

                return def.transform(builder -> builder.type(connectionType).dataFetcher(fetcher).argument(args));
            }
        }

        return def.transform(builder -> builder.dataFetcher(PropertiesDataFetcherFactory.getFetcher(def, field)));
    }
}
