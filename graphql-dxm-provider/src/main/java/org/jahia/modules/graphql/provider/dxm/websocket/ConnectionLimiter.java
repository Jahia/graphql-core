package org.jahia.modules.graphql.provider.dxm.websocket;

import org.jahia.modules.graphql.provider.dxm.config.WebSocketLimits;

import javax.websocket.CloseReason;
import java.util.HashMap;
import java.util.Map;

/**
 * Counts the open subscription connections, in all and for each authenticated user, against
 * {@link WebSocketLimits#getMaxConnections()} and {@link WebSocketLimits#getMaxConnectionsPerUser()}.
 */
public class ConnectionLimiter {

    private int open;
    private final Map<String, Integer> openByUser = new HashMap<>();

    /**
     * Counts a connection when both bounds leave room for it.
     *
     * @param userKey the key of the user who opens the connection, {@code null} for a guest, whom only the total bounds
     * @return {@code null} when the connection is counted, or the reason to close it with
     */
    public synchronized CloseReason acquire(String userKey) {
        int maxConnections = WebSocketLimits.getMaxConnections();
        if (maxConnections > 0 && open >= maxConnections) {
            return ConnectionClosing.TOO_MANY_CONNECTIONS;
        }
        if (userKey != null) {
            int ofUser = openByUser.getOrDefault(userKey, 0);
            int maxPerUser = WebSocketLimits.getMaxConnectionsPerUser();
            if (maxPerUser > 0 && ofUser >= maxPerUser) {
                return ConnectionClosing.TOO_MANY_USER_CONNECTIONS;
            }
            openByUser.put(userKey, ofUser + 1);
        }
        open++;
        return null;
    }

    /**
     * Uncounts a connection that {@link #acquire} counted.
     */
    public synchronized void release(String userKey) {
        open = Math.max(0, open - 1);
        if (userKey != null) {
            openByUser.computeIfPresent(userKey, (key, count) -> count > 1 ? count - 1 : null);
        }
    }

    public synchronized int getOpen() {
        return open;
    }
}
