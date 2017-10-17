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
