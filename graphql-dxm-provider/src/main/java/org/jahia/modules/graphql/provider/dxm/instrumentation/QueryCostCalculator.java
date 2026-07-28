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
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static analysis of a GraphQL document, used by the query-cost guards to reject expensive documents before execution.
 *
 * <p>Both metrics are computed from the operation's field selections only, so they are cheap (no field is fetched) and
 * they can be evaluated before anything is permission-checked or serialized.
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
     * Computes the complexity of an operation, where every field counts as 1 plus the complexity of its sub-selection.
     *
     * <p>This deliberately replaces graphql-java's {@code QueryComplexityCalculator}, which scores
     * {@code __typename} as 0 regardless of the {@code FieldComplexityCalculator} it is given (the exemption lives in
     * a private method, so it cannot be overridden). That exemption is what let an unauthenticated document aliasing
     * {@code __typename} a few thousand times score 0 and pass any budget, while still costing one permission check
     * and one serialized error object per alias. Aliases are distinct selections, so they are counted individually.
     *
     * @param traverser traverser over the operation being analysed
     * @return the total complexity of the operation
     */
    static int calculateComplexity(QueryTraverser traverser) {
        // Post-order accumulation: each field adds its own cost to its parent's running total, so by the time a field
        // is visited its children have already contributed. The null key holds the operation's root-level total.
        Map<QueryVisitorFieldEnvironment, Integer> childComplexityByParent = new LinkedHashMap<>();
        traverser.visitPostOrder(new QueryVisitorStub() {
            @Override
            public void visitField(QueryVisitorFieldEnvironment env) {
                int complexity = 1 + childComplexityByParent.getOrDefault(env, 0);
                childComplexityByParent.merge(env.getParentEnvironment(), complexity, Integer::sum);
            }
        });
        return childComplexityByParent.getOrDefault(null, 0);
    }

    /**
     * Computes how deeply list-typed fields are nested in an operation, i.e. the largest number of list fields found on
     * any single path from the operation root down to a leaf.
     *
     * <p>This is the dimension along which cost grows multiplicatively: a list nested inside a list is evaluated once
     * per item of the outer list, so {@code descendants { nodes { descendants { nodes { ... } } } }} re-walks and
     * re-serializes overlapping subtrees at every level. Neither the complexity guard (which only counts the fields
     * that appear in the document, not the items they expand to) nor the per-connection node limit bounds that
     * product. Plain nesting depth is a poor proxy because it also grows on wide-but-cheap documents, so this metric
     * counts list fields exclusively.
     *
     * <p>Only lists of composite types count. A list of scalars (a multi-valued property's {@code values}, say) is a
     * leaf: it fans out once and cannot recurse, so charging it would penalise ordinary documents without bounding
     * anything.
     *
     * @param traverser traverser over the operation being analysed
     * @return the maximum number of nested list fields on any path, 0 if the operation selects no list field
     */
    static int calculateMaxListNesting(QueryTraverser traverser) {
        int[] maxNesting = {0};
        traverser.visitPreOrder(new QueryVisitorStub() {
            @Override
            public void visitField(QueryVisitorFieldEnvironment env) {
                // Walking up the parent chain per field keeps this independent of visit order and of how the
                // traverser shares environment instances between a field and its children.
                int nesting = 0;
                for (QueryVisitorFieldEnvironment current = env; current != null; current = current.getParentEnvironment()) {
                    if (isListOfCompositeType(current.getFieldDefinition().getType())) {
                        nesting++;
                    }
                }
                maxNesting[0] = Math.max(maxNesting[0], nesting);
            }
        });
        return maxNesting[0];
    }

    private static boolean isListOfCompositeType(GraphQLType type) {
        // A list may be wrapped in non-null ([JCRNode]! is NonNull(List(JCRNode))), so unwrap that first; unwrapAll
        // then reaches the element type through any further list/non-null wrappers.
        if (!GraphQLTypeUtil.isList(GraphQLTypeUtil.unwrapNonNull(type))) {
            return false;
        }
        return GraphQLTypeUtil.unwrapAll(type) instanceof GraphQLCompositeType;
    }
}
