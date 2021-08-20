package org.jahia.modules.graphql.provider.dxm.osgi;

import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import graphql.kickstart.servlet.OsgiGraphQLHttpServlet;
import graphql.kickstart.servlet.apollo.ApolloWebSocketSubscriptionProtocolFactory;
import graphql.kickstart.servlet.subscriptions.WebSocketSubscriptionProtocolFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(
        service = {javax.servlet.http.HttpServlet.class, javax.servlet.Servlet.class, OSGIServlet.class},
        property = {
                "service.description=GraphQL HTTP Servlet",
                "alias=/graphql",
                "osgi.http.whiteboard.servlet.asyncSupported=true",
                "osgi.http.whiteboard.servlet.multipart.enabled=true",
                "jmx.objectname=graphql.servlet:type=graphql"
        }
)
public class OSGIServlet extends OsgiGraphQLHttpServlet {

    @Activate
    public void activate() {
        // todo : check activate from superclass
    }

    @Deactivate
    @Override
    public void deactivate() {
        // todo : check deactivate from superclass
    }

    public GraphQLWebsocketServlet createWs() {
        // todo : check constructor parameters
        return new GraphQLWebsocketServlet(getQueryInvoker().toGraphQLInvoker(), getInvocationInputFactory(), getGraphQLObjectMapper());
    }

    public WebSocketSubscriptionProtocolFactory createSubscriptionProtocolFactory() {
        // todo : check constructor parameters
        return new ApolloWebSocketSubscriptionProtocolFactory(getGraphQLObjectMapper(), getInvocationInputFactory(), getQueryInvoker().toGraphQLInvoker());
    }

}
