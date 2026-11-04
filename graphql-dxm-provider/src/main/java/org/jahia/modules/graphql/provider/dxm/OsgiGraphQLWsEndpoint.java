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
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
        Origin parsed = parseOrigin(origin);
        if (parsed != null && requestUri != null && parsed.equals(Origin.of(toHttpScheme(requestUri.getScheme()), requestUri.getRawAuthority()))) {
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
     * Parses a serialized origin, {@code scheme://host[:port]} with an http or https scheme.
     *
     * @return the parsed origin, or {@code null} for any other value, the opaque origin {@code null} included
     */
    static Origin parseOrigin(String origin) {
        if (origin == null) {
            return null;
        }
        try {
            URI uri = new URI(origin);
            String scheme = uri.getScheme();
            boolean serialized = ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && (uri.getRawPath() == null || uri.getRawPath().isEmpty()) && uri.getRawQuery() == null && uri.getRawFragment() == null;
            return serialized ? Origin.of(scheme.toLowerCase(Locale.ENGLISH), uri.getRawAuthority()) : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Maps a WebSocket or HTTP scheme to the HTTP scheme an origin carries: ws to http, wss to https.
     */
    private static String toHttpScheme(String scheme) {
        if (scheme == null) {
            return null;
        }
        switch (scheme.toLowerCase(Locale.ENGLISH)) {
            case "http":
            case "ws":
                return "http";
            case "https":
            case "wss":
                return "https";
            default:
                return null;
        }
    }

    private static Set<String> configuredOrigins() {
        DXGraphQLConfig config = BundleUtils.getOsgiService(DXGraphQLConfig.class, null);
        return config != null ? config.getCorsOrigins() : Collections.emptySet();
    }

    /**
     * Replaces line breaks, so that a value read from a request stays on one log line.
     */
    private static String forLog(Object value) {
        return String.valueOf(value).replaceAll("[\r\n]", "_");
    }

    /**
     * An http or https origin, its host lower-cased and its port explicit. The host is read from the authority as
     * written, so a host name {@link URI#getHost()} leaves {@code null}, such as one holding an underscore, is kept.
     */
    static final class Origin {
        private final String scheme;
        private final String host;
        private final int port;

        private Origin(String scheme, String host, int port) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
        }

        /**
         * @param scheme    {@code http} or {@code https}
         * @param authority {@code host[:port]}, an IPv6 host in brackets
         * @return the origin, or {@code null} when the scheme or the authority is of another form
         */
        static Origin of(String scheme, String authority) {
            if (scheme == null || authority == null || authority.isEmpty() || authority.indexOf('@') >= 0) {
                return null;
            }
            String host;
            String port = null;
            int hostEnd = authority.startsWith("[") ? authority.indexOf(']') + 1 : authority.lastIndexOf(':');
            if (authority.startsWith("[")) {
                if (hostEnd == 0) {
                    return null;
                }
                host = authority.substring(0, hostEnd);
                if (hostEnd < authority.length()) {
                    if (authority.charAt(hostEnd) != ':') {
                        return null;
                    }
                    port = authority.substring(hostEnd + 1);
                }
            } else if (hostEnd >= 0) {
                host = authority.substring(0, hostEnd);
                port = authority.substring(hostEnd + 1);
            } else {
                host = authority;
            }
            if (host.isEmpty() || (!host.startsWith("[") && host.indexOf(':') >= 0)) {
                return null;
            }
            int defaultPort = "https".equals(scheme) ? 443 : 80;
            if (port == null || port.isEmpty()) {
                return new Origin(scheme, host.toLowerCase(Locale.ENGLISH), defaultPort);
            }
            if (!port.matches("\\d{1,5}") || Integer.parseInt(port) > 65535) {
                return null;
            }
            return new Origin(scheme, host.toLowerCase(Locale.ENGLISH), Integer.parseInt(port));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Origin)) {
                return false;
            }
            Origin other = (Origin) o;
            return scheme.equals(other.scheme) && host.equals(other.host) && port == other.port;
        }

        @Override
        public int hashCode() {
            return Objects.hash(scheme, host, port);
        }
    }

    public static class Configurator extends ServerEndpointConfig.Configurator {

        private static final Logger LOGGER = LoggerFactory.getLogger(Configurator.class);

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
