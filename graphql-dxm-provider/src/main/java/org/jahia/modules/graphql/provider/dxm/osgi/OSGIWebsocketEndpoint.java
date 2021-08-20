package org.jahia.modules.graphql.provider.dxm.osgi;

import graphql.kickstart.execution.subscriptions.SubscriptionProtocolFactory;
import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import graphql.kickstart.servlet.subscriptions.WebSocketSubscriptionProtocolFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@Component(immediate = true, service = Endpoint.class)
@ServerEndpoint(value="/graphqlws")
public class OSGIWebsocketEndpoint extends Endpoint {
    private GraphQLWebsocketServlet delegate;

    private OSGIServlet servlet;
    private WebSocketSubscriptionProtocolFactory subscriptionProtocolFactory;

    @Reference
    public void setServlet(OSGIServlet servlet) {
        this.servlet = servlet;
    }

    @Activate
    public void activate() {
        delegate = servlet.createWs();
        subscriptionProtocolFactory = servlet.createSubscriptionProtocolFactory();
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        // todo : is it the correct way to inject the SubscriptionProtocolFactory .. ?
        endpointConfig.getUserProperties().put(SubscriptionProtocolFactory.class.getName(), subscriptionProtocolFactory);
        delegate.onOpen(session, endpointConfig);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        delegate.onClose(session, closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        delegate.onError(session, thr);
    }
}
