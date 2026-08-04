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
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLTypeUtil;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Static analysis of a GraphQL document, used by the query-cost guards to reject expensive documents before execution.
 *
 * <p>Every metric comes from the operation's own document - its field selections and the arguments they carry - so they
 * are cheap (no field is fetched) and can be evaluated before anything is permission-checked or serialized. They are
 * produced by a single traversal: the document is walked once per request however many guards are enabled.
 *
 * <p>The first two metrics measure the document's shape and cannot see how much data a field will touch, which is why
 * batch size is measured separately. A field handed an explicit list of things to act on states its own size up front,
 * in an argument, so it is knowable here: that is the one cardinality this class can bound, and it is bounded across the
 * whole document so that aliasing a field cannot multiply it.
 *
 * <p>What none of them bounds is how far a document's list <em>fields</em> fan out, which is the dimension along which
 * cost multiplies rather than adds - a list nested inside a list is evaluated once per item of the outer one. Bounding
 * that statically would need a per-field notion of how many items a field can return, which this schema does not carry:
 * {@code jcr.nodeTypes { nodes { extends { nodes } } }} and {@code descendants { nodes { descendants { nodes } } }} are
 * the same shape over connections with the same arguments, yet the first reads a few hundred node type definitions out
 * of an in-memory registry and the second can read millions of JCR nodes. Fan-out is bounded per connection at execution
 * time instead, by {@code graphql.fields.node.limit}, and a query-driven mutation the same way - how many nodes a
 * JCR-SQL2 statement matches is not knowable until it runs.
 */
final class QueryCostCalculator {

    /** Input types whose items are nodes. */
    private static final Set<String> NODE_INPUT_TYPES = new HashSet<>(
            Arrays.asList("InputJCRNode", "InputJCRNodeWithParent", "InputCarriedJCRNode"));
    /** Argument naming the nodes a mutation targets. */
    private static final String NODE_PATHS_ARGUMENT = "pathsOrIds";
    /** Field under which a node input nests further nodes. */
    private static final String NESTED_NODES_FIELD = "children";

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
     * @param traverser     traverser over the operation being analysed
     * @param batchCeiling  batch size above which the count may stop, 0 to skip measuring it
     * @return the measured cost
     */
    static QueryCost calculate(QueryTraverser traverser, int batchCeiling) {
        Measurement measurement = new Measurement(batchCeiling);
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
        private final int batchSize;

        private QueryCost(int complexity, int depth, int batchSize) {
            this.complexity = complexity;
            this.depth = depth;
            this.batchSize = batchSize;
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

        /**
         * @return how many items the document hands the operation's fields in list arguments, summed over every
         *         selection. Unlike the two metrics above this counts input rather than selections, which is what makes
         *         it able to size a mutation batch: how many items a field is handed is stated in its arguments, not in
         *         the shape of the document. Summing over selections is deliberate - the bound is on what one request
         *         asks for in total, so selecting a field several times under aliases does not raise it - and nested
         *         input objects are followed, so items carried inside a recursive input type count the same.
         */
        int getBatchSize() {
            return batchSize;
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
        private final int batchCeiling;

        private int complexity;
        private int depth;
        private int batchSize;

        private Measurement(int batchCeiling) {
            this.batchCeiling = batchCeiling;
        }

        private void measure(QueryVisitorFieldEnvironment env) {
            complexity++;
            depth = Math.max(depth, depthOf(env));
            if (batchCeiling > 0 && batchSize <= batchCeiling) {
                batchSize += nodeItems(env);
            }
        }

        private QueryCost result() {
            return new QueryCost(complexity, depth, batchSize);
        }

        /**
         * How many nodes this field was handed in its arguments.
         * <p>
         * Only arguments that denote nodes count: a list of one of the node input types, or a {@code pathsOrIds} list.
         * Lists of anything else - property values, mixin names, role names, languages - are cardinality of a different
         * kind and must not consume a node allowance. Arguments arrive coerced, so a list passed in a variable is
         * measured like an inline one.
         */
        private int nodeItems(QueryVisitorFieldEnvironment env) {
            GraphQLFieldDefinition field = env.getFieldDefinition();
            if (field == null) {
                return 0;
            }
            int items = 0;
            for (Map.Entry<String, Object> argument : env.getArguments().entrySet()) {
                GraphQLArgument definition = field.getArgument(argument.getKey());
                if (definition == null) {
                    continue;
                }
                String type = GraphQLTypeUtil.unwrapAll(definition.getType()).getName();
                if (NODE_INPUT_TYPES.contains(type)) {
                    items += countNodes(argument.getValue());
                } else if (NODE_PATHS_ARGUMENT.equals(argument.getKey()) && argument.getValue() instanceof Collection) {
                    items += ((Collection<?>) argument.getValue()).size();
                }
            }
            return items;
        }

        /**
         * Counts a node-input value and the nodes nested under it. Iterative, and stops once the running total is past
         * the ceiling: a node input holds a list of itself, so the nesting an argument can carry is caller-controlled and
         * recursion here would be a stack-depth risk on a request that is going to be rejected anyway.
         */
        private int countNodes(Object value) {
            int items = 0;
            Deque<Object> pending = new ArrayDeque<>();
            pending.push(value);
            while (!pending.isEmpty() && items <= batchCeiling) {
                Object current = pending.pop();
                if (current instanceof Collection) {
                    Collection<?> nodes = (Collection<?>) current;
                    items += nodes.size();
                    for (Object node : nodes) {
                        pending.push(node);
                    }
                } else if (current instanceof Map) {
                    Object children = ((Map<?, ?>) current).get(NESTED_NODES_FIELD);
                    if (children != null) {
                        pending.push(children);
                    }
                }
            }
            return items;
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
