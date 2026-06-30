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
package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.FieldComplexityEnvironment;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;

import java.util.Map;

/**
 * Field cost calculator used by {@link graphql.analysis.MaxQueryComplexityInstrumentation} to estimate the backend
 * cost of a GraphQL query <em>before</em> it is executed.
 * <p>
 * The parser caps shipped by graphql-java (grammar depth and token count) only bound the textual shape of a document,
 * not the amount of work it triggers. A query can sit comfortably under those caps yet still fan out to thousands of
 * resolver invocations (e.g. the same scalar aliased thousands of times). This calculator scores a query by the work
 * it triggers so that such expensive-but-small documents can be rejected up front.
 * <p>
 * Scoring rules:
 * <ul>
 *     <li>every field counts as {@value #FIELD_COST};</li>
 *     <li>fields returning a list (the typical high fan-out resolvers such as children, descendants or Relay
 *     connections) additionally multiply the cost of their sub-selection by the number of items the client requested
 *     ({@code first} / {@code last} / {@code limit} argument), or by {@value #DEFAULT_LIST_FAN_OUT} when the list is
 *     unbounded.</li>
 * </ul>
 */
public class JCRFieldComplexityCalculator implements FieldComplexityCalculator {

    /** Base cost charged for every field in the query. */
    static final int FIELD_COST = 1;

    /** Fan-out assumed for a list field when the client did not bound it with a size argument. */
    static final int DEFAULT_LIST_FAN_OUT = 25;

    /** Arguments commonly used to bound the size of a list / connection result, in lookup order. */
    private static final String[] LIMIT_ARGUMENTS = {"first", "last", "limit"};

    @Override
    public int calculate(FieldComplexityEnvironment environment, int childComplexity) {
        GraphQLType fieldType = GraphQLTypeUtil.unwrapNonNull(environment.getFieldDefinition().getType());
        if (GraphQLTypeUtil.isList(fieldType)) {
            return FIELD_COST + resolveFanOut(environment.getArguments()) * childComplexity;
        }
        return FIELD_COST + childComplexity;
    }

    private int resolveFanOut(Map<String, Object> arguments) {
        if (arguments != null) {
            for (String name : LIMIT_ARGUMENTS) {
                Object value = arguments.get(name);
                if (value instanceof Number) {
                    int requested = ((Number) value).intValue();
                    if (requested > 0) {
                        return requested;
                    }
                }
            }
        }
        return DEFAULT_LIST_FAN_OUT;
    }
}
