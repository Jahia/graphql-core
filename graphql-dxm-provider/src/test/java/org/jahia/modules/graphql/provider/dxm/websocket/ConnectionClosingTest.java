package org.jahia.modules.graphql.provider.dxm.websocket;

import org.junit.Test;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConnectionClosingTest {

    @Test
    public void closesWithAShortCloseTimeout() throws Exception {
        Session session = mock(Session.class);
        Map<String, Object> userProperties = new HashMap<>();
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getId()).thenReturn("1");

        ConnectionClosing.close(session, ConnectionClosing.INVALID_MESSAGE);

        assertEquals(1000L, userProperties.get(ConnectionClosing.TOMCAT_SESSION_CLOSE_TIMEOUT));
        verify(session).close(ConnectionClosing.INVALID_MESSAGE);
    }

    @Test
    public void leavesAClosedSessionAlone() throws Exception {
        Session session = mock(Session.class);
        when(session.getUserProperties()).thenThrow(new IllegalStateException());
        when(session.getId()).thenReturn("1");

        ConnectionClosing.close(session, ConnectionClosing.INIT_TIMEOUT);

        verify(session, never()).close(any(CloseReason.class));
    }
}
