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
package org.jahia.modules.graphql.provider.dxm;

import graphql.execution.ExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.kickstart.execution.config.ExecutionStrategyProvider;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class JahiaExecutionStrategyProvider implements ExecutionStrategyProvider {

    private final ExecutionStrategy queryStrategy;
    private final ExecutionStrategy mutationStrategy;
    private final ExecutionStrategy subscriptionExecutionStrategy;

    public JahiaExecutionStrategyProvider() {
        queryStrategy = new JahiaQueryExecutionStrategy(new JahiaDataFetchingExceptionHandler());
        mutationStrategy = new JahiaMutationExecutionStrategy(new JahiaDataFetchingExceptionHandler());
        subscriptionExecutionStrategy = new JahiaSubscriptionExecutionStrategy(new JahiaDataFetchingExceptionHandler());
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
