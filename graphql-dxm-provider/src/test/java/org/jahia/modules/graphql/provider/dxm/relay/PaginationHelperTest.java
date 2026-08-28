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
package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.GraphQLContext;
import graphql.Scalars;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import org.jahia.modules.graphql.provider.dxm.GqlLimitExceededException;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for the per-request node allowance, the bound that stops a document nesting connections from expanding
 * into an unbounded walk of the repository.
 */
public class PaginationHelperTest {

    private static final List<String> NODES = Arrays.asList("a", "b", "c", "d", "e");

    private static DataFetchingEnvironment environmentWithAllowance(Integer allowance) {
        GraphQLContext context = GraphQLContext.newContext().build();
        if (allowance != null) {
            context.put(PaginationHelper.REMAINING_NODE_ALLOWANCE, new AtomicInteger(allowance));
        }
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .graphQLContext(context)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .type(Scalars.GraphQLString)
                        .path(ResultPath.parse("/test/connection"))
                        .build())
                .build();
    }

    private static AtomicInteger allowanceOf(DataFetchingEnvironment environment) {
        return environment.getGraphQlContext().get(PaginationHelper.REMAINING_NODE_ALLOWANCE);
    }

    @Test
    public void shouldPassEveryNodeThroughUnchangedWhileAllowanceLasts() {
        DataFetchingEnvironment environment = environmentWithAllowance(NODES.size());

        List<String> read = PaginationHelper.chargeToRequestBudget(NODES.stream(), environment)
                .collect(Collectors.toList());

        assertEquals(NODES, read);
        assertEquals(0, allowanceOf(environment).get());
    }

    @Test
    public void shouldFailTheRequestOnceTheAllowanceIsSpent() {
        DataFetchingEnvironment environment = environmentWithAllowance(3);

        Stream<String> charged = PaginationHelper.chargeToRequestBudget(NODES.stream(), environment);

        assertThrows(GqlLimitExceededException.class, () -> charged.collect(Collectors.toList()));

        // Refused on the node that would have taken it past the allowance, not at the end of the walk.
        assertEquals(-1, allowanceOf(environment).get());
    }

    @Test
    public void shouldNotBoundAnythingWhenNoAllowanceIsInContext() {
        // 0 in the configuration means unbounded, which reaches here as an absent allowance.
        DataFetchingEnvironment environment = environmentWithAllowance(null);

        List<String> read = PaginationHelper.chargeToRequestBudget(NODES.stream(), environment)
                .collect(Collectors.toList());

        assertEquals(NODES, read);
        assertNull(allowanceOf(environment));
    }

    @Test
    public void shouldChargeOnlyTheNodesActuallyRead() {
        // A connection asked for one page does not walk the whole subtree, and must not be charged as though it had:
        // the allowance is spent on nodes read, so it has to be charged as they are pulled, not up front.
        DataFetchingEnvironment environment = environmentWithAllowance(NODES.size());

        List<String> read = PaginationHelper.chargeToRequestBudget(NODES.stream(), environment)
                .limit(2)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("a", "b"), read);
        assertEquals(NODES.size() - 2, allowanceOf(environment).get());
    }

    @Test
    public void shouldDrawEveryConnectionOfARequestFromTheOneAllowance() {
        // The point of the bound: a nested selection opens one inner connection per node the outer one returned, and
        // each of those is within the per-connection limit. Only a shared allowance bounds their product.
        DataFetchingEnvironment environment = environmentWithAllowance(7);

        PaginationHelper.chargeToRequestBudget(NODES.stream(), environment).collect(Collectors.toList());
        assertEquals(2, allowanceOf(environment).get());

        // The second connection has only what the first left, not a fresh per-connection limit.
        Stream<String> secondConnection = PaginationHelper.chargeToRequestBudget(NODES.stream(), environment);

        assertThrows(GqlLimitExceededException.class, () -> secondConnection.collect(Collectors.toList()));

        assertEquals(-1, allowanceOf(environment).get());
    }

    @Test
    public void shouldReadNothingAndRaiseNothingOnceTheRefusalHasBeenReported() {
        // The documents this bound refuses are the ones that nest connections, so they hold a connection per node the
        // level above returned. Raising the failure again in each of them would answer a request meant to be cut down
        // with an error object per connection - a far bigger response than the one being refused, which is the very
        // thing an amplification attack is after. One refusal is what a caller needs.
        DataFetchingEnvironment environment = environmentWithAllowance(2);

        Stream<String> spendingConnection = PaginationHelper.chargeToRequestBudget(NODES.stream(), environment);

        assertThrows(GqlLimitExceededException.class, () -> spendingConnection.collect(Collectors.toList()));

        // The request has been told once; from here on the remaining connections stay silent.
        List<String> read = PaginationHelper.chargeToRequestBudget(NODES.stream(), environment)
                .collect(Collectors.toList());

        assertEquals(0, read.size());
    }
}
