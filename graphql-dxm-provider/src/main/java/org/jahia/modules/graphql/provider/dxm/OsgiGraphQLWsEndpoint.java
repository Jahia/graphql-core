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
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.config.WebSocketLimits;
import org.jahia.modules.graphql.provider.dxm.util.GqlTypeUtil;
import org.jahia.modules.graphql.provider.dxm.websocket.BoundedSubscriptionConsumer;
import org.jahia.modules.graphql.provider.dxm.websocket.BoundedSubscriptionProtocolFactory;
import org.jahia.modules.graphql.provider.dxm.websocket.ConnectionClosing;
import org.jahia.modules.graphql.provider.dxm.websocket.ConnectionLimiter;
import org.jahia.modules.graphql.provider.dxm.websocket.ThrottledReport;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.securityfilter.PermissionService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServlet;
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component(immediate = true, service = Endpoint.class)
@ServerEndpoint(value="/graphqlws", subprotocols = {OsgiGraphQLWsEndpoint.SUBPROTOCOL}, configurator = OsgiGraphQLWsEndpoint.Configurator.class)
public class OsgiGraphQLWsEndpoint extends Endpoint {
    private Logger logger = LoggerFactory.getLogger(OsgiGraphQLWsEndpoint.class);
    private static GraphQLWebsocketServlet delegate;
    private static GraphQLInvocationInputFactory invocationInputFactory;
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(OsgiGraphQLWsEndpoint.class);
    private static final ConnectionLimiter connections = new ConnectionLimiter();

    static final String SUBPROTOCOL = "graphql-ws";

    /**
     * Key, in the user properties of the endpoint configuration that the handshake hands to {@link #onOpen}, of
     * whether the scopes of the handshake grant at least one field of the subscription type.
     */
    static final String SUBSCRIPTIONS_GRANTED = "org.jahia.modules.graphql.subscriptionsGranted";

    /**
     * The sessions {@link #onOpen} handed to the delegate. {@link #onClose} and {@link #onError} hand only these to the
     * delegate. The container refuses to read the user properties of a closed session, and the websocket bridge calls
     * {@link #onClose} on a closed session when the endpoint is unregistered, so the state is kept here.
     */
    private static final Map<Session, Accepted> accepted = new ConcurrentHashMap<>();

    /**
     * The thread that closes the sessions {@link #onOpen} refuses, once onOpen has returned, and the sessions that send
     * no {@code connection_init} in time. The {@link Configurator} and the endpoint instances are created by the
     * container, so the executor is held statically, as the delegate is.
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
            // The protocol factory also serves as the fallback, so every connection runs the bounded protocol.
            BoundedSubscriptionProtocolFactory protocolFactory = new BoundedSubscriptionProtocolFactory(
                    configuration.getObjectMapper(), configuration.getInvocationInputFactory(), configuration.getGraphQLInvoker());
            bind(new GraphQLWebsocketServlet(Collections.singletonList(protocolFactory), protocolFactory),
                    configuration.getInvocationInputFactory());
        } catch (Exception e) {
            logger.error("Cannot get schema from GQL servlet", e);
        }
        if (CLOSINGS.get() == null) {
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, runnable -> {
                Thread thread = new Thread(runnable, "graphql-websocket-closing");
                thread.setDaemon(true);
                return thread;
            });
            // The init deadline of a released connection leaves the queue when it is cancelled.
            executor.setRemoveOnCancelPolicy(true);
            CLOSINGS.set(executor);
        }
    }

    @Deactivate
    public void deactivate() {
        ScheduledExecutorService closings = CLOSINGS.getAndSet(null);
        if (closings != null) {
            closings.shutdownNow();
        }
        delegate.beginShutDown();
        bind(null, null);
    }

    private static void bind(GraphQLWebsocketServlet servlet, GraphQLInvocationInputFactory inputFactory) {
        delegate = servlet;
        invocationInputFactory = inputFactory;
    }

    /**
     * Key, in the user properties of the handshake ({@link EndpointConfig#getUserProperties()}) that the handshake
     * hands to {@link #onOpen}, of whether its Origin is allowed.
     */
    static final String ORIGIN_ALLOWED = "org.jahia.modules.graphql.originAllowed";

    private static final String ORIGIN_HEADER = "Origin";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        Map<String, Object> userProperties = endpointConfig.getUserProperties();
        if (!Boolean.TRUE.equals(userProperties.get(ORIGIN_ALLOWED))) {
            closeAfterOpen(() -> ConnectionClosing.close(session, ConnectionClosing.ORIGIN_NOT_ALLOWED));
            return;
        }
        if (!SUBPROTOCOL.equals(session.getNegotiatedSubprotocol())) {
            closeAfterOpen(() -> ConnectionClosing.close(session, ConnectionClosing.UNSUPPORTED_SUBPROTOCOL));
            return;
        }
        if (!Boolean.TRUE.equals(userProperties.get(SUBSCRIPTIONS_GRANTED))) {
            closeAfterOpen(() -> ConnectionClosing.close(session, ConnectionClosing.NOT_GRANTED));
            return;
        }

