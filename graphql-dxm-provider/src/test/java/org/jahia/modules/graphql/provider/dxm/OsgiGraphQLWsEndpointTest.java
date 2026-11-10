package org.jahia.modules.graphql.provider.dxm;

import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.ORIGIN_ALLOWED;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.ORIGIN_REFUSED;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.isAllowedOrigin;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OsgiGraphQLWsEndpointTest {

    private static final URI LOCAL = URI.create("ws://localhost:8080/modules/graphqlws");
    private static final URI PUBLIC = URI.create("wss://www.example.com/modules/graphqlws");
    private static final Set<String> NONE = Collections.emptySet();

    private static Set<String> configured(String... origins) {
        return new HashSet<>(asList(origins));
    }

    @Test
    public void acceptsTheOriginTheRequestIsAddressedTo() {
        assertTrue(isAllowedOrigin("http://localhost:8080", LOCAL, NONE));
        assertTrue(isAllowedOrigin("https://www.example.com", PUBLIC, NONE));
    }

    @Test
    public void comparesHostsIgnoringCaseAndPortsWithTheirDefault() {
        assertTrue(isAllowedOrigin("http://LocalHost:8080", LOCAL, NONE));
        assertTrue(isAllowedOrigin("https://www.example.com:443", PUBLIC, NONE));
        assertTrue(isAllowedOrigin("https://www.example.com", URI.create("wss://www.example.com:443/modules/graphqlws"), NONE));
    }

    @Test
    public void acceptsAHostNameThatHoldsAnUnderscore() {
        assertTrue(isAllowedOrigin("http://jahia_dev:8080", URI.create("ws://jahia_dev:8080/modules/graphqlws"), NONE));
        assertFalse(isAllowedOrigin("http://jahia_dev:8081", URI.create("ws://jahia_dev:8080/modules/graphqlws"), NONE));
    }

    @Test
    public void acceptsAnIpv6Host() {
        URI ipv6 = URI.create("ws://[::1]:8080/modules/graphqlws");
        assertTrue(isAllowedOrigin("http://[::1]:8080", ipv6, NONE));
        assertFalse(isAllowedOrigin("http://[::1]:8081", ipv6, NONE));
    }

    @Test
    public void refusesAnotherPortSchemeOrHost() {
        assertFalse(isAllowedOrigin("http://localhost:9999", LOCAL, NONE));
        assertFalse(isAllowedOrigin("http://localhost", LOCAL, NONE));
        assertFalse(isAllowedOrigin("https://localhost:8080", LOCAL, NONE));
        assertFalse(isAllowedOrigin("http://www.example.com", PUBLIC, NONE));
        assertFalse(isAllowedOrigin("https://other.example.com", PUBLIC, NONE));
        assertFalse(isAllowedOrigin("https://www.example.com.other.net", PUBLIC, NONE));
    }

    @Test
    public void refusesAValueThatIsNotASerializedOrigin() {
        for (String origin : asList(null, "", "null", "file://", "localhost:8080", "ws://localhost:8080",
                "http://localhost:8080/", "http://user@localhost:8080", "http://localhost:8080?a=b", "http://local host:8080",
                "http://localhost:8080#x", "http://:8080", "http://localhost:80800", "http://localhost:port", "http://[::1",
                "http://::1:8080")) {
            assertFalse(String.valueOf(origin), isAllowedOrigin(origin, LOCAL, NONE));
        }
    }

    @Test
    public void acceptsAConfiguredOrigin() {
        Set<String> configured = configured("https://front.example.com", "http://localhost:3000", "capacitor://localhost");
        assertTrue(isAllowedOrigin("https://front.example.com", PUBLIC, configured));
        assertTrue(isAllowedOrigin("http://localhost:3000", LOCAL, configured));
        assertTrue(isAllowedOrigin("capacitor://localhost", LOCAL, configured));
        assertFalse(isAllowedOrigin("https://front.example.com:8443", PUBLIC, configured));
        assertFalse(isAllowedOrigin("https://FRONT.example.com", PUBLIC, configured));
        assertFalse(isAllowedOrigin("capacitor://other", LOCAL, configured));
    }

    @Test
    public void refusesAConfiguredOpaqueOriginOrWildcard() {
        assertFalse(isAllowedOrigin("null", LOCAL, configured("null")));
        assertFalse(isAllowedOrigin("*", LOCAL, configured("*")));
        assertFalse(isAllowedOrigin("", LOCAL, configured("")));
    }

    @Test
    public void refusesAnUnconfiguredOriginWhenTheRequestAddressIsUnknown() {
        assertFalse(isAllowedOrigin("http://localhost:8080", null, NONE));
        assertFalse(isAllowedOrigin("http://localhost:8080", URI.create("/modules/graphqlws"), NONE));
        assertTrue(isAllowedOrigin("http://localhost:8080", null, configured("http://localhost:8080")));
    }

    @Test
    public void checkOriginRefusesAMissingOrOpaqueOrigin() {
        OsgiGraphQLWsEndpoint.Configurator configurator = new OsgiGraphQLWsEndpoint.Configurator();
        assertTrue(configurator.checkOrigin("http://localhost:8080"));
        assertFalse(configurator.checkOrigin(null));
        assertFalse(configurator.checkOrigin("null"));
        assertFalse(configurator.checkOrigin("http://:8080"));
        assertFalse(configurator.checkOrigin("http://localhost:8080#x"));
    }

    @Test
    public void modifyHandshakeRefusesTwoOriginHeadersOrNone() {
        Map<String, List<String>> twoOrigins = new HashMap<>();
        twoOrigins.put("Origin", asList("http://localhost:8080", "http://localhost:9999"));
        assertEquals(Boolean.FALSE, handshake(twoOrigins).get(ORIGIN_ALLOWED));
        assertEquals(Boolean.FALSE, handshake(new HashMap<>()).get(ORIGIN_ALLOWED));
    }

    private static Map<String, Object> handshake(Map<String, List<String>> headers) {
        Map<String, Object> userProperties = new HashMap<>();
        ServerEndpointConfig sec = mock(ServerEndpointConfig.class);
        when(sec.getUserProperties()).thenReturn(userProperties);
        HandshakeRequest request = mock(HandshakeRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRequestURI()).thenReturn(LOCAL);
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

    @Test
    public void checkOriginAcceptsAConfiguredOriginOfAnotherScheme() {
        DXGraphQLConfig config = mock(DXGraphQLConfig.class);
        when(config.getCorsOrigins()).thenReturn(Collections.singleton("capacitor://localhost"));
        new OsgiGraphQLWsEndpoint().setConfig(config);
        OsgiGraphQLWsEndpoint.Configurator configurator = new OsgiGraphQLWsEndpoint.Configurator();

        assertTrue(configurator.checkOrigin("capacitor://localhost"));
        assertFalse(configurator.checkOrigin("capacitor://other"));
    }

    private static GraphQLWebsocketServlet mockDelegate() {
        GraphQLWebsocketServlet delegate = mock(GraphQLWebsocketServlet.class);
        Whitebox.setInternalState(OsgiGraphQLWsEndpoint.class, "delegate", delegate);
        return delegate;
    }

    @After
    public void resetStaticState() {
        Whitebox.setInternalState(OsgiGraphQLWsEndpoint.class, "delegate", (Object) null);
        new OsgiGraphQLWsEndpoint().setConfig(null);
    }

    private static Session session() {
        Session session = mock(Session.class);
        Map<String, Object> userProperties = new HashMap<>();
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getId()).thenReturn("1");
        return session;
    }
}
