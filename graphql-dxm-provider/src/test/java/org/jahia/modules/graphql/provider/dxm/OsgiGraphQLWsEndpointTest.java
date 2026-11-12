package org.jahia.modules.graphql.provider.dxm;

import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.ORIGIN_ALLOWED;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.ORIGIN_REFUSED;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.isAllowedOrigin;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OsgiGraphQLWsEndpointTest {

    private static final URI LOCAL = URI.create("ws://localhost:8080/modules/graphqlws");
    private static final URI PUBLIC = URI.create("wss://www.example.com/modules/graphqlws");
    @Test
    public void acceptsTheOriginTheRequestIsAddressedTo() {
        assertTrue(isAllowedOrigin("http://localhost:8080", LOCAL));
        assertTrue(isAllowedOrigin("https://www.example.com", PUBLIC));
    }

    @Test
    public void comparesHostsIgnoringCaseAndPortsWithTheirDefault() {
        assertTrue(isAllowedOrigin("http://LocalHost:8080", LOCAL));
        assertTrue(isAllowedOrigin("https://www.example.com:443", PUBLIC));
        assertTrue(isAllowedOrigin("https://www.example.com", URI.create("wss://www.example.com:443/modules/graphqlws")));
    }

    @Test
    public void acceptsAHostNameThatHoldsAnUnderscore() {
        assertTrue(isAllowedOrigin("http://jahia_dev:8080", URI.create("ws://jahia_dev:8080/modules/graphqlws")));
        assertFalse(isAllowedOrigin("http://jahia_dev:8081", URI.create("ws://jahia_dev:8080/modules/graphqlws")));
    }

    @Test
    public void acceptsAnIpv6Host() {
        URI ipv6 = URI.create("ws://[::1]:8080/modules/graphqlws");
        assertTrue(isAllowedOrigin("http://[::1]:8080", ipv6));
        assertFalse(isAllowedOrigin("http://[::1]:8081", ipv6));
    }

    @Test
    public void refusesAnotherPortSchemeOrHost() {
        assertFalse(isAllowedOrigin("http://localhost:9999", LOCAL));
        assertFalse(isAllowedOrigin("http://localhost", LOCAL));
        assertFalse(isAllowedOrigin("https://localhost:8080", LOCAL));
        assertFalse(isAllowedOrigin("http://www.example.com", PUBLIC));
        assertFalse(isAllowedOrigin("https://other.example.com", PUBLIC));
        assertFalse(isAllowedOrigin("https://www.example.com.other.net", PUBLIC));
    }

    @Test
    public void refusesAValueThatIsNotASerializedOrigin() {
        for (String origin : asList(null, "", "null", "file://", "localhost:8080", "ws://localhost:8080",
                "http://localhost:8080/", "http://user@localhost:8080", "http://localhost:8080?a=b", "http://local host:8080",
                "http://localhost:8080#x", "http://:8080", "http://localhost:80800", "http://localhost:port", "http://[::1",
                "http://::1:8080")) {
            assertFalse(String.valueOf(origin), isAllowedOrigin(origin, LOCAL));
        }
    }

    @Test
    public void refusesAnOriginWhenTheRequestAddressIsUnknown() {
        assertFalse(isAllowedOrigin("http://localhost:8080", null));
        assertFalse(isAllowedOrigin("http://localhost:8080", URI.create("/modules/graphqlws")));
    }

    @Test
    public void checkOriginRefusesAMissingOrOpaqueOrigin() {
        OsgiGraphQLWsEndpoint.Configurator configurator = new OsgiGraphQLWsEndpoint.Configurator();
        assertTrue(configurator.checkOrigin("http://localhost:8080"));
        assertFalse(configurator.checkOrigin(null));
        assertFalse(configurator.checkOrigin("null"));
        assertFalse(configurator.checkOrigin("http://:8080"));
        assertFalse(configurator.checkOrigin("http://localhost:8080#x"));
        assertFalse(configurator.checkOrigin("capacitor://localhost"));
        for (String port : asList("80800", "+8080", "-1", "")) {
            assertFalse(port, configurator.checkOrigin("http://localhost:" + port));
        }
    }

    @Test
    public void refusesAPortThatIsNotWrittenAsADecimalNumber() {
        URI defaultPort = URI.create("ws://localhost/modules/graphqlws");
        assertTrue(isAllowedOrigin("http://localhost", defaultPort));
        assertFalse(isAllowedOrigin("http://localhost:-1", defaultPort));
        assertFalse(isAllowedOrigin("http://localhost:", defaultPort));
        assertFalse(isAllowedOrigin("http://localhost:+8080", LOCAL));
    }

    @Test
    public void modifyHandshakeRefusesTwoOriginHeadersOrNone() {
        Map<String, List<String>> twoOrigins = new HashMap<>();
        twoOrigins.put("Origin", asList("http://localhost:8080", "http://localhost:9999"));
        assertEquals(Boolean.FALSE, handshake(twoOrigins).get(ORIGIN_ALLOWED));
        assertEquals(Boolean.FALSE, handshake(new HashMap<>()).get(ORIGIN_ALLOWED));
    }

    @Test
    public void modifyHandshakeRefusesARequestWithNoAddress() {
        Map<String, List<String>> origin = new HashMap<>();
        origin.put("Origin", Collections.singletonList("http://localhost:8080"));
        assertEquals(Boolean.FALSE, handshake(origin, null).get(ORIGIN_ALLOWED));
    }

    private static Map<String, Object> handshake(Map<String, List<String>> headers) {
        return handshake(headers, LOCAL);
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
        EndpointConfig config = mock(EndpointConfig.class);
        when(config.getUserProperties()).thenReturn(Collections.singletonMap(ORIGIN_ALLOWED, Boolean.FALSE));

        new OsgiGraphQLWsEndpoint().onOpen(session, config);

        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session).close(reason.capture());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
        assertEquals(Boolean.TRUE, session.getUserProperties().get(ORIGIN_REFUSED));
    }

    @Test
    public void onOpenClosesASessionWithNoOriginDecision() throws Exception {
        GraphQLWebsocketServlet delegate = mockDelegate();
        Session session = session();
        EndpointConfig config = mock(EndpointConfig.class);
        when(config.getUserProperties()).thenReturn(new HashMap<>());

        new OsgiGraphQLWsEndpoint().onOpen(session, config);

        verify(session).close(any(CloseReason.class));
        verify(delegate, never()).onOpen(any(Session.class), any(EndpointConfig.class));
    }

    @Test
    public void onOpenMarksASessionBeforeClosingIt() throws Exception {
        GraphQLWebsocketServlet delegate = mockDelegate();
        Session session = session();
        EndpointConfig config = mock(EndpointConfig.class);
        when(config.getUserProperties()).thenReturn(Collections.singletonMap(ORIGIN_ALLOWED, Boolean.FALSE));
        OsgiGraphQLWsEndpoint endpoint = new OsgiGraphQLWsEndpoint();
        // A container may call onClose from within close(), before close() returns.
        doAnswer(invocation -> {
            endpoint.onClose(session, invocation.getArgument(0));
            return null;
        }).when(session).close(any(CloseReason.class));

        endpoint.onOpen(session, config);

        verify(delegate, never()).onClose(any(Session.class), any(CloseReason.class));
    }

    @Test
    public void onCloseAndOnErrorLeaveARefusedSessionAlone() {
        GraphQLWebsocketServlet delegate = mockDelegate();
        Session session = session();
        session.getUserProperties().put(ORIGIN_REFUSED, Boolean.TRUE);
        OsgiGraphQLWsEndpoint endpoint = new OsgiGraphQLWsEndpoint();

        endpoint.onClose(session, new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Origin not allowed"));
        endpoint.onError(session, new IllegalStateException());

        verify(delegate, never()).onClose(any(Session.class), any(CloseReason.class));
        verify(delegate, never()).onError(any(Session.class), any(Throwable.class));
    }

    @Test
    public void onCloseAndOnErrorHandAnAcceptedSessionToTheDelegate() {
        GraphQLWebsocketServlet delegate = mockDelegate();
        Session session = session();
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, null);
        Throwable error = new IllegalStateException();
        OsgiGraphQLWsEndpoint endpoint = new OsgiGraphQLWsEndpoint();

        endpoint.onClose(session, reason);
        endpoint.onError(session, error);

        verify(delegate).onClose(session, reason);
        verify(delegate).onError(session, error);
    }

    private static GraphQLWebsocketServlet mockDelegate() {
        GraphQLWebsocketServlet delegate = mock(GraphQLWebsocketServlet.class);
        Whitebox.setInternalState(OsgiGraphQLWsEndpoint.class, "delegate", delegate);
        return delegate;
    }

    @After
    public void resetStaticState() {
        Whitebox.setInternalState(OsgiGraphQLWsEndpoint.class, "delegate", (Object) null);
    }

    private static Session session() {
        Session session = mock(Session.class);
        Map<String, Object> userProperties = new HashMap<>();
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getId()).thenReturn("1");
        return session;
    }
}
