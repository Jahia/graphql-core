/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm;

import graphql.ExecutionResult;
import graphql.execution.*;
import graphql.kickstart.servlet.context.DefaultGraphQLWebSocketContext;
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
            HttpSession httpSession = (HttpSession) context.getSession().getUserProperties().get(HttpSession.class.getName());
            JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) httpSession.getAttribute("org.jahia.usermanager.jahiauser"));
            return super.execute(executionContext, parameters);
        } finally {
            JcrSessionFilter.endRequest();
        }
    }

    @Override
    protected FieldValueInfo completeField(ExecutionContext executionContext, ExecutionStrategyParameters parameters, FetchedValue fetchedValue) {
        try {
            DefaultGraphQLWebSocketContext context = (DefaultGraphQLWebSocketContext) executionContext.getContext();
            HttpSession httpSession = (HttpSession) context.getSession().getUserProperties().get(HttpSession.class.getName());
            JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) httpSession.getAttribute("org.jahia.usermanager.jahiauser"));
            return super.completeField(executionContext, parameters, fetchedValue);
        } finally {
            JcrSessionFilter.endRequest();
        }
    }
}
