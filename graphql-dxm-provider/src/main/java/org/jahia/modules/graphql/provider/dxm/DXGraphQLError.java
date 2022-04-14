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

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

public class DXGraphQLError implements GraphQLError {
    private final String message;
    private List<Object> path;
    private List<SourceLocation> locations;
    private ErrorType errorType;
    private BaseGqlClientException exception;

    public DXGraphQLError(BaseGqlClientException exception, List<Object> path, List<SourceLocation> locations) {
        this.message = exception.getMessage();
        this.path = path;
        this.locations = locations;
        this.errorType = exception.getErrorType();
        this.exception = exception;
    }

    @Override
    public List<Object> getPath() {
        return path;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return locations;
    }

    @Override
    public ErrorClassification getErrorType() {
        if (errorType != null) {
            return errorType;
        } else {
            return new SimpleClassification(exception.getClass().getSimpleName());
        }
    }

    @Override
    public Map<String, Object> getExtensions() {
        return exception.getExtensions();
    }

    @Override
    public Map<String, Object> toSpecification() {
        Map<String, Object> result = GraphQLError.super.toSpecification();
        result.put("errorType", getErrorType().toSpecification(this));
        return result;
    }

    private class SimpleClassification implements ErrorClassification {
        private String value;

        public SimpleClassification(String value) {
            this.value = value;
        }

        @Override
        public Object toSpecification(GraphQLError error) {
            return value;
        }
    }
}
