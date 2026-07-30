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

import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.ExecutionContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Static analysis of a GraphQL document, used by the query-cost guards to reject expensive documents before execution.
 *
 * <p>Both metrics come from the operation's field selections only, so they are cheap (no field is fetched) and can be
 * evaluated before anything is permission-checked or serialized. They are produced by a single traversal: the document
 * is walked once per request whether one guard is enabled or both.
 *
 * <p>Neither metric bounds how far a document's list fields fan out, which is the dimension along which cost multiplies
 * rather than adds - a list nested inside a list is evaluated once per item of the outer one. Bounding that statically
 * would need a per-field notion of how many items a field can return, which this schema does not carry: {@code
 * jcr.nodeTypes { nodes { extends { nodes } } }} and {@code descendants { nodes { descendants { nodes } } }} are the
 * same shape over connections with the same arguments, yet the first reads a few hundred node type definitions out of
 * an in-memory registry and the second can read millions of JCR nodes. Fan-out is bounded per connection at execution
 * time instead, by {@code graphql.fields.node.limit}.
 */
final class QueryCostCalculator {

    private QueryCostCalculator() {
    }

    static QueryTraverser newTraverser(ExecutionContext executionContext) {
        return QueryTraverser.newQueryTraverser()
                .schema(executionContext.getGraphQLSchema())
                .document(executionContext.getDocument())
                .operationName(executionContext.getExecutionInput().getOperationName())
                .coercedVariables(executionContext.getCoercedVariables())
                .build();
    }

    /**
     * Measures an operation.
     *
     * @param traverser traverser over the operation being analysed
     * @return the measured cost
     */
    static QueryCost calculate(QueryTraverser traverser) {
        Measurement measurement = new Measurement();
        // Pre-order, so that a field is always measured after the parent it hangs off: that makes its depth one step
        // from the parent's rather than a walk back up to the root, which keeps the analysis linear in the size of the
        // document. Pre-order also skips fields excluded by @skip/@include, which are never going to execute.
        traverser.visitPreOrder(new QueryVisitorStub() {
            @Override
            public void visitField(QueryVisitorFieldEnvironment env) {
                measurement.measure(env);
            }
        });
        return measurement.result();
    }

    /**
     * What one document costs, along the two dimensions the guards bound. Kept together because a single traversal
     * produces both.
     */
    static final class QueryCost {

        private final int complexity;
        private final int depth;

        private QueryCost(int complexity, int depth) {
            this.complexity = complexity;
            this.depth = depth;
        }

        /**
         * @return the number of fields the document selects, where every field counts as 1 plus the complexity of its
         *         sub-selection. Aliases are distinct selections and count individually, and so do meta fields such as
         *         {@code __typename}: each one still costs a permission check and, when denied, an error object in the
         *         response. This deliberately differs from graphql-java's {@code QueryComplexityCalculator}, which
         *         scores {@code __typename} as 0 whatever {@code FieldComplexityCalculator} it is given - the exemption
         *         sits in a private method - so that a document aliasing it a few thousand times scores 0 and passes
         *         any budget.
         */
        int getComplexity() {
            return complexity;
        }

        /**
         * @return the number of fields on the longest path from the operation root down to a leaf. This matches the
         *         depth graphql-java's {@code MaxQueryDepthInstrumentation} reports, so a configured maximum keeps the
         *         meaning it had when that instrumentation enforced it.
         */
        int getDepth() {
            return depth;
        }
    }

    /**
     * Accumulates the metrics over one traversal. Each field's depth is memoized so that it is charged from its
     * parent's already-known depth instead of walking up to the root, which would make the analysis quadratic in the
     * size of a deeply nested document - and the analysis runs on every request, before the operation is authorized.
     * The memo is keyed by identity: the traverser hands a field's children the very environment instance it passed to
     * the visitor, while environment equality is value-based, so two sibling selections of the same field with the same
     * arguments compare equal yet have to be measured separately.
     */
    private static final class Measurement {

        private final Map<QueryVisitorFieldEnvironment, Integer> depthByField = new IdentityHashMap<>();

        private int complexity;
        private int depth;

        private void measure(QueryVisitorFieldEnvironment env) {
            complexity++;
            depth = Math.max(depth, depthOf(env));
        }

        private QueryCost result() {
            return new QueryCost(complexity, depth);
        }

        private int depthOf(QueryVisitorFieldEnvironment env) {
            Integer known = depthByField.get(env);
            if (known != null) {
                return known;
            }
            // Collect the ancestors not measured yet, then fill them in downwards. Under a pre-order traversal the
            // parent is always known already and this walks a single field, but doing it this way keeps the result
            // independent of the visit order.
            Deque<QueryVisitorFieldEnvironment> pending = new ArrayDeque<>();
            QueryVisitorFieldEnvironment ancestor = env;
            while (ancestor != null && !depthByField.containsKey(ancestor)) {
                pending.push(ancestor);
                ancestor = ancestor.getParentEnvironment();
            }
            int fieldDepth = ancestor == null ? 0 : depthByField.get(ancestor);
            while (!pending.isEmpty()) {
                fieldDepth++;
                depthByField.put(pending.pop(), fieldDepth);
            }
            return fieldDepth;
        }
    }
}
