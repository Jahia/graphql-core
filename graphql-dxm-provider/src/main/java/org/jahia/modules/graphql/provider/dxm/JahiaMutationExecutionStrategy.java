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

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.execution.*;
import graphql.language.SourceLocation;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Extends some aspects of the standard strategy.
 */
public class JahiaMutationExecutionStrategy extends AsyncSerialExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(JahiaMutationExecutionStrategy.class);

    public JahiaMutationExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        if(SettingsBean.getInstance().isFullReadOnlyMode()) {
            String message = "Operation is not permitted as DX is in read-only mode";
            logger.warn(message);
            DXGraphQLError error = new DXGraphQLError(new GqlReadOnlyModeException(message), parameters.getPath().toList(), new ArrayList<>());
            return CompletableFuture.completedFuture(new ExecutionResultImpl(error));
        }
        return super.execute(executionContext, parameters);
    }

    /**
     * Extend the standard behavior to complete any GqlJcrMutation field via persisting any changes made to JCR during its execution.
     */
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
