package org.jahia.modules.graphql.provider.dxm;

import graphql.Scalars;
import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import graphql.kickstart.servlet.config.DefaultGraphQLSchemaServletProvider;
import graphql.kickstart.servlet.config.GraphQLSchemaServletProvider;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.websocket.BoundedSubscriptionConsumer;
import org.jahia.modules.graphql.provider.dxm.websocket.ConnectionClosing;
import org.jahia.services.securityfilter.PermissionService;
import org.jahia.services.usermanager.JahiaUser;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

import javax.jcr.Node;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.ORIGIN_ALLOWED;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.SUBPROTOCOL;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.SUBSCRIPTIONS_GRANTED;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.isAllowedHandshake;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.isSameOrigin;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OsgiGraphQLWsEndpointTest {

    private static final URI LOCAL = URI.create("ws://localhost:8080/modules/graphqlws");
    private static final URI PUBLIC = URI.create("wss://www.example.com/modules/graphqlws");
    private static final String DEFAULT_CONFIG_PID = "org.jahia.modules.graphql.provider~default";

    @Test
    public void acceptsTheOriginTheRequestIsAddressedTo() {
        assertTrue(isSameOrigin("http://localhost:8080", LOCAL));
        assertTrue(isSameOrigin("https://www.example.com", PUBLIC));
    }

    @Test
    public void comparesHostsIgnoringCaseAndPortsWithTheirDefault() {
        assertTrue(isSameOrigin("http://LocalHost:8080", LOCAL));
        assertTrue(isSameOrigin("https://www.example.com:443", PUBLIC));
        assertTrue(isSameOrigin("https://www.example.com", URI.create("wss://www.example.com:443/modules/graphqlws")));
        assertTrue(isSameOrigin("http://localhost:80", URI.create("ws://localhost/modules/graphqlws")));
    }

    @Test
    public void acceptsAnUnderscoreOrIpv6Host() {
        assertTrue(isSameOrigin("http://jahia_dev:8080", URI.create("ws://jahia_dev:8080/modules/graphqlws")));
        assertTrue(isSameOrigin("http://[::1]:8080", URI.create("ws://[::1]:8080/modules/graphqlws")));
    }

    @Test
    public void refusesAnotherPortSchemeOrHost() {
        assertFalse(isSameOrigin("http://localhost:9999", LOCAL));
        assertFalse(isSameOrigin("http://localhost", LOCAL));
        assertFalse(isSameOrigin("https://localhost:8080", LOCAL));
        assertFalse(isSameOrigin("http://www.example.com", PUBLIC));
        assertFalse(isSameOrigin("https://other.example.com", PUBLIC));
        assertFalse(isSameOrigin("https://www.example.com.other.net", PUBLIC));
        assertFalse(isSameOrigin("https://www.example.com:80", PUBLIC));
        assertFalse(isSameOrigin("http://localhost:443", URI.create("ws://localhost/modules/graphqlws")));
    }

    @Test
    public void refusesAnyOtherValue() {
        for (String origin : asList(null, "", "null", "file://", "localhost:8080", "ws://localhost:8080", "http://localhost:8080/",
                "http://user@localhost:8080", "http://localhost:8080?a=b", "http://localhost:8080#x", "http://localhost:+8080")) {
            assertFalse(String.valueOf(origin), isSameOrigin(origin, LOCAL));
        }
    }

    @Test
    public void refusesAnOriginWhenTheRequestAddressIsUnknown() {
        assertFalse(isSameOrigin("http://localhost:8080", null));
        assertFalse(isSameOrigin("http://localhost:8080", URI.create("/modules/graphqlws")));
    }

    @Test
    public void acceptsAHandshakeWithoutOrigin() {
        assertTrue(isAllowedHandshake(null, LOCAL));
        assertTrue(isAllowedHandshake(Collections.emptyList(), LOCAL));
    }

    @Test
    public void acceptsOneOriginHeaderNamingTheRequestAddressOnly() {
        assertTrue(isAllowedHandshake(Collections.singletonList("http://localhost:8080"), LOCAL));
        assertFalse(isAllowedHandshake(Collections.singletonList("null"), LOCAL));
        assertFalse(isAllowedHandshake(asList("http://localhost:8080", "http://localhost:8080"), LOCAL));
    }

    @Test
    public void modifyHandshakeRefusesTwoOriginHeaders() {
        Map<String, List<String>> twoOrigins = new HashMap<>();
        twoOrigins.put("Origin", asList("http://localhost:8080", "http://localhost:9999"));
        assertEquals(Boolean.FALSE, handshake(twoOrigins, LOCAL).get(ORIGIN_ALLOWED));
    }

    @Test
    public void modifyHandshakeRefusesARequestWithNoAddress() {
        Map<String, List<String>> origin = new HashMap<>();
        origin.put("Origin", Collections.singletonList("http://localhost:8080"));
        assertEquals(Boolean.FALSE, handshake(origin, null).get(ORIGIN_ALLOWED));
    }

    private static Map<String, Object> handshake(Map<String, List<String>> headers, URI requestUri) {
        Map<String, Object> userProperties = new HashMap<>();
        ServerEndpointConfig sec = mock(ServerEndpointConfig.class);
        when(sec.getUserProperties()).thenReturn(userProperties);
        HandshakeRequest request = mock(HandshakeRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRequestURI()).thenReturn(requestUri);
        new OsgiGraphQLWsEndpoint.Configurator().modifyHandshake(sec, request, null);
        return userProperties;
    }

    @Test
    public void onOpenClosesASessionWhoseOriginIsNotAllowed() throws Exception {
        Session session = session();

        new OsgiGraphQLWsEndpoint().onOpen(session, config(Boolean.FALSE));

        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session).close(reason.capture());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
        assertEquals(1000L, session.getUserProperties().get("org.apache.tomcat.websocket.SESSION_CLOSE_TIMEOUT"));
        assertFalse(OsgiGraphQLWsEndpoint.isAccepted(session));
    }

    @Test
    public void onOpenClosesARefusedSessionOnceItHasReturned() throws Exception {
        GraphQLWebsocketServlet delegate = mockDelegate();
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        closings().set(executor);
        Session session = session();

        new OsgiGraphQLWsEndpoint().onOpen(session, config(Boolean.FALSE));

        verify(session, never()).close(any(CloseReason.class));
        ArgumentCaptor<Runnable> close = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).schedule(close.capture(), eq(100L), eq(TimeUnit.MILLISECONDS));
        close.getValue().run();
        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session).close(reason.capture());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
        verify(delegate, never()).onOpen(any(Session.class), any(EndpointConfig.class));
    }

    @Test
    public void onOpenClosesASessionWithNoOriginDecision() throws Exception {
        GraphQLWebsocketServlet delegate = mockDelegate();
        Session session = session();

        new OsgiGraphQLWsEndpoint().onOpen(session, config(null));

        verify(session).close(any(CloseReason.class));
        verify(delegate, never()).onOpen(any(Session.class), any(EndpointConfig.class));
    }

    @Test
    public void onCloseAndOnErrorLeaveARefusedSessionAlone() {
        GraphQLWebsocketServlet delegate = mockDelegate();
        Session session = session();
        OsgiGraphQLWsEndpoint endpoint = new OsgiGraphQLWsEndpoint();

        endpoint.onClose(session, new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Origin not allowed"));
        endpoint.onError(session, new IllegalStateException());

        verify(delegate, never()).onClose(any(Session.class), any(CloseReason.class));
        verify(delegate, never()).onError(any(Session.class), any(Throwable.class));
    }

    @Test
    public void onCloseAndOnErrorHandAnAcceptedSessionToTheDelegate() {
        GraphQLWebsocketServlet delegate = mockDelegate();
        Session session = session(SUBPROTOCOL);
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, null);
        Throwable error = new IllegalStateException();
        OsgiGraphQLWsEndpoint endpoint = new OsgiGraphQLWsEndpoint();
        endpoint.onOpen(session, accepted(user("root")));

        // The container reports an error before it closes the session.
        endpoint.onError(session, error);
        endpoint.onClose(session, reason);
        endpoint.onClose(session, reason);

        verify(delegate).onError(session, error);
        verify(delegate).onClose(session, reason);
    }

    @Test
    public void onCloseReleasesASessionThatIsAlreadyClosed() throws Exception {
        setLimit("graphql.websocket.maxConnections", "1");
        GraphQLWebsocketServlet delegate = mockDelegate();
        OsgiGraphQLWsEndpoint endpoint = new OsgiGraphQLWsEndpoint();
        Session first = session(SUBPROTOCOL);
        Session second = session(SUBPROTOCOL);
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Service is unregistered");

        endpoint.onOpen(first, accepted(user("root")));
        // A closed session refuses access to its user properties.
        when(first.getUserProperties()).thenThrow(new IllegalStateException());
        endpoint.onClose(first, reason);
        endpoint.onOpen(second, accepted(user("root")));
        endpoint.onClose(second, reason);

        verify(delegate).onClose(first, reason);
        verify(delegate).onOpen(eq(second), any(EndpointConfig.class));
    }

    @Test
    public void onOpenClosesASessionWithAnotherSubprotocol() throws Exception {
        GraphQLWebsocketServlet delegate = mockDelegate();
        for (String subprotocol : asList("", "graphql-transport-ws", null)) {
            Session session = session(subprotocol);

            new OsgiGraphQLWsEndpoint().onOpen(session, accepted(user("root")));

            verify(session).close(ConnectionClosing.UNSUPPORTED_SUBPROTOCOL);
        }
        verify(delegate, never()).onOpen(any(Session.class), any(EndpointConfig.class));
    }

    @Test
    public void onOpenClosesASessionGrantedNoSubscriptionOnceItHasReturned() throws Exception {
        GraphQLWebsocketServlet delegate = mockDelegate();
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        closings().set(executor);
        Session session = session(SUBPROTOCOL);
        EndpointConfig config = accepted(null);
        config.getUserProperties().put(SUBSCRIPTIONS_GRANTED, Boolean.FALSE);

        new OsgiGraphQLWsEndpoint().onOpen(session, config);

        verify(session, never()).close(any(CloseReason.class));
        ArgumentCaptor<Runnable> close = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).schedule(close.capture(), eq(100L), eq(TimeUnit.MILLISECONDS));
        close.getValue().run();
        verify(session).close(ConnectionClosing.NOT_GRANTED);
        verify(delegate, never()).onOpen(any(Session.class), any(EndpointConfig.class));
    }

    @Test
    public void onOpenClosesASessionGrantedNoSubscription() throws Exception {
        GraphQLWebsocketServlet delegate = mockDelegate();
        Session session = session(SUBPROTOCOL);
        EndpointConfig config = accepted(null);
        config.getUserProperties().put(SUBSCRIPTIONS_GRANTED, Boolean.FALSE);

        new OsgiGraphQLWsEndpoint().onOpen(session, config);

        verify(session).close(ConnectionClosing.NOT_GRANTED);
        verify(delegate, never()).onOpen(any(Session.class), any(EndpointConfig.class));
    }

    @Test
    public void onOpenBoundsTheOpenConnectionsAndOnCloseReleasesThem() throws Exception {
        setLimit("graphql.websocket.maxConnections", "1");
        GraphQLWebsocketServlet delegate = mockDelegate();
        OsgiGraphQLWsEndpoint endpoint = new OsgiGraphQLWsEndpoint();
        Session first = session(SUBPROTOCOL);
        Session second = session(SUBPROTOCOL);
        Session third = session(SUBPROTOCOL);

        endpoint.onOpen(first, accepted(user("root")));
        endpoint.onOpen(second, accepted(user("guest")));
        endpoint.onClose(first, new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, null));
        endpoint.onOpen(third, accepted(user("guest")));
        endpoint.onClose(third, new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, null));

        verify(delegate).onOpen(eq(first), any(EndpointConfig.class));
        verify(second).close(ConnectionClosing.TOO_MANY_CONNECTIONS);
        verify(delegate, never()).onOpen(eq(second), any(EndpointConfig.class));
        verify(delegate).onOpen(eq(third), any(EndpointConfig.class));
        assertFalse(OsgiGraphQLWsEndpoint.isAccepted(first));
    }

    @Test
    public void onOpenBoundsTheOpenConnectionsOfAUser() throws Exception {
        setLimit("graphql.websocket.maxConnectionsPerUser", "1");
        mockDelegate();
        OsgiGraphQLWsEndpoint endpoint = new OsgiGraphQLWsEndpoint();
        Session first = session(SUBPROTOCOL);
        Session second = session(SUBPROTOCOL);
        Session guest = session(SUBPROTOCOL);
        Session otherGuest = session(SUBPROTOCOL);

        endpoint.onOpen(first, accepted(user("root")));
        endpoint.onOpen(second, accepted(user("root")));
        endpoint.onOpen(guest, accepted(user("guest")));
        endpoint.onOpen(otherGuest, accepted(null));

        verify(second).close(ConnectionClosing.TOO_MANY_USER_CONNECTIONS);
        verify(guest, never()).close(any(CloseReason.class));
        verify(otherGuest, never()).close(any(CloseReason.class));
        for (Session session : asList(first, guest, otherGuest)) {
            endpoint.onClose(session, new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, null));
        }
    }

    @Test
    public void closesASessionThatSentNoConnectionInit() throws Exception {
        Session silent = session(SUBPROTOCOL);
        Session initialized = session(SUBPROTOCOL);
        initialized.getUserProperties().put(BoundedSubscriptionConsumer.INITIALIZED, Boolean.TRUE);

        OsgiGraphQLWsEndpoint.closeIfNotInitialized(silent);
        OsgiGraphQLWsEndpoint.closeIfNotInitialized(initialized);

        verify(silent).close(ConnectionClosing.INIT_TIMEOUT);
        verify(initialized, never()).close(any(CloseReason.class));
    }

    @Test
    public void grantsSubscriptionsWhenTheScopesGrantOneOfItsFields() throws Exception {
        GraphQLSchema schema = schemaWithSubscriptions("first", "second");
        PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.hasPermission(eq("graphql.Subscription.second"), nullable(Node.class))).thenReturn(true);

        assertTrue(OsgiGraphQLWsEndpoint.grantsAnySubscription(permissionService, schema));
        assertFalse(OsgiGraphQLWsEndpoint.grantsAnySubscription(mock(PermissionService.class), schema));
        assertFalse(OsgiGraphQLWsEndpoint.grantsAnySubscription(null, schema));
        assertFalse(OsgiGraphQLWsEndpoint.grantsAnySubscription(permissionService, null));
        assertFalse(OsgiGraphQLWsEndpoint.grantsAnySubscription(permissionService, schemaWithSubscriptions()));
    }

    @Test
    public void readsTheSchemaRebuiltAfterTheServletIsBound() {
        AtomicReference<GraphQLSchemaServletProvider> current =
                new AtomicReference<>(new DefaultGraphQLSchemaServletProvider(schemaWithSubscriptions()));
        Whitebox.setInternalState(OsgiGraphQLWsEndpoint.class, "invocationInputFactory",
                GraphQLInvocationInputFactory.newBuilder(current::get).build());
        assertNull(OsgiGraphQLWsEndpoint.currentSchema().getSubscriptionType());

        GraphQLSchema rebuilt = schemaWithSubscriptions("first");
        current.set(new DefaultGraphQLSchemaServletProvider(rebuilt));

        assertSame(rebuilt, OsgiGraphQLWsEndpoint.currentSchema());
    }

    private static GraphQLSchema schemaWithSubscriptions(String... fields) {
        GraphQLObjectType query = GraphQLObjectType.newObject().name("Query")
                .field(GraphQLFieldDefinition.newFieldDefinition().name("a").type(Scalars.GraphQLString)).build();
        GraphQLSchema.Builder schema = GraphQLSchema.newSchema().query(query);
        if (fields.length > 0) {
            GraphQLObjectType.Builder subscription = GraphQLObjectType.newObject().name("Subscription");
            for (String field : fields) {
                subscription.field(GraphQLFieldDefinition.newFieldDefinition().name(field).type(Scalars.GraphQLString));
            }
            schema.subscription(subscription.build());
        }
        return schema.build();
    }

    private static void setLimit(String key, String value) throws Exception {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(key, value);
        new DXGraphQLConfig().updated(DEFAULT_CONFIG_PID, properties);
    }

    private static EndpointConfig accepted(JahiaUser user) {
        Map<String, Object> userProperties = new HashMap<>();
        userProperties.put(ORIGIN_ALLOWED, Boolean.TRUE);
        userProperties.put(SUBSCRIPTIONS_GRANTED, Boolean.TRUE);
        if (user != null) {
            userProperties.put(Constants.SESSION_USER, user);
        }
        EndpointConfig config = mock(EndpointConfig.class);
        when(config.getUserProperties()).thenReturn(userProperties);
        return config;
    }

    private static JahiaUser user(String name) {
        JahiaUser user = mock(JahiaUser.class);
        when(user.getName()).thenReturn(name);
        when(user.getUserKey()).thenReturn("/users/" + name);
        return user;
    }

    private static Session session(String subprotocol) {
        Session session = session();
        when(session.getNegotiatedSubprotocol()).thenReturn(subprotocol);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static GraphQLWebsocketServlet mockDelegate() {
        GraphQLWebsocketServlet delegate = mock(GraphQLWebsocketServlet.class);
        Whitebox.setInternalState(OsgiGraphQLWsEndpoint.class, "delegate", delegate);
        return delegate;
    }

    private static AtomicReference<ScheduledExecutorService> closings() {
        return Whitebox.getInternalState(OsgiGraphQLWsEndpoint.class, "CLOSINGS");
    }

    @After
    public void resetStaticState() {
        Whitebox.setInternalState(OsgiGraphQLWsEndpoint.class, "delegate", (Object) null);
        Whitebox.setInternalState(OsgiGraphQLWsEndpoint.class, "invocationInputFactory", (Object) null);
        ScheduledExecutorService executor = closings().getAndSet(null);
        if (executor != null) {
            executor.shutdownNow();
        }
        new DXGraphQLConfig().deleted(DEFAULT_CONFIG_PID);
    }

    private static EndpointConfig config(Boolean originAllowed) {
        Map<String, Object> userProperties = new HashMap<>();
        if (originAllowed != null) {
            userProperties.put(ORIGIN_ALLOWED, originAllowed);
        }
        EndpointConfig config = mock(EndpointConfig.class);
        when(config.getUserProperties()).thenReturn(userProperties);
        return config;
    }

    private static Session session() {
        Session session = mock(Session.class);
        when(session.getUserProperties()).thenReturn(new HashMap<>());
        when(session.getId()).thenReturn("1");
        return session;
    }
}
