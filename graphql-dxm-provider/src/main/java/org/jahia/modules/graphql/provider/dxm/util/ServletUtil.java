package org.jahia.modules.graphql.provider.dxm.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ServletUtil {

    /**
     * Unwraps request if it is instance of HttpServletRequestWrapper else returns original request
     *<p>
     * This may be necessary in some situations as Felix's ServletHandlerRequest can transform request
     * </p>
     * from:
     * <p>
     * contextPath: ""
     * servletPath: "/modules"
     * pathInfo: "/graphql"
     * requestURI: "/modules/graphql"
     *</p>
     * to:
     *<p>
     * contextPath: ""
     * servletPath: "/graphql"
     * pathInfo: null
     * requestURI: "/modules/graphql"
     *</p>
     * <p>
     * Which may lead to undesirable artifacts in urls. For example, using such a request to process outbound rewrite rules
     * will result in "/modules" suffix on context even if the context is originally empty.
     * </p>
     *
     *
     * @param request
     * @return HttpServletRequest
     */
    public static HttpServletRequest unwrapRequest(HttpServletRequest request) {
        if (request instanceof HttpServletRequestWrapper) {
            return (HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest();
        }

        return request;
    }
}
