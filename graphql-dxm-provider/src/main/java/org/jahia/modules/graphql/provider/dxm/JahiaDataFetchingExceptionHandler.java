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

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom DataFetchingExceptionHandler
 */
public class JahiaDataFetchingExceptionHandler implements DataFetcherExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(JahiaDataFetchingExceptionHandler.class);

    @NotNull
    public static GraphQLError transformException(Throwable exception, DataFetchingEnvironment environment) {
        return transformException(exception, environment.getExecutionStepInfo().getPath(), environment.getField().getSourceLocation());
    }

    public static GraphQLError transformException(Throwable exception, ExecutionPath path, SourceLocation sourceLocation) {
        // Unwrap exception from MethodDataFetcher
        exception = unwrapException(exception);
        if (exception instanceof BaseGqlClientException) {
            return new DXGraphQLError((BaseGqlClientException) exception, path.toList(), sourceLocation != null ? Collections.singletonList(sourceLocation) : new ArrayList<>());
        } else {
            return new ExceptionWhileDataFetching(path, exception, sourceLocation);
        }
    }

    private static Throwable unwrapException(Throwable exception) {
        if (exception instanceof RuntimeException && exception.getCause() instanceof InvocationTargetException) {
            return ((InvocationTargetException) exception.getCause()).getTargetException();
        }
        return exception;
    }

    @Override
    public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        exception = unwrapException(exception);

        DataFetcherExceptionHandlerResult.Builder builder = DataFetcherExceptionHandlerResult.newResult();
        ExecutionPath path = handlerParameters.getPath();
        SourceLocation sourceLocation = handlerParameters.getField().getSingleField().getSourceLocation();

        GraphQLError error = transformException(exception, path, sourceLocation);
        builder.error(error);

        if (!(error instanceof DXGraphQLError)) {
            log.warn(error.getMessage(), exception);
        }

        return builder.build();
    }
}
