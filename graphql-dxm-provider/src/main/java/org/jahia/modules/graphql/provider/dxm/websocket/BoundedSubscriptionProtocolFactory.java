package org.jahia.modules.graphql.provider.dxm.websocket;

import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import graphql.kickstart.servlet.apollo.ApolloWebSocketSubscriptionProtocolFactory;

import javax.websocket.Session;
import java.util.function.Consumer;

/**
 * The {@code graphql-ws} protocol, with each connection bounded by a {@link BoundedSubscriptionSession} and a
 * {@link BoundedSubscriptionConsumer}.
 */
public class BoundedSubscriptionProtocolFactory extends ApolloWebSocketSubscriptionProtocolFactory {

    public BoundedSubscriptionProtocolFactory(GraphQLObjectMapper objectMapper,
                                              GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
                                              GraphQLInvoker graphQLInvoker) {
        super(objectMapper, invocationInputFactory, graphQLInvoker);
    }

    @Override
    public SubscriptionSession createSession(Session session) {
        return new BoundedSubscriptionSession(new GraphQLSubscriptionMapper(getObjectMapper()), session);
    }

    @Override
    public Consumer<String> createConsumer(SubscriptionSession session) {
        return new BoundedSubscriptionConsumer((BoundedSubscriptionSession) session,
                getObjectMapper().getJacksonMapper(), super.createConsumer(session));
    }
}
