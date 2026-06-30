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
package org.jahia.modules.graphql.instrumentation;

import graphql.Scalars;
import graphql.analysis.FieldComplexityEnvironment;
import graphql.language.Field;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;
import org.jahia.modules.graphql.provider.dxm.instrumentation.JCRFieldComplexityCalculator;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JCRFieldComplexityCalculatorTest {

    private static final int FIELD_COST = 1;
    private static final int DEFAULT_LIST_FAN_OUT = 25;

    private final JCRFieldComplexityCalculator calculator = new JCRFieldComplexityCalculator();

    @Test
    public void scalarFieldCostsOnePlusChildren() {
        assertEquals(FIELD_COST, calculate(Scalars.GraphQLString, null, 0));
        assertEquals(FIELD_COST + 5, calculate(Scalars.GraphQLString, null, 5));
    }

    @Test
    public void aliasedScalarsAccumulateOneEach() {
        // The reported attack aliases a single scalar thousands of times. Each alias costs exactly FIELD_COST,
        // so graphql-java sums them to the number of aliases - well within reach of a sane maxComplexity budget.
        int total = 0;
        for (int i = 0; i < 4000; i++) {
            total += calculate(Scalars.GraphQLString, null, 0);
        }
        assertEquals(4000, total);
    }

    @Test
    public void boundedListMultipliesChildrenByRequestedPageSize() {
        assertEquals(FIELD_COST + 10 * 3, calculate(list(), Collections.singletonMap("first", 10), 3));
        assertEquals(FIELD_COST + 50 * 2, calculate(list(), Collections.singletonMap("last", 50), 2));
        assertEquals(FIELD_COST + 100 * 4, calculate(list(), Collections.singletonMap("limit", 100), 4));
    }

    @Test
    public void unboundedListUsesDefaultFanOut() {
        assertEquals(FIELD_COST + DEFAULT_LIST_FAN_OUT * 4, calculate(list(), null, 4));
    }

    @Test
    public void nonPositivePageSizeFallsBackToDefaultFanOut() {
        assertEquals(FIELD_COST + DEFAULT_LIST_FAN_OUT * 2, calculate(list(), Collections.singletonMap("first", 0), 2));
    }

    private int calculate(GraphQLOutputType fieldType, Map<String, Object> arguments, int childComplexity) {
        GraphQLFieldDefinition definition = GraphQLFieldDefinition.newFieldDefinition()
                .name("field")
                .type(fieldType)
                .build();
        Map<String, Object> args = arguments == null ? Collections.emptyMap() : arguments;
        FieldComplexityEnvironment environment = new FieldComplexityEnvironment(new Field("field"), definition, null, args, null);
        return calculator.calculate(environment, childComplexity);
    }

    private static GraphQLOutputType list() {
        return GraphQLList.list(Scalars.GraphQLString);
    }
}
