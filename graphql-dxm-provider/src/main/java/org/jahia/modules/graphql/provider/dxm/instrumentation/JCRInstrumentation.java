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
package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetcher;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.osgi.OSGIServiceInjectorDataFetcher;
import org.jahia.modules.graphql.provider.dxm.security.GqlJcrPermissionChecker;
import org.jahia.modules.graphql.provider.dxm.security.GqlJcrPermissionDataFetcher;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * JCR instrumentation implementation
 */
public class JCRInstrumentation extends SimpleInstrumentation {

    public static final String GRAPHQL_VARIABLES = "graphQLVariables";
    public static final String FRAGMENTS_BY_NAME = "fragmentsByName";

    private static final Logger logger = LoggerFactory.getLogger(JCRInstrumentation.class);

    private final DXGraphQLConfig dxGraphQLConfig;

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
        HttpServletRequest servletRequest = ContextUtil.getHttpServletRequest(executionContext.getContext());

        // Null only in the case with integration tests.
        if (servletRequest != null) {
            servletRequest.setAttribute(GRAPHQL_VARIABLES, executionContext.getCoercedVariables().toMap());
            servletRequest.setAttribute(FRAGMENTS_BY_NAME, executionContext.getFragmentsByName());
        }

        /*
         * Configure introspection through Graphql context based on user permissions
         * We ran into issues with subscription requests being denied due to missing user session.
         * Add filter check to avoid exceptions since introspection can only be done on QUERY operations anyways
         */
        boolean isQueryOperation = OperationDefinition.Operation.QUERY.equals(executionContext.getOperationDefinition().getOperation());
        boolean isIntrospectionCheckEnabled = dxGraphQLConfig.isIntrospectionCheckEnabled();
        logger.debug("isIntrospectionCheckEnabled: {}", isIntrospectionCheckEnabled);
        if (isIntrospectionCheckEnabled && isQueryOperation) {
            GqlJcrPermissionChecker.configureIntrospectionFromPermissions(executionContext.getGraphQLContext());
        }
        return executionContext;
    }
}
