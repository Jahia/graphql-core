package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.ExecutionResult;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import org.jahia.modules.graphql.provider.dxm.node.JcrVersionSessions;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;

/**
 * Opens the version sessions of a query that reads a version, through {@link JcrVersionSessions}. It runs before
 * {@link JCRInstrumentation}, whose introspection check opens the first session of the operation, and it aborts an
 * operation that cannot read its version.
 */
public class VersionSessionInstrumentation extends SimplePerformantInstrumentation {

    @Override
    public ExecutionContext instrumentExecutionContext(ExecutionContext executionContext, InstrumentationExecutionParameters parameters,
                                                       InstrumentationState state) {
        JcrVersionSessions.getInstance().beginOperation(executionContext.getOperationDefinition(),
                executionContext.getFragmentsByName(), executionContext.getCoercedVariables().toMap(),
                ContextUtil.getHttpServletRequest(executionContext.getGraphQLContext()), executionContext.getGraphQLContext());
        return executionContext;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters,
                                                                         InstrumentationState state) {
        JcrVersionSessions.getInstance().checkOperation(parameters.getExecutionContext().getGraphQLContext());
        return super.beginExecuteOperation(parameters, state);
    }
}
