package org.jahia.modules.graphql.provider.dxm;

import graphql.ExecutionResult;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.NonNullableFieldWasNullException;

import java.util.concurrent.CompletableFuture;

/**
 * Custom Execution Strategy
 */
public class JCRExecutionStrategy extends AsyncExecutionStrategy {


    public JCRExecutionStrategy() {
        super (new JCRDataFetchingExceptionHandler());
    }

    @Override
    protected CompletableFuture<ExecutionResult> completeValue(ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws NonNullableFieldWasNullException {
        return super.completeValue(executionContext, parameters);
    }



}
