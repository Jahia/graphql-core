package org.jahia.modules.graphql.provider.dxm.websocket;

import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage;
import graphql.kickstart.servlet.apollo.ApolloWebSocketSubscriptionSession;
import org.reactivestreams.Subscription;

import javax.websocket.Session;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A subscription session that runs at most one subscription for each operation id, and a bounded number of
 * operations at once.
 * <p>
 * An id is reserved when its {@code start} message arrives, before the operation executes, and freed when the
 * operation stops, completes, fails, or answers an error instead of a subscription. A subscription that arrives for an
 * id that is not reserved, or that already runs one, is cancelled at once, so that every running subscription is the
 * one {@link #close} cancels.
 */
public class BoundedSubscriptionSession extends ApolloWebSocketSubscriptionSession {

    /**
     * Why {@link #reserve} refused an id.
     */
    public enum Refusal {
        ID_IN_USE("An operation with this id is already running"),
        TOO_MANY_OPERATIONS("Too many operations on this connection");

        private final String message;

        Refusal(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final Object lock = new Object();
    private final Set<String> reserved = new HashSet<>();
    private final Map<String, Subscription> running = new HashMap<>();

    public BoundedSubscriptionSession(GraphQLSubscriptionMapper mapper, Session session) {
        super(mapper, session);
    }

    /**
     * Reserves an operation id.
     *
     * @param id            the id of the operation
     * @param maxOperations the number of ids one connection may reserve at once, {@code 0} for no bound
     * @return {@code null} when the id is reserved, or why it is not
     */
    public Refusal reserve(String id, int maxOperations) {
        synchronized (lock) {
            if (reserved.contains(id)) {
                return Refusal.ID_IN_USE;
            }
            if (maxOperations > 0 && reserved.size() >= maxOperations) {
                return Refusal.TOO_MANY_OPERATIONS;
            }
            reserved.add(id);
            return null;
        }
    }

    /**
     * Frees an id whose operation runs no subscription.
     */
    public void release(String id) {
        synchronized (lock) {
            if (!running.containsKey(id)) {
                reserved.remove(id);
            }
        }
    }

    /**
     * Answers a {@code start} message that {@link #reserve} refused with an error for its id.
     */
    public void refuse(String id, Refusal refusal) {
        super.sendMessage(new OperationMessage(OperationMessage.Type.GQL_ERROR, id,
                Collections.singletonList(Collections.singletonMap("message", refusal.getMessage()))));
    }

    @Override
    public void add(String id, Subscription subscription) {
        synchronized (lock) {
            if (reserved.contains(id) && !running.containsKey(id)) {
                try {
                    super.add(id, subscription);
                    running.put(id, subscription);
                    return;
                } catch (IllegalStateException e) {
                    // The connection is closed: the subscription is cancelled below.
                }
            }
        }
        subscription.cancel();
    }

    @Override
    public void unsubscribe(String id) {
        synchronized (lock) {
            reserved.remove(id);
            running.remove(id);
        }
        super.unsubscribe(id);
    }

    @Override
    public void sendMessage(Object payload) {
        if (payload instanceof OperationMessage) {
            OperationMessage message = (OperationMessage) payload;
            if (message.getType() == OperationMessage.Type.GQL_ERROR && message.getId() != null) {
                release(message.getId());
            }
        }
        super.sendMessage(payload);
    }

    /**
     * @return the number of operation ids reserved on this connection
     */
    public int getReservedCount() {
        synchronized (lock) {
            return reserved.size();
        }
    }
}
