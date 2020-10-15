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
package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.servlet.GraphQLContext;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.osgi.OSGIServiceInjectorDataFetcher;
import org.jahia.modules.graphql.provider.dxm.security.GqlJcrPermissionDataFetcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * JCR instrumentation implementation
 */
public class JCRInstrumentation extends SimpleInstrumentation {

    public static final String GRAPHQL_VARIABLES = "graphQLVariables";
    public static final String FRAGMENTS_BY_NAME = "fragmentsByName";

    private DXGraphQLConfig dxGraphQLConfig;

    JCRInstrumentation(DXGraphQLConfig dxGraphQLConfig) {
        this.dxGraphQLConfig = dxGraphQLConfig;
    }

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {
        return super.instrumentDataFetcher(
                new GqlJcrPermissionDataFetcher<>(
                        new OSGIServiceInjectorDataFetcher<>(
                                dataFetcher
                        ), 
                        dxGraphQLConfig.getPermissions()), 
                parameters
        );
    }

    @Override
    public ExecutionContext instrumentExecutionContext(ExecutionContext executionContext, InstrumentationExecutionParameters parameters) {

        executionContext = super.instrumentExecutionContext(executionContext, parameters);

        Optional<HttpServletRequest> request = ((GraphQLContext) executionContext.getContext()).getRequest();
        if (!request.isPresent()) {
            // Only the case with integration tests.
            return executionContext;
        }

        HttpServletRequest servletRequest = request.get();
        servletRequest.setAttribute(GRAPHQL_VARIABLES, executionContext.getVariables());
        servletRequest.setAttribute(FRAGMENTS_BY_NAME, executionContext.getFragmentsByName());
        return executionContext;
    }
}
