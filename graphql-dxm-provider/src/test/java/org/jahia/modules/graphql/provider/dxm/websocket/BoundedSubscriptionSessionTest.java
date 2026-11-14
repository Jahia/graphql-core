package org.jahia.modules.graphql.provider.dxm.websocket;

import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscription;

import javax.websocket.Session;
import java.util.HashMap;

import static org.jahia.modules.graphql.provider.dxm.websocket.BoundedSubscriptionSession.Refusal.ID_IN_USE;
import static org.jahia.modules.graphql.provider.dxm.websocket.BoundedSubscriptionSession.Refusal.TOO_MANY_OPERATIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BoundedSubscriptionSessionTest {

    private BoundedSubscriptionSession session;

    @Before
    public void setUp() {
        session = newSession();
    }

    static BoundedSubscriptionSession newSession() {
        Session webSocketSession = mock(Session.class);
        when(webSocketSession.getUserProperties()).thenReturn(new HashMap<>());
        when(webSocketSession.getId()).thenReturn("1");
        when(webSocketSession.isOpen()).thenReturn(true);
        return new BoundedSubscriptionSession(new GraphQLSubscriptionMapper(GraphQLObjectMapper.newBuilder().build()), webSocketSession);
    }

    @Test
    public void reservesAnIdOnce() {
        assertNull(session.reserve("1", 0));
        assertEquals(ID_IN_USE, session.reserve("1", 0));
    }

    @Test
    public void reservesUpToTheBound() {
        assertNull(session.reserve("1", 2));
        assertNull(session.reserve("2", 2));
        assertEquals(TOO_MANY_OPERATIONS, session.reserve("3", 2));
        assertNull(session.reserve("3", 0));
    }

    @Test
    public void runsOneSubscriptionForAnId() {
        Subscription first = mock(Subscription.class);
        Subscription second = mock(Subscription.class);
        session.reserve("1", 0);

        session.add("1", first);
        session.add("1", second);

        verify(first, never()).cancel();
        verify(second).cancel();
        session.close("done");
        verify(first).cancel();
    }

    @Test
    public void cancelsASubscriptionForAnIdNotReserved() {
        Subscription subscription = mock(Subscription.class);

        session.add("1", subscription);

        verify(subscription).cancel();
        assertEquals(0, session.getSubscriptions().getSubscriptionCount());
    }

    @Test
    public void cancelsASubscriptionWhoseOperationStoppedBeforeItArrived() {
        Subscription subscription = mock(Subscription.class);
        session.reserve("1", 0);
        session.unsubscribe("1");

        session.add("1", subscription);

        verify(subscription).cancel();
    }

    @Test
    public void stopCancelsTheSubscriptionAndFreesTheId() {
        Subscription subscription = mock(Subscription.class);
        session.reserve("1", 0);
        session.add("1", subscription);

        session.unsubscribe("1");

        verify(subscription).cancel();
        assertNull(session.reserve("1", 0));
    }

    @Test
    public void anErrorAnswerFreesTheIdOfAnOperationWithNoSubscription() {
        session.reserve("1", 0);

        session.sendMessage(new OperationMessage(OperationMessage.Type.GQL_ERROR, "1", null));

        assertEquals(0, session.getReservedCount());
    }

    @Test
    public void aRefusalKeepsTheRunningOperation() {
        Subscription subscription = mock(Subscription.class);
        session.reserve("1", 0);
        session.add("1", subscription);

        session.refuse("1", ID_IN_USE);

        assertEquals(ID_IN_USE, session.reserve("1", 0));
        verify(subscription, never()).cancel();
    }

    @Test
    public void cancelsASubscriptionThatArrivesAfterTheClose() {
        Subscription subscription = mock(Subscription.class);
        session.reserve("1", 0);
        session.close("done");

        session.add("1", subscription);

        verify(subscription).cancel();
    }
}
