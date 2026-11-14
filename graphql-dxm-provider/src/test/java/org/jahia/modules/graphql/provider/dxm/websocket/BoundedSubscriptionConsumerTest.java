package org.jahia.modules.graphql.provider.dxm.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.websocket.CloseReason;
import java.util.Hashtable;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BoundedSubscriptionConsumerTest {

    private static final String DEFAULT_CONFIG_PID = "org.jahia.modules.graphql.provider~default";
    private static final String START = "{\"type\":\"start\",\"id\":\"%s\",\"payload\":{\"query\":\"subscription { a }\"}}";

    private BoundedSubscriptionSession session;
    private Consumer<String> delegate;
    private BoundedSubscriptionConsumer consumer;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        session = BoundedSubscriptionSessionTest.newSession();
        delegate = mock(Consumer.class);
        consumer = new BoundedSubscriptionConsumer(session, new ObjectMapper(), delegate);
    }

    @After
    public void restoreLimits() {
        new DXGraphQLConfig().deleted(DEFAULT_CONFIG_PID);
    }

    @Test
    public void closesTheConnectionOnAMessageOutsideTheProtocol() throws Exception {
        for (String text : asList("not json", "null", "[]", "{}", "{\"type\":\"unknown\"}", "{\"type\":\"ka\"}",
                "{\"type\":\"data\",\"id\":\"1\"}", "{\"type\":\"start\",\"payload\":{\"query\":\"q\"}}",
                "{\"type\":\"start\",\"id\":\"1\",\"payload\":\"q\"}", "{\"type\":\"start\",\"id\":\"1\"}", "{\"type\":\"stop\"}")) {
            BoundedSubscriptionSession refused = BoundedSubscriptionSessionTest.newSession();
            new BoundedSubscriptionConsumer(refused, new ObjectMapper(), delegate).accept(text);

            ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
            verify(refused.unwrap()).close(reason.capture());
            assertEquals(text, ConnectionClosing.INVALID_MESSAGE, reason.getValue());
        }
        verify(delegate, never()).accept(anyString());
    }

    @Test
    public void handsOnTheMessagesOfTheProtocol() throws Exception {
        for (String text : asList("{\"type\":\"connection_init\",\"payload\":{}}", String.format(START, "1"),
                "{\"type\":\"stop\",\"id\":\"1\"}", "{\"type\":\"connection_terminate\"}")) {
            consumer.accept(text);
            verify(delegate).accept(text);
        }
        verify(session.unwrap(), never()).close(any(CloseReason.class));
    }

    @Test
    public void recordsTheConnectionInit() {
        consumer.accept("{\"type\":\"connection_init\"}");

        assertEquals(Boolean.TRUE, session.getUserProperties().get(BoundedSubscriptionConsumer.INITIALIZED));
    }

    @Test
    public void answersAStartWithARunningIdWithoutRunningIt() throws Exception {
        String start = String.format(START, "1");

        consumer.accept(start);
        consumer.accept(start);

        verify(delegate, times(1)).accept(start);
        assertEquals(1, session.getReservedCount());
        verify(session.unwrap(), never()).close(any(CloseReason.class));
    }

    @Test
    public void answersAStartBeyondTheBoundWithoutRunningIt() throws Exception {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("graphql.websocket.maxSubscriptionsPerConnection", "2");
        new DXGraphQLConfig().updated(DEFAULT_CONFIG_PID, properties);

        for (String id : asList("1", "2", "3")) {
            consumer.accept(String.format(START, id));
        }

        verify(delegate, never()).accept(String.format(START, "3"));
        assertEquals(2, session.getReservedCount());
    }

    @Test
    public void closesTheConnectionAndFreesTheIdWhenAMessageCannotBeProcessed() throws Exception {
        String start = String.format(START, "1");
        doThrow(new IllegalArgumentException()).when(delegate).accept(start);

        consumer.accept(start);

        verify(session.unwrap()).close(ConnectionClosing.PROCESSING_ERROR);
        assertEquals(0, session.getReservedCount());
    }
}
