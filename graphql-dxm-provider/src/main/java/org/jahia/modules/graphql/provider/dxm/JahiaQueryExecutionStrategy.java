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
package org.jahia.modules.graphql.provider.dxm;

import graphql.GraphQLError;
import graphql.execution.*;
import graphql.language.SourceLocation;

public class JahiaQueryExecutionStrategy extends AsyncExecutionStrategy {

    public JahiaQueryExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    @Override
    protected FieldValueInfo completeField(ExecutionContext executionContext,
            ExecutionStrategyParameters parameters, FetchedValue fetchedValue) {
        FieldValueInfo result = super.completeField(executionContext, parameters, fetchedValue);

        Object value = fetchedValue.getFetchedValue();
        if (value instanceof DXGraphQLFieldCompleter && executionContext.getErrors().isEmpty()) {
            // we only complete field if there were no errors on execution
            try {
                ((DXGraphQLFieldCompleter) value).completeField();
            } catch (Exception e) {
                SourceLocation sourceLocation = parameters.getField().getSingleField().getSourceLocation();
                GraphQLError error = JahiaDataFetchingExceptionHandler.transformException(e, parameters.getPath(), sourceLocation);
                executionContext.addError(error, parameters.getPath());
            }
        }

        return result;
    }
}
