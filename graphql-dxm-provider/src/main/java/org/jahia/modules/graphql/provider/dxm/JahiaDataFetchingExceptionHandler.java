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

        log.error(error.getMessage(), exception);

        return builder.build();
    }
}
