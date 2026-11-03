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
import java.util.Set;

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
     * User-property key under which the handshake records whether its Origin is allowed.
     */
    static final String ORIGIN_ALLOWED = "org.jahia.modules.graphql.originAllowed";

    private static final String ORIGIN_HEADER = "Origin";

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        Map<String, Object> userProperties = endpointConfig.getUserProperties();
        if (!Boolean.TRUE.equals(userProperties.get(ORIGIN_ALLOWED))) {
            session.getUserProperties().put(ORIGIN_ALLOWED, Boolean.FALSE);
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
        return !Boolean.FALSE.equals(session.getUserProperties().get(ORIGIN_ALLOWED));
    }

    /**
     * Tells whether a handshake carrying {@code origin} is accepted on a request addressed to {@code requestUri}: the
     * origin must be the one the request is addressed to, or one of {@code allowedOrigins}.
     *
     * @param origin         the value of the Origin header, {@code null} when the header is absent
     * @param requestUri     the absolute URI of the handshake request, in the ws or wss scheme
     * @param allowedOrigins the origins configured in {@code http.cors.allow-origin}
     * @return {@code true} if the origin is accepted
     */
    static boolean isAllowedOrigin(String origin, URI requestUri, Set<String> allowedOrigins) {
        URI originUri = parseOrigin(origin);
        if (originUri == null) {
            return false;
        }
        if (allowedOrigins.contains(origin)) {
            return true;
        }
        return requestUri != null && isSameOrigin(originUri, requestUri);
    }

    /**
     * Parses a serialized origin, {@code scheme://host[:port]} with an http or https scheme.
     *
     * @return the parsed origin, or {@code null} for any other value, the opaque origin {@code null} included
     */
    static URI parseOrigin(String origin) {
        if (origin == null) {
            return null;
        }
        try {
            URI uri = new URI(origin);
            boolean serialized = ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null
                    && (uri.getRawPath() == null || uri.getRawPath().isEmpty()) && uri.getRawQuery() == null && uri.getRawFragment() == null;
            return serialized ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean isSameOrigin(URI origin, URI requestUri) {
        String scheme = toHttpScheme(requestUri.getScheme());
        return scheme != null && scheme.equals(toHttpScheme(origin.getScheme()))
                && requestUri.getHost() != null && requestUri.getHost().equalsIgnoreCase(origin.getHost())
                && portOf(requestUri, scheme) == portOf(origin, scheme);
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

    private static int portOf(URI uri, String httpScheme) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equals(httpScheme) ? 443 : 80;
    }

    private static Set<String> configuredOrigins() {
        DXGraphQLConfig config = BundleUtils.getOsgiService(DXGraphQLConfig.class, null);
        return config != null ? config.getCorsOrigins() : Collections.emptySet();
    }

    public static class Configurator extends ServerEndpointConfig.Configurator {

        /**
         * Accepts only a handshake that carries a serialized http or https origin. Whether that origin is allowed
         * depends on the address of the request, so {@link #modifyHandshake} decides it.
         */
        @Override
        public boolean checkOrigin(String originHeaderValue) {
            return parseOrigin(originHeaderValue) != null;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            List<String> origins = request.getHeaders().get(ORIGIN_HEADER);
            boolean originAllowed = origins != null && origins.size() == 1
                    && isAllowedOrigin(origins.get(0), request.getRequestURI(), configuredOrigins());
            sec.getUserProperties().put(ORIGIN_ALLOWED, originAllowed);
            if (!originAllowed) {
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
