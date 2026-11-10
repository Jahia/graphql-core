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

import graphql.kickstart.execution.subscriptions.SubscriptionProtocolFactory;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import graphql.kickstart.servlet.OsgiGraphQLHttpServlet;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.securityfilter.PermissionService;
import org.jahia.services.usermanager.JahiaUser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Component(immediate = true, service = Endpoint.class)
@ServerEndpoint(value="/graphqlws", subprotocols = {"graphql-ws", "graphql-transport-ws"}, configurator = OsgiGraphQLWsEndpoint.Configurator.class)
public class OsgiGraphQLWsEndpoint extends Endpoint {
    private Logger logger = LoggerFactory.getLogger(OsgiGraphQLWsEndpoint.class);
    private static GraphQLWebsocketServlet delegate;

    /**
     * User-property key under which the authorization scopes resolved for the handshake (HTTP)
     * request are stashed, so the subscription execution thread can restore them and apply the same
     * permission checks as an HTTP request. Mirrors {@link Constants#SESSION_USER}.
     */
    public static final String SESSION_SCOPES = "org.jahia.modules.graphql.securityScopes";

    @Reference(service = HttpServlet.class, target = "(component.name=graphql.kickstart.servlet.OsgiGraphQLHttpServlet)")
    public void setServlet(HttpServlet servlet) {
        try {
            GraphQLConfiguration configuration = ((OsgiGraphQLHttpServlet)servlet).getSchemaBuilder().getConfiguration();
            delegate = new GraphQLWebsocketServlet(configuration.getGraphQLInvoker(), configuration.getInvocationInputFactory(), configuration.getObjectMapper());
        } catch (Exception e) {
            logger.error("Cannot get schema from GQL servlet", e);
        }
    }

    @Deactivate
    public void deactivate() {
        delegate.beginShutDown();
        delegate = null;
    }

    /**
     * Key, in the user properties of the endpoint configuration ({@link EndpointConfig#getUserProperties()}) that the
     * handshake hands to {@link #onOpen}, of whether the Origin of the handshake is allowed.
     */
    static final String ORIGIN_ALLOWED = "org.jahia.modules.graphql.originAllowed";

    /**
     * Key, in the user properties of a WebSocket session ({@link Session#getUserProperties()}), that marks a session
     * closed by {@link #onOpen} because its Origin is not allowed, so that {@link #onClose} and {@link #onError} leave
     * it alone.
     */
    static final String ORIGIN_REFUSED = "org.jahia.modules.graphql.originRefused";

