package org.jahia.modules.graphql.provider.dxm;

import graphql.execution.ExecutionStrategy;
import graphql.servlet.ExecutionStrategyProvider;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class JCRExecutionStrategyProvider implements ExecutionStrategyProvider {

    private final JCRExecutionStrategy executionStrategy;

    public JCRExecutionStrategyProvider() {
        executionStrategy = new JCRExecutionStrategy();
    }

    @Override
    public ExecutionStrategy getQueryExecutionStrategy() {
        return executionStrategy;
    }

    @Override
    public ExecutionStrategy getMutationExecutionStrategy() {
        return executionStrategy;
    }

    @Override
    public ExecutionStrategy getSubscriptionExecutionStrategy() {
        return executionStrategy;
    }
}
