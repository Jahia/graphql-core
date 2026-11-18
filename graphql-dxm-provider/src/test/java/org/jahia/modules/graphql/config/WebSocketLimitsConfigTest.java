package org.jahia.modules.graphql.config;

import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.config.WebSocketLimits;
import org.junit.After;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WebSocketLimitsConfigTest {

    private static final String DEFAULT_CONFIG_PID = "org.jahia.modules.graphql.provider~default";
    private static final String OTHER_CONFIG_PID = "org.jahia.modules.graphql.provider~custom";

    private final DXGraphQLConfig config = new DXGraphQLConfig();

    @After
    public void restoreDefaults() {
        config.deleted(DEFAULT_CONFIG_PID);
    }

    private static Dictionary<String, Object> props(String... keyValues) {
        Dictionary<String, Object> dict = new Hashtable<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            dict.put(keyValues[i], keyValues[i + 1]);
        }
        return dict;
    }

    @Test
    public void appliesTheCodeDefaultsWithoutConfiguration() {
        config.deleted(DEFAULT_CONFIG_PID);

        assertEquals(10, WebSocketLimits.getInitTimeoutSeconds());
        assertEquals(1000, WebSocketLimits.getMaxConnections());
        assertEquals(20, WebSocketLimits.getMaxConnectionsPerUser());
        assertEquals(50, WebSocketLimits.getMaxSubscriptionsPerConnection());
    }

    @Test
    public void appliesTheValuesOfTheDefaultConfiguration() throws ConfigurationException {
        config.updated(DEFAULT_CONFIG_PID, props("graphql.websocket.initTimeout", "5", "graphql.websocket.maxConnections", "0",
                "graphql.websocket.maxConnectionsPerUser", "3", "graphql.websocket.maxSubscriptionsPerConnection", "7"));

        assertEquals(5, WebSocketLimits.getInitTimeoutSeconds());
        assertEquals(0, WebSocketLimits.getMaxConnections());
        assertEquals(3, WebSocketLimits.getMaxConnectionsPerUser());
        assertEquals(7, WebSocketLimits.getMaxSubscriptionsPerConnection());
    }

    @Test
    public void ignoresTheValuesOfAnotherConfiguration() throws ConfigurationException {
        config.updated(OTHER_CONFIG_PID, props("graphql.websocket.maxConnections", "0",
                "graphql.websocket.maxSubscriptionsPerConnection", "0"));

        assertEquals(1000, WebSocketLimits.getMaxConnections());
        assertEquals(50, WebSocketLimits.getMaxSubscriptionsPerConnection());
    }

    @Test
    public void revertsToTheCodeDefaultWhenAValueIsRemoved() throws ConfigurationException {
        config.updated(DEFAULT_CONFIG_PID, props("graphql.websocket.maxConnections", "5"));
        config.updated(DEFAULT_CONFIG_PID, props("graphql.websocket.initTimeout", "5"));

        assertEquals(1000, WebSocketLimits.getMaxConnections());
        assertEquals(5, WebSocketLimits.getInitTimeoutSeconds());
    }

    @Test
    public void rejectsANegativeValueAndKeepsTheLastOne() throws ConfigurationException {
        config.updated(DEFAULT_CONFIG_PID, props("graphql.websocket.maxConnections", "5"));
        try {
            config.updated(DEFAULT_CONFIG_PID, props("graphql.websocket.maxConnections", "-1"));
            fail("A negative value is rejected");
        } catch (ConfigurationException expected) {
            assertEquals(5, WebSocketLimits.getMaxConnections());
        }
    }
}
