package org.jahia.modules.graphql.provider.dxm;

import graphql.ErrorType;
import org.jahia.exceptions.JahiaRuntimeException;

public class BaseGqlClientException extends JahiaRuntimeException {

    private ErrorType errorType;

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

    public BaseGqlClientException(Throwable cause, ErrorType errorType) {
        super(cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
