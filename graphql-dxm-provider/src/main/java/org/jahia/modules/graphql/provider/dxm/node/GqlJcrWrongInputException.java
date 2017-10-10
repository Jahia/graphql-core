package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.exceptions.JahiaRuntimeException;

/**
 * Indicates any wrong query input parameter value.
 */
public class GqlJcrWrongInputException extends JahiaRuntimeException {

    private static final long serialVersionUID = -1604229619716908650L;

    /**
     * Create an exception instance.
     *
     * @param message Error message
     * @param cause Cause if any
     */
    public GqlJcrWrongInputException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create an exception instance.
     *
     * @param message Error message
     */
    public GqlJcrWrongInputException(String message) {
        super(message);
    }
}
