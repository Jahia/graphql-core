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
package org.jahia.modules.graphql.provider.dxm.util;

import graphql.GraphQLContext;
import graphql.kickstart.execution.context.DefaultGraphQLContext;
import graphql.kickstart.servlet.context.GraphQLServletContext;
import graphql.language.OperationDefinition;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple utility class to
 */
public class ContextUtil {

    private static final Logger log = LoggerFactory.getLogger(ContextUtil.class);
    private static final String LIVE_OPERATION_HEADER = "X-Jahia-Live-Operation";

    private ContextUtil() {
    }

    /**
     * Get request if http context
     *
     * @param context context
     * @return response
     */
    public static HttpServletRequest getHttpServletRequest(Object context) {
        if (context instanceof GraphQLContext) {
            return ((GraphQLContext) context).get(HttpServletRequest.class);
        }

        if (context instanceof DefaultGraphQLContext) {
            return (HttpServletRequest) ((DefaultGraphQLContext) context).getMapOfContext().get(HttpServletRequest.class);
        }

        if (context instanceof GraphQLServletContext) {
            return ((GraphQLServletContext) context).getHttpServletRequest();
        }

        return null;
    }

    /**
     * Get response if http context
     *
     * @param context context
     * @return response
     */
    public static HttpServletResponse getHttpServletResponse(Object context) {
        if (context instanceof GraphQLContext) {
            return ((GraphQLContext) context).get(HttpServletResponse.class);
        }

        if (context instanceof DefaultGraphQLContext) {
            return (HttpServletResponse) ((DefaultGraphQLContext) context).getMapOfContext().get(HttpServletResponse.class);
        }

        if (context instanceof GraphQLServletContext) {
            return ((GraphQLServletContext) context).getHttpServletResponse();
        }

        return null;
    }

    /**
     * Conditionally sets the HTTP header {@value LIVE_OPERATION_HEADER} on the response if the specified workspace is "live".
     * The header value will be the lower-case name of the provided GraphQL operation.
     * <p>
     * The HTTP response is retrieved from the given {@link GraphQLContext}.
     *
     * @param workspace      the JCR workspace to check
     * @param operation      the GraphQL operation (query, mutation, or subscription)
     * @param graphQLContext the GraphQL context used to obtain the {@link HttpServletResponse}
     */
    public static void setJcrLiveOperationHeaderIfNeeded(NodeQueryExtensions.Workspace workspace, OperationDefinition.Operation operation, GraphQLContext graphQLContext) {
        setJcrLiveOperationHeaderIfNeeded(workspace, operation, getHttpServletResponse(graphQLContext));
    }

    /**
     * Conditionally sets the HTTP header {@value LIVE_OPERATION_HEADER} on the provided response if the specified workspace is "live".
     * The header value will be the lower-case name of the provided GraphQL operation.
     *
     * @param workspace the JCR workspace to check
     * @param operation the GraphQL operation (query, mutation, or subscription)
     * @param response  the HTTP response on which to set the header (can be {@code null})
     */
    public static void setJcrLiveOperationHeaderIfNeeded(NodeQueryExtensions.Workspace workspace, OperationDefinition.Operation operation, HttpServletResponse response) {
        if (response != null && NodeQueryExtensions.Workspace.LIVE.equals(workspace)) {
            String operationName = operation.name().toLowerCase();
            log.debug("The 'live' workspace is used, setting the http header {}: {}", LIVE_OPERATION_HEADER, operationName);
            response.setHeader(LIVE_OPERATION_HEADER, operationName);
        }
    }
}
