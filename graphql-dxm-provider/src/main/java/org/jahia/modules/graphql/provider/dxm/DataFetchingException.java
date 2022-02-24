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

/**
 * GraphQL exception which is caused by the failure of a data fetching operation.
 */
public class DataFetchingException extends BaseGqlClientException {

    private static final long serialVersionUID = 7641894928244318180L;

    public DataFetchingException(String message) {
        super(message, ErrorType.DataFetchingException);
    }

    public DataFetchingException(String message, Throwable cause) {
        super(message, cause, ErrorType.DataFetchingException);
    }

    public DataFetchingException(Throwable cause) {
        super(cause, ErrorType.DataFetchingException);
    }
}
