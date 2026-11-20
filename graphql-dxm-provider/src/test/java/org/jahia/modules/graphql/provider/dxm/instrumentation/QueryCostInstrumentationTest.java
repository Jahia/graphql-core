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

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link QueryCostInstrumentation}: the verdicts it passes on a document, run through graphql-java's own
 * execution so that what is asserted is the response a client reads.
 */
public class QueryCostInstrumentationTest {

    private static final GraphQLSchema SCHEMA = new SchemaGenerator().makeExecutableSchema(
            new SchemaParser().parse("type Query { name: String }"), RuntimeWiring.newRuntimeWiring().build());

    /** Builds {@code { a0: name a1: name ... }}, a flat document executing exactly {@code fields} fields. */
    private static String aliasedFields(int fields) {
        return "{" + IntStream.range(0, fields).mapToObj(i -> "a" + i + ": name").collect(Collectors.joining(" "))
                + "}";
    }

    private static ExecutionResult execute(QueryCostInstrumentation guards, String query) {
        return GraphQL.newGraphQL(SCHEMA).instrumentation(guards).build().execute(query);
    }

    // --- expanded fields ---

    @Test
    public void shouldRefuseAnOperationExpandingPastTheLimitWithOneErrorNamingTheLimit() {
        ExecutionResult result = execute(new QueryCostInstrumentation(0, 0, 100, 0, 0), aliasedFields(101));
        assertEquals(1, result.getErrors().size());
        // The count stops one past the limit, so there is no measurement to report: the message names the bound.
        assertEquals("maximum query expanded field count exceeded, more than 100", result.getErrors().get(0).getMessage());
        assertNull(result.getData());
    }

    @Test
    public void shouldServeAnOperationAtTheLimit() {
        ExecutionResult result = execute(new QueryCostInstrumentation(0, 0, 100, 0, 0), aliasedFields(100));
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void shouldNotBoundExpandedFieldsAtZero() {
        ExecutionResult result = execute(new QueryCostInstrumentation(0, 0, 0, 0, 0), aliasedFields(101));
        assertTrue(result.getErrors().isEmpty());
    }
}
