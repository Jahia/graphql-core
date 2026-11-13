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
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component(immediate = true, service = Endpoint.class)
@ServerEndpoint(value="/graphqlws", subprotocols = {"graphql-ws", "graphql-transport-ws"}, configurator = OsgiGraphQLWsEndpoint.Configurator.class)
public class OsgiGraphQLWsEndpoint extends Endpoint {
    private Logger logger = LoggerFactory.getLogger(OsgiGraphQLWsEndpoint.class);
    private static GraphQLWebsocketServlet delegate;

    /**
     * The thread that closes the sessions {@link #onOpen} refuses, once onOpen has returned. The {@link Configurator}
     * and the endpoint instances are created by the container, so the executor is held statically, as the delegate is.
     */
    private static final AtomicReference<ScheduledExecutorService> CLOSINGS = new AtomicReference<>();
    private static final long REFUSAL_CLOSE_DELAY_MILLIS = 100;

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
        if (CLOSINGS.get() == null) {
            CLOSINGS.set(new ScheduledThreadPoolExecutor(1, runnable -> {
                Thread thread = new Thread(runnable, "graphql-websocket-closing");
                thread.setDaemon(true);
                return thread;
            }));
        }
    }

    @Deactivate
    public void deactivate() {
        delegate.beginShutDown();
        delegate = null;
        ScheduledExecutorService closings = CLOSINGS.getAndSet(null);
        if (closings != null) {
            closings.shutdownNow();
        }
    }

    /**
     * Key, in the user properties of the handshake ({@link EndpointConfig#getUserProperties()}), of whether its Origin
     * is allowed. The container copies those properties into the session ({@link Session#getUserProperties()}), where
     * {@link #onClose} and {@link #onError} read the key too.
     */
    static final String ORIGIN_ALLOWED = "org.jahia.modules.graphql.originAllowed";

    private static final String ORIGIN_HEADER = "Origin";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        Map<String, Object> userProperties = endpointConfig.getUserProperties();
        if (!Boolean.TRUE.equals(userProperties.get(ORIGIN_ALLOWED))) {
            logger.debug("Closing WebSocket session {}: its origin is not allowed", session.getId());
            closeAfterOpen(() -> {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Origin not allowed"));
                } catch (IOException e) {
                    logger.debug("Cannot close WebSocket session {}", session.getId(), e);
                }
            });
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

    /**
     * Closes a session that {@link #onOpen} refuses, shortly after onOpen returns: the websocket bridge of Jahia
     * registers a session once onOpen returns, and the close then finds the session registered. With no executor, or
     * one that refuses the task while the endpoint shuts down, the session is closed at once.
     */
    private static void closeAfterOpen(Runnable close) {
        ScheduledExecutorService executor = CLOSINGS.get();
        if (executor != null) {
            try {
                executor.schedule(close, REFUSAL_CLOSE_DELAY_MILLIS, TimeUnit.MILLISECONDS);
                return;
            } catch (RejectedExecutionException e) {
                // The endpoint is shutting down: the session is closed below.
            }
        }
        close.run();
    }

    private static boolean isOpenedByDelegate(Session session) {
        return Boolean.TRUE.equals(session.getUserProperties().get(ORIGIN_ALLOWED));
    }

    /**
     * Tells whether a handshake is accepted on a request addressed to {@code requestUri}: a handshake without an Origin
     * header is, and an Origin header must name the one origin the request is addressed to.
     *
     * @param origins    the values of the Origin header, {@code null} when the header is absent
     * @param requestUri the absolute URI of the handshake request, in the ws or wss scheme
     * @return {@code true} if the handshake is accepted
     */
    static boolean isAllowedHandshake(List<String> origins, URI requestUri) {
        if (origins == null || origins.isEmpty()) {
            return true;
        }
        return origins.size() == 1 && isSameOrigin(origins.get(0), requestUri);
    }

    /**
     * Compares an Origin header, as text and ignoring case, with the origin the request is addressed to, a default port
     * written or not, in the shape of {@code org.jahia.utils.Url#isSameOrigin} of Jahia core.
     */
    static boolean isSameOrigin(String origin, URI requestUri) {
        if (origin == null || requestUri == null || requestUri.getRawAuthority() == null) {
            return false;
        }
        String scheme = requestUri.getScheme();
        if ("ws".equalsIgnoreCase(scheme)) {
            scheme = HTTP;
        } else if ("wss".equalsIgnoreCase(scheme)) {
            scheme = HTTPS;
        }
        return withoutDefaultPort(origin).equalsIgnoreCase(withoutDefaultPort(scheme + "://" + requestUri.getRawAuthority()));
    }

    private static String withoutDefaultPort(String origin) {
        if (StringUtils.startsWithIgnoreCase(origin, HTTP + "://") && origin.endsWith(":80")) {
            return origin.substring(0, origin.length() - ":80".length());
        }
        if (StringUtils.startsWithIgnoreCase(origin, HTTPS + "://") && origin.endsWith(":443")) {
            return origin.substring(0, origin.length() - ":443".length());
        }
        return origin;
    }

    public static class Configurator extends ServerEndpointConfig.Configurator {

        private static final Logger LOGGER = LoggerFactory.getLogger(Configurator.class);

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            List<String> origins = request.getHeaders().get(ORIGIN_HEADER);
            boolean originAllowed = isAllowedHandshake(origins, request.getRequestURI());
            sec.getUserProperties().put(ORIGIN_ALLOWED, originAllowed);
            if (!originAllowed) {
                LOGGER.warn("Rejected non-same-origin GraphQL subscription connection (origin={})", origins);
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
