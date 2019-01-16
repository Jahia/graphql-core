package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.GraphQLException;
import graphql.language.ObjectTypeDefinition;
import graphql.schema.idl.errors.MissingInterfaceFieldError;

class EmptyFieldsError extends GraphQLException {
    EmptyFieldsError(ObjectTypeDefinition def) {
        super(String.format("Definition '%s' %s is missing fields", def.getName(), MissingInterfaceFieldError.lineCol(def)));
    }
}
