package org.jahia.modules.graphql.provider.dxm;

import graphql.GraphQLError;
import graphql.execution.*;
import graphql.language.SourceLocation;
import org.hibernate.annotations.Fetch;

public class JahiaQueryExecutionStrategy extends AsyncExecutionStrategy {

    public JahiaQueryExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    protected FieldValueInfo completeField(ExecutionContext executionContext, ExecutionStrategyParameters parameters, Object fetchedValue) {
        FieldValueInfo result = super.completeField(executionContext, parameters, (FetchedValue) fetchedValue);

        if (fetchedValue instanceof DXGraphQLFieldCompleter && executionContext.getErrors().isEmpty()) {
            // we only complete field if there were no errors on execution
            try {
                ((DXGraphQLFieldCompleter) fetchedValue).completeField();
            } catch (Exception e) {
                SourceLocation sourceLocation = parameters.getField().getSingleField().getSourceLocation();
                GraphQLError error = JahiaDataFetchingExceptionHandler.transformException(e, parameters.getPath(), sourceLocation);
                executionContext.addError(error, parameters.getPath());
            }
        }

        return result;
    }
}
