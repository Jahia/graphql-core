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
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.securityfilter.PermissionService;
import org.jahia.services.securityfilter.ScopeDefinition;
import org.jahia.services.usermanager.JahiaUser;

import javax.servlet.http.HttpSession;
import javax.websocket.Session;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class JahiaSubscriptionExecutionStrategy extends SubscriptionExecutionStrategy {

    public JahiaSubscriptionExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws NonNullableFieldWasNullException {
        try {
            Session session = executionContext.getGraphQLContext().get(Session.class);
            JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) session.getUserProperties().get(Constants.SESSION_USER));
            restoreScopes(session);

            return super.execute(executionContext, parameters);
        } finally {
            resetScopes();
            JcrSessionFilter.endRequest();
        }
    }

    @Override
    protected FieldValueInfo completeField(ExecutionContext executionContext, ExecutionStrategyParameters parameters, FetchedValue fetchedValue) {
        boolean resetUser = false;
        if (JCRSessionFactory.getInstance().getCurrentUser() == null) {
            Session session = executionContext.getGraphQLContext().get(Session.class);
            JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) session.getUserProperties().get(Constants.SESSION_USER));
            restoreScopes(session);
            resetUser = true;
        }

        try {
            return super.completeField(executionContext, parameters, fetchedValue);
        } finally {
            if (resetUser) {
                resetScopes();
                JcrSessionFilter.endRequest();
            }
        }
    }

    /**
     * Restore the authorization scopes captured for this connection at handshake time (see
     * {@link OsgiGraphQLWsEndpoint#SESSION_SCOPES}) onto the subscription execution thread, so
     * subscription data fetchers apply the same permission checks as HTTP requests. This mirrors the
     * scope propagation the HTTP query/mutation executor already performs. An anonymous connection
     * carries no privileged scope; an authenticated one keeps exactly its own scopes.
     */
    @SuppressWarnings("unchecked")
    private void restoreScopes(Session session) {
        PermissionService permissionService = BundleUtils.getOsgiService(PermissionService.class, null);
        if (permissionService == null) {
            return;
        }
        Collection<ScopeDefinition> scopes = (Collection<ScopeDefinition>) session.getUserProperties().get(OsgiGraphQLWsEndpoint.SESSION_SCOPES);
        if (scopes != null) {
            permissionService.setCurrentScopes(scopes);
        } else {
            permissionService.resetScopes();
        }
    }

    private void resetScopes() {
        PermissionService permissionService = BundleUtils.getOsgiService(PermissionService.class, null);
        if (permissionService != null) {
            permissionService.resetScopes();
        }
    }
}
