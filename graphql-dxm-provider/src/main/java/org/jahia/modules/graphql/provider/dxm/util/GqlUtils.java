package org.jahia.modules.graphql.provider.dxm.util;

import java.util.function.Supplier;

public class GqlUtils {

    public static class SupplierFalse implements Supplier<Object> {

        @Override
        public Boolean get() {
            return Boolean.FALSE;
        }
    }

    public static class SupplierTrue implements Supplier<Object> {

        @Override
        public Boolean get() {
            return Boolean.TRUE;
        }
    }

    private GqlUtils() {
    }
}
