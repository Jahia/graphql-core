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

import graphql.language.*;
import graphql.language.ObjectTypeExtensionDefinition.Builder;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConnectionHelper {

    public static ObjectTypeExtensionDefinition transformQueryExtensions(
            ObjectTypeExtensionDefinition oldQuery,
            Map<String, ConnectionTypeInfo> connectionFieldNameToSDLType) {
        List<FieldDefinition> fieldDefs = oldQuery.getFieldDefinitions()
                .stream()
                .filter(f -> !connectionFieldNameToSDLType.containsKey(f.getName()))
                .collect(Collectors.toList());
        ObjectTypeExtensionDefinition newQuery = oldQuery.transformExtension(builder -> builder.fieldDefinitions(fieldDefs));
        return newQuery.transformExtension(new TransformQueryExtension(connectionFieldNameToSDLType));
    }

    public static GraphQLObjectType getOrCreateConnection(SDLSchemaService service, GraphQLNamedOutputType node, String typeName) {
        Map<String, GraphQLObjectType> edges = service.getEdges();
        Map<String, GraphQLObjectType> connections = service.getConnections();
        GraphQLObjectType edge = edges.get(node.getName());

        if (edge == null) {
            edge = service.getRelay().edgeType(node.getName(), node, null, Collections.emptyList());
            edges.put(node.getName(), edge);
        }

        GraphQLObjectType connectionType = connections.get(typeName);

        if (connectionType == null) {
            connectionType = service.getRelay().connectionType(
                    typeName,
                    edge,
                    Collections.emptyList());
            connections.put(typeName, connectionType);
        }
        return connectionType;
    }

    public static class ConnectionTypeInfo {
        private String mappedToType;
        private String connectionName;

        public ConnectionTypeInfo(String connectionName) {
            this.connectionName = connectionName;
            this.mappedToType = connectionName.replace(SDLConstants.CONNECTION_QUERY_SUFFIX, "");
        }

        public String getMappedToType() {
            return mappedToType;
        }

        public String getConnectionName() {
            return connectionName;
        }
    }

    public static class TransformQueryExtension implements Consumer<Builder> {

        private Map<String, ConnectionTypeInfo> connectionFieldNameToSDLType;

        public TransformQueryExtension(Map<String, ConnectionTypeInfo> connectionFieldNameToSDLType) {
            this.connectionFieldNameToSDLType = connectionFieldNameToSDLType;
        }

        @Override
        public void accept(Builder builder) {
            List<FieldDefinition> fieldDefs = new ArrayList<>();
            for (Map.Entry<String, ConnectionTypeInfo> entry : connectionFieldNameToSDLType.entrySet()) {
                if (entry.getKey().contains(".")) {
                    continue; // Do not include ObjectTypeDefinition connection fields
                }
                builder.fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name(entry.getKey())
                        .type(new ListType(TypeName.newTypeName(entry.getValue().getMappedToType()).build()))
                        .build());
            }
        }
    }
}
