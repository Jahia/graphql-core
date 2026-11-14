package org.jahia.modules.graphql.provider.dxm.config;

/**
 * Holds the effective bounds of the GraphQL subscription WebSocket endpoint. {@link DXGraphQLConfig} pushes them in
 * whenever the configuration is recomputed, and a value of {@code 0} disables a bound.
 */
public final class WebSocketLimits {

    static final String INIT_TIMEOUT = "graphql.websocket.initTimeout";
    static final String MAX_CONNECTIONS = "graphql.websocket.maxConnections";
    static final String MAX_CONNECTIONS_PER_USER = "graphql.websocket.maxConnectionsPerUser";
    static final String MAX_SUBSCRIPTIONS_PER_CONNECTION = "graphql.websocket.maxSubscriptionsPerConnection";

    static final int DEFAULT_INIT_TIMEOUT_SECONDS = 10;
    static final int DEFAULT_MAX_CONNECTIONS = 1000;
    static final int DEFAULT_MAX_CONNECTIONS_PER_USER = 20;
    static final int DEFAULT_MAX_SUBSCRIPTIONS_PER_CONNECTION = 50;

    private static volatile int initTimeoutSeconds = DEFAULT_INIT_TIMEOUT_SECONDS;
    private static volatile int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private static volatile int maxConnectionsPerUser = DEFAULT_MAX_CONNECTIONS_PER_USER;
    private static volatile int maxSubscriptionsPerConnection = DEFAULT_MAX_SUBSCRIPTIONS_PER_CONNECTION;

    private WebSocketLimits() {
        throw new IllegalStateException("Utility class is not meant to be instantiated");
    }

    /**
     * @return the number of seconds a connection has to send its {@code connection_init} message
     */
    public static int getInitTimeoutSeconds() {
        return initTimeoutSeconds;
    }

    /**
     * @return the number of connections the endpoint holds open at once
     */
    public static int getMaxConnections() {
        return maxConnections;
    }

    /**
     * @return the number of connections one authenticated user holds open at once
     */
    public static int getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    /**
     * @return the number of subscriptions one connection runs at once
     */
    public static int getMaxSubscriptionsPerConnection() {
        return maxSubscriptionsPerConnection;
    }

    static void update(int initTimeout, int connections, int connectionsPerUser, int subscriptionsPerConnection) {
        initTimeoutSeconds = initTimeout;
        maxConnections = connections;
        maxConnectionsPerUser = connectionsPerUser;
        maxSubscriptionsPerConnection = subscriptionsPerConnection;
    }
}
