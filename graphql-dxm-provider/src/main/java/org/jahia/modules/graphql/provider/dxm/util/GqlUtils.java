package org.jahia.modules.graphql.provider.dxm.util;

import java.util.function.Supplier;

/**
 * Common GraphQL utilities.
 */
public class GqlUtils {

    /**
     * A supplier that always supplies for Boolean.FALSE.
     *
     * Should be used to supply for FALSE default value to GraphQL boolean parameters via @GraphQLDefaultValue
     */
    public static class SupplierFalse implements Supplier<Object> {

        @Override
        public Boolean get() {
            return Boolean.FALSE;
        }
    }

    /**
     * A supplier that always supplies for Boolean.TRUE.
     *
     * Should be used to supply for TRUE default value to GraphQL boolean parameters via @GraphQLDefaultValue
     */
    public static class SupplierTrue implements Supplier<Object> {

        @Override
        public Boolean get() {
            return Boolean.TRUE;
        }
    }

    private GqlUtils() {
    }
}
