package org.jahia.modules.graphql.provider.dxm.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Closes a subscription connection with a reason, and reports the closes of each reason in the log at most once per
 * interval, with the number of closes since the previous report.
 */
public final class ConnectionClosing {

    public static final CloseReason ORIGIN_NOT_ALLOWED =
            new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Origin not allowed");
    public static final CloseReason UNSUPPORTED_SUBPROTOCOL =
            new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, "Unsupported subprotocol");
    public static final CloseReason NOT_GRANTED =
            new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Subscriptions not allowed");
    public static final CloseReason TOO_MANY_CONNECTIONS =
            new CloseReason(CloseReason.CloseCodes.TRY_AGAIN_LATER, "Too many connections");
    public static final CloseReason TOO_MANY_USER_CONNECTIONS =
            new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Too many connections for this user");
    public static final CloseReason INIT_TIMEOUT =
            new CloseReason(CloseReason.CloseCodes.getCloseCode(4408), "Connection initialisation timeout");
    public static final CloseReason INVALID_MESSAGE =
            new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid message");
    public static final CloseReason PROCESSING_ERROR =
            new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Cannot process message");

    /**
     * Tomcat session property: milliseconds Tomcat waits for the client to answer a close before it drops the
     * connection, 30 s by default. A Long.
     */
    static final String TOMCAT_SESSION_CLOSE_TIMEOUT = "org.apache.tomcat.websocket.SESSION_CLOSE_TIMEOUT";
    static final Long CLOSE_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private static final Logger logger = LoggerFactory.getLogger(ConnectionClosing.class);
    private static final long REPORT_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);
    private static final Map<String, ThrottledReport> reports = new ConcurrentHashMap<>();

    private ConnectionClosing() {
        throw new IllegalStateException("Utility class is not meant to be instantiated");
    }

    /**
     * Closes a connection, and reports it unless a close of the same reason was reported within the interval. The
     * close gets a short timeout, so the container drops the connection at its next periodic check when the client
     * never answers the close.
     */
    public static void close(Session session, CloseReason reason) {
        int closes = reports.computeIfAbsent(reason.getReasonPhrase(), phrase -> new ThrottledReport(REPORT_INTERVAL_NANOS)).count();
        if (closes > 0) {
            logger.warn("Closed GraphQL subscription connection {} with code {}: {} ({} such closes since the last report)",
                    session.getId(), reason.getCloseCode().getCode(), reason.getReasonPhrase(), closes);
        }
        try {
            session.getUserProperties().put(TOMCAT_SESSION_CLOSE_TIMEOUT, CLOSE_TIMEOUT_MILLIS);
            session.close(reason);
        } catch (IllegalStateException e) {
            logger.debug("GraphQL subscription connection {} is already closed", session.getId());
        } catch (IOException e) {
            logger.debug("Cannot close GraphQL subscription connection {}", session.getId(), e);
        }
    }
}