        JahiaUser user = (JahiaUser) userProperties.get(Constants.SESSION_USER);
        String userKey = JahiaUserManagerService.isGuest(user) ? null : user.getUserKey();
        CloseReason full = connections.acquire(userKey);
        if (full != null) {
            closeAfterOpen(() -> ConnectionClosing.close(session, full));
            return;
        }
        Accepted connection = new Accepted(userKey);
        accepted.put(session, connection);

        JCRSessionFactory.getInstance().setCurrentUser(user);
        try {
            delegate.onOpen(session, endpointConfig);
        } catch (RuntimeException e) {
            release(session);
            throw e;
        } finally {
            JCRSessionFactory.getInstance().setCurrentUser(null);
        }
        scheduleInitDeadline(session, connection);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        if (release(session)) {
            delegate.onClose(session, closeReason);
        }
    }

    @Override
    public void onError(Session session, Throwable thr) {
        if (accepted.containsKey(session)) {
            delegate.onError(session, thr);
        }
    }

    /**
     * Uncounts an accepted session and cancels its init deadline, once.
     *
     * @return {@code true} if the session was accepted and not released yet
     */
    private static boolean release(Session session) {
        Accepted connection = accepted.remove(session);
        if (connection == null) {
            return false;
        }
        connections.release(connection.userKey);
        Future<?> deadline = connection.initDeadline.get();
        if (deadline != null) {
            deadline.cancel(false);
        }
        return true;
    }

    static boolean isAccepted(Session session) {
        return accepted.containsKey(session);
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

    private static void scheduleInitDeadline(Session session, Accepted connection) {
        ScheduledExecutorService executor = CLOSINGS.get();
        int timeout = WebSocketLimits.getInitTimeoutSeconds();
        if (executor == null || timeout <= 0) {
            return;
        }
        try {
            connection.initDeadline.set(executor.schedule(() -> closeIfNotInitialized(session), timeout, TimeUnit.SECONDS));
        } catch (RejectedExecutionException e) {
            // The endpoint is shutting down and closes every session.
        }
    }

    static void closeIfNotInitialized(Session session) {
        if (session.isOpen() && !Boolean.TRUE.equals(session.getUserProperties().get(BoundedSubscriptionConsumer.INITIALIZED))) {
            ConnectionClosing.close(session, ConnectionClosing.INIT_TIMEOUT);
        }
    }

    /**
     * Returns the schema the subscriptions run on, or null before the servlet is bound. The schema provider changes
     * each time the schema is rebuilt, so it is read at each call.
     */
    static GraphQLSchema currentSchema() {
        GraphQLInvocationInputFactory inputFactory = invocationInputFactory;
        return inputFactory == null ? null : inputFactory.getSchemaProvider().getSchema();
    }

    /**
     * Tells whether the scopes of the current request grant at least one field of the subscription type, with the
     * check that each field applies when it runs.
     */
    static boolean grantsAnySubscription(PermissionService permissionService, GraphQLSchema schema) {
        if (permissionService == null || schema == null || schema.getSubscriptionType() == null) {
            return false;
        }
        GraphQLObjectType subscriptionType = schema.getSubscriptionType();
        String typeName = GqlTypeUtil.getTypeName(subscriptionType);
        for (GraphQLFieldDefinition field : subscriptionType.getFieldDefinitions()) {
            try {
                if (permissionService.hasPermission("graphql." + typeName + "." + field.getName(), (Node) null)) {
                    return true;
                }
            } catch (RepositoryException e) {
                STATIC_LOGGER.debug("Cannot check the access to {}", field.getName(), e);
            }
        }
        return false;
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

    /**
     * An accepted session: the key of the user it counts for, null for a guest, and the task that closes it if it
     * sends no {@code connection_init} in time.
     */
    private static final class Accepted {
        private final String userKey;
        private final AtomicReference<Future<?>> initDeadline = new AtomicReference<>();

        private Accepted(String userKey) {
            this.userKey = userKey;
        }
    }

    public static class Configurator extends ServerEndpointConfig.Configurator {

        private static final Logger LOGGER = LoggerFactory.getLogger(Configurator.class);

        /**
         * The refused handshakes are reported at most once per minute, with the number of refusals since the previous
         * report.
         */
        private static final ThrottledReport REFUSED_ORIGINS = new ThrottledReport(TimeUnit.MINUTES.toNanos(1));

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            List<String> origins = request.getHeaders().get(ORIGIN_HEADER);
            boolean originAllowed = isAllowedHandshake(origins, request.getRequestURI());
            sec.getUserProperties().put(ORIGIN_ALLOWED, originAllowed);
            if (!originAllowed) {
                int refusals = REFUSED_ORIGINS.count();
                if (refusals > 0) {
                    LOGGER.warn("Rejected non-same-origin GraphQL subscription connection (origin={}; {} such refusals since the last report)",
                            origins, refusals);
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
            sec.getUserProperties().put(SUBSCRIPTIONS_GRANTED, grantsAnySubscription(permissionService, currentSchema()));
        }
    }

}
