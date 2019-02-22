package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.language.ObjectTypeExtensionDefinition.Builder;

import java.util.Map;
import java.util.function.Consumer;

public class ConnectionHelper {

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
}
