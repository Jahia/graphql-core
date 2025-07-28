/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions.Workspace;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TrackerInstrumentation implements Instrumentation {
    private static final Logger log = LoggerFactory.getLogger(TrackerInstrumentation.class);
    /**
     * Should match the name defined in {@link org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery}.
     */
    private static final String JCR_FIELD_TYPE = "JCRQuery!";
    /**
     * Should match the name of the <code>workspace</code> parameter in {@link org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions#getJcr(Workspace)} and {@link org.jahia.modules.graphql.provider.dxm.node.NodeMutationExtensions#getJcr(Workspace, boolean)}
     */
    private static final String WORKSPACE_ARGUMENT = "workspace";
    private static final String JCR_WORKSPACE_HEADER = "X-Jahia-Graphql-Jcr-Workspace";
    private static final String OPERATION_HEADER = "X-Jahia-Graphql-Operation";

    @Override
    public InstrumentationState createState(InstrumentationCreateStateParameters parameters) {
        return new JCRTrackerInstrumentationState();
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters, InstrumentationState state) {
        OperationDefinition.Operation operation = parameters.getExecutionContext().getOperationDefinition().getOperation();
        ((JCRTrackerInstrumentationState) state).setOperation(operation);
        return Instrumentation.super.beginExecuteOperation(parameters, state);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(InstrumentationFieldParameters parameters, InstrumentationState state) {
        GraphQLFieldDefinition field = parameters.getField();
        if (JCR_FIELD_TYPE.equals(field.getType().toString())) {
            Object defaultValue = getDefaultValue(parameters);
            Workspace workspace = (Workspace) Optional.ofNullable(parameters.getExecutionStepInfo().getArgument(WORKSPACE_ARGUMENT)).orElse(defaultValue);
            ((JCRTrackerInstrumentationState) state).setWorkspace(workspace);
        }
        return Instrumentation.super.beginField(parameters, state);
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        JCRTrackerInstrumentationState jcrTrackerInstrumentationState = (JCRTrackerInstrumentationState) state;
        if (log.isTraceEnabled()) {
            log.trace("operation: {}", jcrTrackerInstrumentationState.operation);
            log.trace("workspace: {}", jcrTrackerInstrumentationState.workspace);
        }
        HttpServletResponse response = ContextUtil.getHttpServletResponse(parameters.getGraphQLContext());
        if (response != null && !response.isCommitted()) {
            // 'operation' is expected to be set all the time
            // 'workspace' is set when processing a GraphQL request on the JCR (the workspace type has been successfully retrieved)
            if (jcrTrackerInstrumentationState.operation != null) {
                log.debug("Setting the http response header with the GraphQL operation: {}={}", OPERATION_HEADER, jcrTrackerInstrumentationState.operation);
                response.setHeader(OPERATION_HEADER, jcrTrackerInstrumentationState.operation.name().toLowerCase());
            }
            if (jcrTrackerInstrumentationState.workspace != null) {
                log.debug("Setting the http response header with the GraphQL JCR workspace: {}={}", JCR_WORKSPACE_HEADER, jcrTrackerInstrumentationState.workspace);
                response.setHeader(JCR_WORKSPACE_HEADER, jcrTrackerInstrumentationState.workspace.name().toLowerCase());
            }
        }
        return Instrumentation.super.instrumentExecutionResult(executionResult, parameters, state);
    }

    private static Object getDefaultValue(InstrumentationFieldParameters parameters) {
        GraphQLArgument workspaceArgument = parameters.getField().getArgument(WORKSPACE_ARGUMENT);
        return workspaceArgument == null ? null : workspaceArgument.getArgumentDefaultValue().getValue();
    }

    /**
     * Track details about the JCR requests being executed
     */
    private static class JCRTrackerInstrumentationState implements InstrumentationState {
        private OperationDefinition.Operation operation;
        private Workspace workspace;

        private void setOperation(OperationDefinition.Operation operation) {
            this.operation = operation;
        }

        private void setWorkspace(Workspace workspace) {
            this.workspace = workspace;
        }
    }
}
