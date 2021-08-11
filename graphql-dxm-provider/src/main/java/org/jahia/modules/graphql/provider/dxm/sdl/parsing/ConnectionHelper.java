package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.language.ObjectTypeExtensionDefinition.Builder;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConnectionHelper {

    public static ObjectTypeExtensionDefinition newQueryWithoutConnections(ObjectTypeExtensionDefinition oldQuery, Map<String, ConnectionTypeInfo> connectionFieldNameToSDLType) {
        Builder query = ObjectTypeExtensionDefinition.newObjectTypeExtensionDefinition()
                .name("Query");

        query.name(oldQuery.getName());
        query.comments(oldQuery.getComments());
        query.description(oldQuery.getDescription());
        query.directives(oldQuery.getDirectives());
        query.implementz(oldQuery.getImplements());
        query.sourceLocation(oldQuery.getSourceLocation());
        query.fieldDefinitions(oldQuery.getFieldDefinitions()
                .stream()
                .filter(f -> !connectionFieldNameToSDLType.containsKey(f.getName()))
                .collect(Collectors.toList())
        );

        return query.build();
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

        public ConnectionTypeInfo(String mappedToType, String connectionName) {
            this.mappedToType = mappedToType;
            this.connectionName = connectionName;
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
            for (Map.Entry<String, ConnectionTypeInfo> entry : connectionFieldNameToSDLType.entrySet()) {
                builder
                        .fieldDefinition(FieldDefinition.newFieldDefinition()
                                .name(entry.getKey())
                                .type(new ListType(TypeName.newTypeName(entry.getValue().getMappedToType()).build()))
                                .build());
            }
        }
    }
}
