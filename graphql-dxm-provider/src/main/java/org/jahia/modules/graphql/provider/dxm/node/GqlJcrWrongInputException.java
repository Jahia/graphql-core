package org.jahia.modules.graphql.provider.dxm.node;

import graphql.ErrorType;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;

/**
 * Indicates any wrong query input parameter value.
 */
public class GqlJcrWrongInputException extends BaseGqlClientException {

    private static final long serialVersionUID = -1604229619716908650L;

    /**
     * Create an exception instance.
     *
     * @param message Error message
     * @param cause Cause if any
     */
    public GqlJcrWrongInputException(String message, Throwable cause) {
        super(message, cause, ErrorType.ValidationError);
    }

    /**
     * Create an exception instance.
     *
     * @param message Error message
     */
    public GqlJcrWrongInputException(String message) {
        super(message, ErrorType.ValidationError);
    }
}
