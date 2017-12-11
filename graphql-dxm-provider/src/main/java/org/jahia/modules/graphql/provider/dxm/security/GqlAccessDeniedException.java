package org.jahia.modules.graphql.provider.dxm.security;

import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;

/**
 * Exception throw when access is denied for the requested resource
 */
public class GqlAccessDeniedException extends BaseGqlClientException {
    GqlAccessDeniedException() {
        super("Permission denied", null);
    }
}
