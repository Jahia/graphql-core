package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.language.ObjectTypeExtensionDefinition.Builder;

import java.util.Map;
import java.util.function.Consumer;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.language.FieldDefinition.newFieldDefinition;

public class ConnectionHelper {

    public static void addPageInfoTypeToRegistry(TypeDefinitionRegistry typeDefinitionRegistry) {
        ObjectTypeDefinition.Builder builder = ObjectTypeDefinition.newObjectTypeDefinition();
        builder.name("CustomPageInfo")
                .description(makeDescription("Information about pagination in a connection."))
                .fieldDefinition(newFieldDefinition()
                        .name("hasNextPage")
                        .type(new NonNullType(TypeName.newTypeName(GraphQLBoolean.getName()).build()))
                        .description(makeDescription("When paginating forwards, are there more items?"))
                        .build())
                .fieldDefinition(newFieldDefinition()
                        .name("hasPreviousPage")
                        .type(new NonNullType(TypeName.newTypeName(GraphQLBoolean.getName()).build()))
                        .description(makeDescription("When paginating backwards, are there more items?"))
                        .build())
                .fieldDefinition(newFieldDefinition()
                        .name("startCursor")
                        .type(TypeName.newTypeName(GraphQLString.getName()).build())
                        .description(makeDescription("When paginating backwards, the cursor to continue."))
                        .build())
                .fieldDefinition(newFieldDefinition()
                        .name("endCursor")
                        .type(TypeName.newTypeName(GraphQLString.getName()).build())
                        .description(makeDescription("When paginating forwards, the cursor to continue."))
                        .build())
                .fieldDefinition(newFieldDefinition()
                        .name("nodesCount")
                        .type(TypeName.newTypeName(GraphQLInt.getName()).build())
                        .description(makeDescription("When paginating forwards, the cursor to continue."))
                        .build())
                .fieldDefinition(newFieldDefinition()
                        .name("totalCount")
                        .type(TypeName.newTypeName(GraphQLInt.getName()).build())
                        .description(makeDescription("When paginating forwards, the cursor to continue."))
                        .build());
        typeDefinitionRegistry.add(builder.build());
    }

    public static void addEdgeTypeToRegistry(TypeDefinitionRegistry typeDefinitionRegistry, String type) {
        ObjectTypeDefinition.Builder builder = ObjectTypeDefinition.newObjectTypeDefinition();
        builder
                .name(type + "Edge")
                .description(makeDescription("An edge in a connection"))
                .fieldDefinition(newFieldDefinition()
                        .name("node")
                        .type(TypeName.newTypeName(type).build())
                        .description(makeDescription("The item at the end of the edge"))
                        .build())
                .fieldDefinition(newFieldDefinition()
                        .name("cursor")
                        .type(new NonNullType(TypeName.newTypeName(GraphQLString.getName()).build()))
                        .description(makeDescription("cursor marks a unique position or index into the connection"))
                        .build())
                .fieldDefinition(newFieldDefinition()
                        .name("index")
                        .type(TypeName.newTypeName(GraphQLInt.getName()).build())
                        .description(makeDescription("index in the connection"))
                        .build());
        typeDefinitionRegistry.add(builder.build());
    }

    public static ObjectTypeDefinition connectionType(String type) {
        ObjectTypeDefinition.Builder builder = ObjectTypeDefinition.newObjectTypeDefinition();
        builder
                .name(type + "Connection")
                .description(makeDescription("A connection to a list of items."))
                .fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name("nodes")
                        .description(makeDescription("a list of nodes"))
                        .type(new ListType(TypeName.newTypeName(type).build()))
                        .build())
                .fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name("edges")
                        .description(makeDescription("a list of edges"))
                        .type(new ListType(TypeName.newTypeName(type + "Edge").build()))
                        .build())
                .fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name("pageInfo")
                        .description(makeDescription("details about this specific page"))
                        .type(new NonNullType(TypeName.newTypeName("CustomPageInfo").build()))
                        .build());
        return builder.build();
    }

    public static ObjectTypeExtensionDefinition newQuery() {
        return ObjectTypeExtensionDefinition.newObjectTypeExtensionDefinition()
                .name("Query")
                .build();
    }

    public static class TransformQueryExtension implements Consumer<Builder> {

        private Map<String, String> connectionFieldNameToSDLType;

        public TransformQueryExtension(Map<String, String> connectionFieldNameToSDLType) {
            this.connectionFieldNameToSDLType = connectionFieldNameToSDLType;
        }

        @Override
        public void accept(Builder builder) {
            for (Map.Entry<String, String> entry : connectionFieldNameToSDLType.entrySet()) {
                builder
                        .fieldDefinition(FieldDefinition.newFieldDefinition()
                                .name(entry.getKey())
                                .type(new ListType(TypeName.newTypeName(entry.getValue()).build()))
                                .build());
            }
        }
    }

    private static Description makeDescription(String description) {
        return new Description(description, new SourceLocation(0, 0), true);
    }
}
