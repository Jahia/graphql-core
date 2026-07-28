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
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aborts execution when a document nests list-typed fields deeper than the configured limit.
 *
 * <p>Recursive fields such as {@code descendants} and {@code children} return a connection over the same type they are
 * declared on, so a document can nest them repeatedly. Each level is evaluated once per item of the level above it, so
 * a handful of levels expands into a very large, expensive response from a small request - overlapping subtrees get
 * re-walked and re-serialized at every level.
 *
 * <p>Neither of the other guards bounds that expansion: the complexity guard counts the fields written in the document
 * (a deeply nested document is textually small), the depth guard counts every field and so has to be set generously
 * enough for legitimate documents that are deep but cheap, and {@code graphql.fields.node.limit} caps each connection
 * individually rather than their product. This guard bounds exactly the nesting that multiplies.
 *
 * @see QueryCostCalculator#calculateMaxListNesting(graphql.analysis.QueryTraverser)
 */
public class MaxListNestingInstrumentation extends SimplePerformantInstrumentation {

    private static final Logger logger = LoggerFactory.getLogger(MaxListNestingInstrumentation.class);

    private final int maxListNesting;

    public MaxListNestingInstrumentation(int maxListNesting) {
        this.maxListNesting = maxListNesting;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters, InstrumentationState state) {
        int listNesting = QueryCostCalculator.calculateMaxListNesting(
                QueryCostCalculator.newTraverser(parameters.getExecutionContext()));
        if (logger.isDebugEnabled()) {
            logger.debug("Query list nesting: {}", listNesting);
        }
        if (listNesting > maxListNesting) {
            throw new AbortExecutionException("maximum query list nesting exceeded " + listNesting + " > " + maxListNesting);
        }
        return SimpleInstrumentationContext.noOp();
    }
}
