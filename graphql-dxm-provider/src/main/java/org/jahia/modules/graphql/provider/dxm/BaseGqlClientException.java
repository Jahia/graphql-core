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

import graphql.ErrorType;
import org.jahia.exceptions.JahiaRuntimeException;

import java.util.Map;

/**
 * Base exception for the GraphQL errors.
 */
public class BaseGqlClientException extends JahiaRuntimeException {

    private static final long serialVersionUID = 2380023950503433037L;

    private ErrorType errorType;
    private Map<String,Object> extensions;

    public BaseGqlClientException(ErrorType errorType) {
        this.errorType = errorType;
    }

    public BaseGqlClientException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public BaseGqlClientException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public BaseGqlClientException(String message, Throwable cause, Map<String,Object> extensions) {
        super(message, cause);
        this.extensions = extensions;
    }

    public BaseGqlClientException(String message, Throwable cause, ErrorType errorType, Map<String,Object> extensions) {
        super(message, cause);
        this.errorType = errorType;
        this.extensions = extensions;
    }

    public BaseGqlClientException(Throwable cause, ErrorType errorType) {
        super(cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }
}