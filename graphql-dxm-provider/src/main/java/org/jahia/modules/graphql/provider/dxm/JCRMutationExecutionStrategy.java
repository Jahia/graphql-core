package org.jahia.modules.graphql.provider.dxm;

import graphql.ExecutionResult;
import graphql.execution.*;
import org.jahia.modules.graphql.provider.dxm.node.NodeMutationExtensions;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;

import java.util.concurrent.CompletableFuture;

public class JCRMutationExecutionStrategy extends AsyncSerialExecutionStrategy {

    public JCRMutationExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws NonNullableFieldWasNullException {
        return super.execute(executionContext, parameters);
    }


    @Override
    protected CompletableFuture<ExecutionResult> completeField(ExecutionContext executionContext, ExecutionStrategyParameters parameters, Object fetchedValue) {
        CompletableFuture<ExecutionResult> result = super.completeField(executionContext, parameters, fetchedValue);
        if (fetchedValue instanceof NodeMutationExtensions.GraphQLMutationJCR) {
            ((NodeMutationExtensions.GraphQLMutationJCR)fetchedValue).save();
        }
        return result;
    }


}
