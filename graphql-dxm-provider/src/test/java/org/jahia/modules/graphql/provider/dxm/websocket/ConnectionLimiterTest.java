package org.jahia.modules.graphql.provider.dxm.websocket;

import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConnectionLimiterTest {

    private static final String DEFAULT_CONFIG_PID = "org.jahia.modules.graphql.provider~default";

    private final ConnectionLimiter limiter = new ConnectionLimiter();

    @Before
    public void setLimits() throws ConfigurationException {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("graphql.websocket.maxConnections", "3");
        properties.put("graphql.websocket.maxConnectionsPerUser", "2");
        new DXGraphQLConfig().updated(DEFAULT_CONFIG_PID, properties);
    }

    @After
    public void restoreLimits() {
        new DXGraphQLConfig().deleted(DEFAULT_CONFIG_PID);
    }

    @Test
    public void boundsTheConnectionsOfAUser() {
        assertNull(limiter.acquire("/users/a"));
        assertNull(limiter.acquire("/users/a"));
        assertEquals(ConnectionClosing.TOO_MANY_USER_CONNECTIONS, limiter.acquire("/users/a"));
        assertNull(limiter.acquire("/users/b"));
        assertEquals(3, limiter.getOpen());
    }

    @Test
    public void boundsTheConnectionsInAllGuestsIncluded() {
        assertNull(limiter.acquire(null));
        assertNull(limiter.acquire(null));
        assertNull(limiter.acquire(null));
        assertEquals(ConnectionClosing.TOO_MANY_CONNECTIONS, limiter.acquire(null));
        assertEquals(ConnectionClosing.TOO_MANY_CONNECTIONS, limiter.acquire("/users/a"));
    }

    @Test
    public void releaseMakesRoomAgain() {
        limiter.acquire("/users/a");
        limiter.acquire("/users/a");

        limiter.release("/users/a");

        assertNull(limiter.acquire("/users/a"));
        assertEquals(2, limiter.getOpen());
    }
}