    private static final String ORIGIN_HEADER = "Origin";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        Map<String, Object> userProperties = endpointConfig.getUserProperties();
        if (!Boolean.TRUE.equals(userProperties.get(ORIGIN_ALLOWED))) {
            session.getUserProperties().put(ORIGIN_REFUSED, Boolean.TRUE);
            logger.debug("Closing WebSocket session {}: its origin is not allowed", session.getId());
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Origin not allowed"));
            } catch (IOException e) {
                logger.debug("Cannot close WebSocket session {}", session.getId(), e);
            }
            return;
        }

        JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) userProperties.get(Constants.SESSION_USER));

        try {
            delegate.onOpen(session, endpointConfig);
        } finally {
            JCRSessionFactory.getInstance().setCurrentUser(null);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        if (isOpenedByDelegate(session)) {
            delegate.onClose(session, closeReason);
        }
    }

    @Override
    public void onError(Session session, Throwable thr) {
        if (isOpenedByDelegate(session)) {
            delegate.onError(session, thr);
        }
    }

    private static boolean isOpenedByDelegate(Session session) {
        return !Boolean.TRUE.equals(session.getUserProperties().get(ORIGIN_REFUSED));
    }

    /**
     * Tells whether a handshake carrying {@code origin} is accepted on a request addressed to {@code requestUri}: the
     * origin must be the one the request is addressed to, or one of the configured origins. The configured origins are
     * read only when the origin is not the one the request is addressed to.
     *
     * @param origin            the value of the Origin header, {@code null} when the header is absent
     * @param requestUri        the absolute URI of the handshake request, in the ws or wss scheme
     * @param configuredOrigins the origins configured in {@code http.cors.allow-origin}
     * @return {@code true} if the origin is accepted
     */
    static boolean isAllowedOrigin(String origin, URI requestUri, Supplier<Set<String>> configuredOrigins) {
        URL parsed = parseOrigin(origin);
        if (parsed != null && requestUri != null && isSameOrigin(parsed, requestOrigin(requestUri))) {
            return true;
        }
        return isConfigured(origin, configuredOrigins);
    }

    /**
     * Tells whether {@code origin} is one of the configured origins, compared as exactly the text it holds. The opaque
     * origin {@code null} and the wildcard {@code *} never match.
     */
    static boolean isConfigured(String origin, Supplier<Set<String>> configuredOrigins) {
        return origin != null && !origin.isEmpty() && !"null".equals(origin) && !"*".equals(origin)
                && configuredOrigins.get().contains(origin);
    }

    /**
     * Parses a serialized origin, {@code scheme://host[:port]} with an http or https scheme. It is parsed as a
     * {@link URL} rather than a {@link URI}, because {@link URI#getHost()} is {@code null} for a host name that holds
     * an underscore.
     *
     * @return the parsed origin, or {@code null} for any other value, the opaque origin {@code null} included
     */
    static URL parseOrigin(String origin) {
        if (origin == null) {
            return null;
        }
        try {
            URL url = new URL(origin);
            boolean serialized = (HTTP.equals(url.getProtocol()) || HTTPS.equals(url.getProtocol()))
                    && !url.getHost().isEmpty() && url.getUserInfo() == null && url.getFile().isEmpty() && url.getRef() == null;
            return serialized ? url : null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Returns the origin a handshake request is addressed to: the host and port of its URI, with ws read as http and
     * wss as https, or {@code null} when the URI holds no host.
     */
    private static URL requestOrigin(URI requestUri) {
        if (requestUri.getRawAuthority() == null) {
            return null;
        }
        String scheme = requestUri.getScheme();
        if ("ws".equalsIgnoreCase(scheme)) {
            scheme = HTTP;
        } else if ("wss".equalsIgnoreCase(scheme)) {
            scheme = HTTPS;
        }
        return parseOrigin(scheme + "://" + requestUri.getRawAuthority());
    }

    /**
     * Compares the scheme, the host and the port of two origins, a missing port read as the default one of the scheme.
     * {@link URL#equals} is not used, because it resolves both host names.
     */
    private static boolean isSameOrigin(URL origin, URL other) {
        return other != null && origin.getProtocol().equals(other.getProtocol())
                && origin.getHost().equalsIgnoreCase(other.getHost())
                && portOf(origin) == portOf(other);
    }

    private static int portOf(URL url) {
        return url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
    }

    private static Set<String> configuredOrigins() {
        DXGraphQLConfig config = BundleUtils.getOsgiService(DXGraphQLConfig.class, null);
        return config != null ? config.getCorsOrigins() : Collections.emptySet();
    }

    public static class Configurator extends ServerEndpointConfig.Configurator {

        private static final Logger LOGGER = LoggerFactory.getLogger(Configurator.class);

        /**
         * Replaces line breaks, so that a value read from a request stays on one log line.
         */
        private static String forLog(Object value) {
            return String.valueOf(value).replaceAll("[\r\n]", "_");
        }

        /**
         * Accepts only a handshake that carries a serialized http or https origin, or one of the configured origins.
         * Whether an http or https origin is allowed depends on the address of the request, so
         * {@link #modifyHandshake} decides it.
         */
        @Override
        public boolean checkOrigin(String originHeaderValue) {
            return parseOrigin(originHeaderValue) != null || isConfigured(originHeaderValue, OsgiGraphQLWsEndpoint::configuredOrigins);
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            List<String> origins = request.getHeaders().get(ORIGIN_HEADER);
            boolean originAllowed = origins != null && origins.size() == 1
                    && isAllowedOrigin(origins.get(0), request.getRequestURI(), OsgiGraphQLWsEndpoint::configuredOrigins);
            sec.getUserProperties().put(ORIGIN_ALLOWED, originAllowed);
            if (!originAllowed) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Origin {} is not allowed for a WebSocket connection addressed to {}",
                            forLog(origins), forLog(request.getRequestURI()));
                }
                return;
            }

            // Based on GraphQLWebsocketServlet.modifyHandshake - skip the last part sending response header, already handled by whiteboard
            sec.getUserProperties().put(HandshakeRequest.class.getName(), request);

            List<String> protocol = request.getHeaders().get(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL);
            if (protocol == null) {
                protocol = Collections.emptyList();
            }

            SubscriptionProtocolFactory subscriptionProtocolFactory = delegate.getSubscriptionProtocolFactory(protocol);
            sec.getUserProperties().put(SubscriptionProtocolFactory.class.getName(), subscriptionProtocolFactory);

            // Add the current user to user props
            sec.getUserProperties().put(Constants.SESSION_USER, JCRSessionFactory.getInstance().getCurrentUser());

            // Capture the authorization scopes resolved for this handshake (HTTP) request so the
            // subscription execution thread can restore them (see JahiaSubscriptionExecutionStrategy).
            // The handshake runs through the servlet filter chain, so the scopes are resolved here.
            PermissionService permissionService = BundleUtils.getOsgiService(PermissionService.class, null);
            if (permissionService != null) {
                sec.getUserProperties().put(SESSION_SCOPES, permissionService.getCurrentScopes());
            }
        }
    }

}
