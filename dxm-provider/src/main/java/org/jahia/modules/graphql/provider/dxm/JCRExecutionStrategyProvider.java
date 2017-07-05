package org.jahia.modules.graphql.provider.dxm;

import graphql.execution.ExecutionStrategy;
import graphql.servlet.ExecutionStrategyProvider;
import org.osgi.service.component.annotations.Component;

@Component
public class JCRExecutionStrategyProvider implements ExecutionStrategyProvider {
    @Override
    public ExecutionStrategy getQueryExecutionStrategy() {
        return new JCRExecutionStrategy();
    }

    @Override
    public ExecutionStrategy getMutationExecutionStrategy() {
        return new JCRExecutionStrategy();
    }

    @Override
    public ExecutionStrategy getSubscriptionExecutionStrategy() {
        return new JCRExecutionStrategy();
    }
}
