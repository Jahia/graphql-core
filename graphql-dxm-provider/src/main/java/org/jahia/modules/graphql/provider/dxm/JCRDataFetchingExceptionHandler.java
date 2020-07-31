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
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Custom DataFetchingExceptionHandler
 */
public class JCRDataFetchingExceptionHandler implements DataFetcherExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(JCRDataFetchingExceptionHandler.class);

    @Override
    public void accept(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();

        ExecutionPath path = handlerParameters.getPath();
        GraphQLError error = transformException(exception, path, handlerParameters.getField().getSourceLocation());
        handlerParameters.getExecutionContext().addError(error, path);

        if (!(error instanceof DXGraphQLError)) {
            log.warn(error.getMessage(), exception);
        }
    }

    public static GraphQLError transformException(Throwable exception, ExecutionPath path, SourceLocation sourceLocation) {
        // Unwrap exception from MethodDataFetcher
        if (exception instanceof RuntimeException && exception.getCause() instanceof InvocationTargetException) {
            exception = ((InvocationTargetException) exception.getCause()).getTargetException();
        }

        if (exception instanceof BaseGqlClientException) {
            return new DXGraphQLError((BaseGqlClientException) exception, path.toList(), sourceLocation != null ? Collections.singletonList(sourceLocation) : new ArrayList<>());
        } else {
            return new ExceptionWhileDataFetching(path, exception, sourceLocation);
        }
    }
}
