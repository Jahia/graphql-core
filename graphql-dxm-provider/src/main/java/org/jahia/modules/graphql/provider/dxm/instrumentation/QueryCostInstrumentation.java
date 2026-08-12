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
import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionContext;
import graphql.language.OperationDefinition;
import org.jahia.modules.graphql.provider.dxm.config.GraphQLLimits;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aborts execution when a document is too expensive, along either of the dimensions {@link QueryCostCalculator}
 * measures: how many fields it selects, and how deeply it nests them.
 *
 * <p>The document is measured once and both limits are checked against that one measurement, so enabling the second
 * guard does not walk the document again. That matters because the measurement happens on every request, before the
 * operation is authorized, and because expanding a document is itself work an attacker controls: graphql-java's
 * traverser re-expands a fragment definition at each spread of it, so a guard that traverses repeatedly multiplies
 * exactly the cost it exists to bound.
 *
 * <p>It also opens the request's node allowance, the one bound here that is not a verdict on the document: the guards
 * above reject a selection for its shape, whereas the allowance limits how far that shape may expand once it is walking
 * actual content, and so can only be spent as the fields run.
 *
 * <p>A limit of 0 or less disables that individual check. The messages keep graphql-java's wording, whose enforcement
 * this class took over from {@link graphql.analysis.MaxQueryComplexityInstrumentation} and
 * {@link graphql.analysis.MaxQueryDepthInstrumentation}, so existing clients and log filters are unaffected.
 */
public class QueryCostInstrumentation extends SimplePerformantInstrumentation {

    private static final Logger logger = LoggerFactory.getLogger(QueryCostInstrumentation.class);

    private final int maxComplexity;
    private final int maxDepth;
    private final int maxBatchSize;
    private final int maxNodesPerRequest;

    /**
     * @param maxComplexity      maximum number of selected fields, 0 to disable that check
     * @param maxDepth           maximum nesting depth, 0 to disable that check
     * @param maxBatchSize       maximum number of items a mutation may be handed in list arguments across the whole
     *                           document, 0 to disable that check
     * @param maxNodesPerRequest maximum number of nodes the request's fields may walk in total, 0 to disable that bound
     */
    public QueryCostInstrumentation(int maxComplexity, int maxDepth, int maxBatchSize, int maxNodesPerRequest) {
        this.maxComplexity = maxComplexity;
        this.maxDepth = maxDepth;
        this.maxBatchSize = maxBatchSize;
        this.maxNodesPerRequest = maxNodesPerRequest;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters, InstrumentationState state) {
        ExecutionContext executionContext = parameters.getExecutionContext();
        // Batch size is only enforced on mutations, so only measured there: a query would pay for a count it never uses.
        int batchCeiling = isMutation(executionContext) ? maxBatchSize : 0;
        QueryCostCalculator.QueryCost cost =
                QueryCostCalculator.calculate(QueryCostCalculator.newTraverser(executionContext), batchCeiling);
        if (logger.isDebugEnabled()) {
            logger.debug("Query cost: complexity {}, depth {}, batch size {}",
                    cost.getComplexity(), cost.getDepth(), cost.getBatchSize());
        }
        if (maxComplexity > 0 && cost.getComplexity() > maxComplexity) {
            throw new AbortExecutionException(
                    "maximum query complexity exceeded " + cost.getComplexity() + " > " + maxComplexity);
        }
        if (maxDepth > 0 && cost.getDepth() > maxDepth) {
            throw new AbortExecutionException(
                    "maximum query depth exceeded " + cost.getDepth() + " > " + maxDepth);
        }
        // Mutations only: the items are things to write, committed together in one JCR session. A query handed a list of
        // paths to read is bounded per connection at execution time instead.
        if (batchCeiling > 0) {
            if (cost.getBatchSize() > maxBatchSize) {
                throw new AbortExecutionException(
                        "maximum mutation batch size exceeded " + cost.getBatchSize() + " > " + maxBatchSize);
            }
            // Hand the rest of the allowance to the fields whose cardinality could not be measured here, so that a
            // query-driven mutation draws from what this request has left instead of from the full limit each time.
            executionContext.getGraphQLContext().put(GraphQLLimits.REMAINING_BATCH_ALLOWANCE,
                    new AtomicInteger(maxBatchSize - cost.getBatchSize()));
        }
        // Opens the request's node allowance, which the connections draw down as they walk the repository. Unlike the
        // three bounds above this one cannot be settled here: how many nodes a field reaches is a property of the
        // content, not of the document, and only becomes known as the fields run. What the document analysis above
        // can bound is the shape of a selection; what this bounds is how far that shape expands once pointed at a
        // repository - the dimension along which a small, shallow, legal document still multiplies out.
        if (maxNodesPerRequest > 0) {
            executionContext.getGraphQLContext().put(PaginationHelper.REMAINING_NODE_ALLOWANCE,
                    new AtomicInteger(maxNodesPerRequest));
        }
        return SimpleInstrumentationContext.noOp();
    }

    private static boolean isMutation(ExecutionContext executionContext) {
        OperationDefinition operation = executionContext.getOperationDefinition();
        return operation != null && OperationDefinition.Operation.MUTATION.equals(operation.getOperation());
    }
}
