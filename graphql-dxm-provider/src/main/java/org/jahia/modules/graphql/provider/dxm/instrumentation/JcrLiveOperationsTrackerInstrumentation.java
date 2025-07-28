/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
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

import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNamedType;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions.Workspace;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.modules.graphql.provider.dxm.util.GqlTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions.WORKSPACE_PARAM_NAME;

/**
 * Custom instrumentation to track operations (queries and mutations) on the live JCR workspace.
 */
public class JcrLiveOperationsTrackerInstrumentation implements Instrumentation {
    private static final Logger log = LoggerFactory.getLogger(JcrLiveOperationsTrackerInstrumentation.class);

    private static final String LIVE_OPERATION_HEADER = "X-Jahia-Live-Operation";
    private static final String QUERY_OPERATION = OperationDefinition.Operation.QUERY.name().toLowerCase();
    private static final String MUTATION_OPERATION = OperationDefinition.Operation.MUTATION.name().toLowerCase();

    @Override
    public InstrumentationState createState(InstrumentationCreateStateParameters parameters) {
        return new TrackerInstrumentationState();
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(InstrumentationFieldParameters parameters, InstrumentationState state) {
        GraphQLFieldDefinition field = parameters.getField();
        GraphQLNamedType unwrappedType = GqlTypeUtil.unwrapType(field.getType());
        TrackerInstrumentationState customState = (TrackerInstrumentationState) state;
        Workspace workspace = (Workspace) Optional.ofNullable(parameters.getExecutionStepInfo().getArgument(WORKSPACE_PARAM_NAME)).orElseGet(() -> getDefaultValue(parameters));
        if (Workspace.LIVE.equals(workspace)) {
            log.debug("'live' workspace used for field '{}'", field.getName());
            switch (unwrappedType.getName()) {
                case GqlJcrQuery.NAME:
                    customState.hasLiveQuery = true;
                    break;
                case GqlJcrMutation.NAME:
                    customState.hasLiveMutation = true;
                    break;
                default:
            }
        }
        return Instrumentation.super.beginField(parameters, state);
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        TrackerInstrumentationState jcrTrackerInstrumentationState = (TrackerInstrumentationState) state;
        HttpServletResponse response = ContextUtil.getHttpServletResponse(parameters.getGraphQLContext());
        if (response != null && !response.isCommitted()) {
            // Set the header only if the live workspace is used
            if (jcrTrackerInstrumentationState.hasLiveQuery) {
                log.debug("The GraphQL request contains a query on the live JCR workspace, setting the http header {} :{}", LIVE_OPERATION_HEADER, QUERY_OPERATION);
                response.setHeader(LIVE_OPERATION_HEADER, QUERY_OPERATION);
            } else if (jcrTrackerInstrumentationState.hasLiveMutation) {
                log.debug("The GraphQL request contains a mutation on the live JCR workspace, setting the http header {} :{}", LIVE_OPERATION_HEADER, MUTATION_OPERATION);
                response.setHeader(LIVE_OPERATION_HEADER, MUTATION_OPERATION);
            }
        }
        return Instrumentation.super.instrumentExecutionResult(executionResult, parameters, state);
    }

    private static Object getDefaultValue(InstrumentationFieldParameters parameters) {
        GraphQLArgument workspaceArgument = parameters.getField().getArgument(WORKSPACE_PARAM_NAME);
        return workspaceArgument == null ? null : workspaceArgument.getArgumentDefaultValue().getValue();
    }

    /**
     * Track details about a GraphQL request being executed
     */
    private static class TrackerInstrumentationState implements InstrumentationState {
        private boolean hasLiveQuery;
        private boolean hasLiveMutation;
    }
}
