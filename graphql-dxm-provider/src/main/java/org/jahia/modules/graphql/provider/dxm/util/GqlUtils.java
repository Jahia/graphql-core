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
