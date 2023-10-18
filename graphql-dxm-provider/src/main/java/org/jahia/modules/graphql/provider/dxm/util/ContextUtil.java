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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple utility class to
 */
public class ContextUtil {

    private ContextUtil() {
    }

    /**
     * Get request if http context
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
}
