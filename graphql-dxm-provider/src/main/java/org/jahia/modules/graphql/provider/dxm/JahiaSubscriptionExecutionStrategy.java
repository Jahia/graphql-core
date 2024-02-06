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

import graphql.ExecutionResult;
import graphql.execution.*;
import graphql.kickstart.servlet.context.DefaultGraphQLWebSocketContext;
import org.jahia.api.Constants;
import org.jahia.bin.filters.jcr.JcrSessionFilter;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.usermanager.JahiaUser;

import javax.servlet.http.HttpSession;
import java.util.concurrent.CompletableFuture;

public class JahiaSubscriptionExecutionStrategy extends SubscriptionExecutionStrategy {

    public JahiaSubscriptionExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws NonNullableFieldWasNullException {
        try {
            DefaultGraphQLWebSocketContext context = (DefaultGraphQLWebSocketContext) executionContext.getContext();
            JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) context.getSession().getUserProperties().get(Constants.SESSION_USER));

            return super.execute(executionContext, parameters);
        } finally {
            JcrSessionFilter.endRequest();
        }
    }

    @Override
    protected FieldValueInfo completeField(ExecutionContext executionContext, ExecutionStrategyParameters parameters, FetchedValue fetchedValue) {
        boolean resetUser = false;
        if (JCRSessionFactory.getInstance().getCurrentUser() == null) {
            DefaultGraphQLWebSocketContext context = (DefaultGraphQLWebSocketContext) executionContext.getContext();
            JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) context.getSession().getUserProperties().get(Constants.SESSION_USER));
            resetUser = true;
        }

        try {
            return super.completeField(executionContext, parameters, fetchedValue);
        } finally {
            if (resetUser) {
                JcrSessionFilter.endRequest();
            }
        }
    }
}
