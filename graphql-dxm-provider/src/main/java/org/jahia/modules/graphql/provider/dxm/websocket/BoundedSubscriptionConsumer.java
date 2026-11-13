package org.jahia.modules.graphql.provider.dxm.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage.Type;
import org.jahia.modules.graphql.provider.dxm.config.WebSocketLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reads each message of a {@code graphql-ws} connection before the protocol handler does. It hands on the messages a
 * client sends in that protocol, records the {@code connection_init} message, reserves the id of each {@code start}
 * message on the {@link BoundedSubscriptionSession}, and closes the connection on any other message.
 */
public class BoundedSubscriptionConsumer implements Consumer<String> {

    /**
     * Key, in the user properties of a WebSocket session, set once the client has sent {@code connection_init}.
     */
    public static final String INITIALIZED = "org.jahia.modules.graphql.websocketInitialized";

    private static final Logger logger = LoggerFactory.getLogger(BoundedSubscriptionConsumer.class);
    private static final Set<Type> CLIENT_TYPES =
            EnumSet.of(Type.GQL_CONNECTION_INIT, Type.GQL_START, Type.GQL_STOP, Type.GQL_CONNECTION_TERMINATE);

    private final BoundedSubscriptionSession session;
    private final ObjectMapper objectMapper;
    private final Consumer<String> delegate;

    public BoundedSubscriptionConsumer(BoundedSubscriptionSession session, ObjectMapper objectMapper, Consumer<String> delegate) {
        this.session = session;
        this.objectMapper = objectMapper;
        this.delegate = delegate;
    }

    @Override
    public void accept(String text) {
        OperationMessage message = read(text);
        if (message == null) {
            close(ConnectionClosing.INVALID_MESSAGE);
            return;
        }
        String startedId = null;
        if (message.getType() == Type.GQL_CONNECTION_INIT) {
            session.getUserProperties().put(INITIALIZED, Boolean.TRUE);
        } else if (message.getType() == Type.GQL_START) {
            BoundedSubscriptionSession.Refusal refusal =
                    session.reserve(message.getId(), WebSocketLimits.getMaxSubscriptionsPerConnection());
            if (refusal != null) {
                session.refuse(message.getId(), refusal);
                return;
            }
            startedId = message.getId();
        }
        try {
            delegate.accept(text);
        } catch (RuntimeException e) {
            if (startedId != null) {
                session.release(startedId);
            }
            logger.debug("Cannot process a message of GraphQL subscription connection {}", session.getId(), e);
            close(ConnectionClosing.PROCESSING_ERROR);
        }
    }

    /**
     * @return the message, or {@code null} when it is not one a client sends in the protocol
     */
    private OperationMessage read(String text) {
        OperationMessage message;
        try {
            message = objectMapper.readValue(text, OperationMessage.class);
        } catch (JsonProcessingException e) {
            return null;
        }
        if (message == null || !CLIENT_TYPES.contains(message.getType())) {
            return null;
        }
        boolean needsId = message.getType() == Type.GQL_START || message.getType() == Type.GQL_STOP;
        if (needsId && message.getId() == null) {
            return null;
        }
        if (message.getType() == Type.GQL_START && !(message.getPayload() instanceof Map)) {
            return null;
        }
        return message;
    }

    private void close(CloseReason reason) {
        ConnectionClosing.close(session.unwrap(), reason);
    }
}
