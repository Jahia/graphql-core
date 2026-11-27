package org.jahia.modules.graphql.provider.dxm.scanner;

import graphql.annotations.annotationTypes.GraphQLTypeExtension;

@GraphQLTypeExtension(Object.class)
public class FailingInitializerExtension {

    static final Object UNAVAILABLE = fail();

    private static Object fail() {
        throw new IllegalStateException("the static initializer of FailingInitializerExtension ran");
    }
}
