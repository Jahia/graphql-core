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
package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;

import graphql.ErrorType;

/**
 * Indicates inability to resolve a property that assumed to be a node reference, to an existing node, due to property's actual type or value.
 */
public class GqlJcrUnresolvedNodeReferenceException extends BaseGqlClientException {

    private static final long serialVersionUID = 4964063045501303555L;

    /**
     * Create an exception instance.
     *
     * @param message Error message
     * @param cause Cause if any
     */
    public GqlJcrUnresolvedNodeReferenceException(String message, Throwable cause) {
        super(message, ErrorType.DataFetchingException);
    }

    /**
     * Create an exception instance.
     *
     * @param message Error message
     */
    public GqlJcrUnresolvedNodeReferenceException(String message) {
        this(message, null);
    }
}
