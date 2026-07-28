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
 * Aborts execution when a document's complexity exceeds the configured budget, where every field counts as 1 plus the
 * complexity of its sub-selection.
 *
 * <p>Replaces graphql-java's {@link graphql.analysis.MaxQueryComplexityInstrumentation}, which exempts
 * {@code __typename} from the count and therefore scores a document that aliases it thousands of times as 0 - passing
 * any budget while still triggering one permission check and one serialized error object per alias. The abort message
 * keeps graphql-java's wording so existing clients and log filters are unaffected.
 *
 * @see QueryCostCalculator#calculateComplexity(graphql.analysis.QueryTraverser)
 */
public class JahiaMaxQueryComplexityInstrumentation extends SimplePerformantInstrumentation {

    private static final Logger logger = LoggerFactory.getLogger(JahiaMaxQueryComplexityInstrumentation.class);

    private final int maxComplexity;

    public JahiaMaxQueryComplexityInstrumentation(int maxComplexity) {
        this.maxComplexity = maxComplexity;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters, InstrumentationState state) {
        int totalComplexity = QueryCostCalculator.calculateComplexity(
                QueryCostCalculator.newTraverser(parameters.getExecutionContext()));
        if (logger.isDebugEnabled()) {
            logger.debug("Query complexity: {}", totalComplexity);
        }
        if (totalComplexity > maxComplexity) {
            throw new AbortExecutionException("maximum query complexity exceeded " + totalComplexity + " > " + maxComplexity);
        }
        return SimpleInstrumentationContext.noOp();
    }
}
