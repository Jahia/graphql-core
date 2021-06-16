package org.jahia.modules.graphql.provider.dxm;

import graphql.GraphQLError;
import graphql.execution.*;
import graphql.language.SourceLocation;

public class JahiaQueryExecutionStrategy extends AsyncExecutionStrategy {

    public JahiaQueryExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    @Override
    protected FieldValueInfo completeField(ExecutionContext executionContext, ExecutionStrategyParameters parameters, Object fetchedValue) {
        FieldValueInfo result = super.completeField(executionContext, parameters, fetchedValue);

        if (fetchedValue instanceof DXGraphQLFieldCompleter && executionContext.getErrors().isEmpty()) {
            // we only complete field if there were no errors on execution
            try {
                ((DXGraphQLFieldCompleter) fetchedValue).completeField();
            } catch (Exception e) {
                SourceLocation sourceLocation = (parameters.getField() != null && !parameters.getField().isEmpty()) ? parameters.getField().get(0).getSourceLocation() : null;
                GraphQLError error = JahiaDataFetchingExceptionHandler.transformException(e, parameters.getPath(), sourceLocation);
                executionContext.addError(error, parameters.getPath());
            }
        }

        return result;
    }
}
