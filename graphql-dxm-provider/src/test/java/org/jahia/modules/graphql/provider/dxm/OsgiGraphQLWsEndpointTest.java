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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.ORIGIN_ALLOWED;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.isAllowedHandshake;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.isSameOrigin;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OsgiGraphQLWsEndpointTest {

    private static final URI LOCAL = URI.create("ws://localhost:8080/modules/graphqlws");
    private static final URI PUBLIC = URI.create("wss://www.example.com/modules/graphqlws");

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
        ScheduledExecutorService executor = closings().getAndSet(null);
        if (executor != null) {
            executor.shutdownNow();
        }
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
        when(session.getId()).thenReturn("1");
        return session;
    }
}
