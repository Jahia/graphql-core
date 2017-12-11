package org.jahia.modules.graphql.provider.dxm;

import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.ExecutionStrategy;
import graphql.servlet.ExecutionStrategyProvider;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class JCRExecutionStrategyProvider implements ExecutionStrategyProvider {

    private final ExecutionStrategy queryStrategy;
    private final ExecutionStrategy mutationStrategy;
    private final ExecutionStrategy subscriptionExecutionStrategy;

    public JCRExecutionStrategyProvider() {
        queryStrategy = new AsyncExecutionStrategy(new JCRDataFetchingExceptionHandler());
        mutationStrategy = new JCRMutationExecutionStrategy(new JCRDataFetchingExceptionHandler());
        subscriptionExecutionStrategy = queryStrategy;
    }

    @Override
    public ExecutionStrategy getQueryExecutionStrategy() {
        return queryStrategy;
    }

    @Override
    public ExecutionStrategy getMutationExecutionStrategy() {
        return mutationStrategy;
    }

    @Override
    public ExecutionStrategy getSubscriptionExecutionStrategy() {
        return subscriptionExecutionStrategy;
    }
}
