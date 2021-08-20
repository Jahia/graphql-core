/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm;

import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.subscriptions.SubscriptionProtocolFactory;
import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import graphql.kickstart.servlet.apollo.ApolloWebSocketSubscriptionProtocolFactory;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import graphql.kickstart.servlet.subscriptions.WebSocketSubscriptionProtocolFactory;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.usermanager.JahiaUser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@Component(immediate = true, service = Endpoint.class)
@ServerEndpoint(value="/graphql", subprotocols = {"graphql-ws"}, configurator = OsgiGraphQLWsEndpoint.Configurator.class)
public class OsgiGraphQLWsEndpoint extends Endpoint {
    private GraphQLWebsocketServlet delegate;
    private WebSocketSubscriptionProtocolFactory subscriptionProtocolFactory;

    @Reference(service = HttpServlet.class, target = "(component.name=graphql.kickstart.servlet.OsgiGraphQLHttpServlet)")
    public void setServlet(HttpServlet servlet) {
        try {
            GraphQLQueryInvoker queryInvoker = (GraphQLQueryInvoker) getter(servlet, "getQueryInvoker");
            GraphQLInvocationInputFactory invocationInputFactory = (GraphQLInvocationInputFactory) getter(servlet, "getInvocationInputFactory");
            GraphQLObjectMapper graphQLObjectMapper = (GraphQLObjectMapper) getter(servlet, "getGraphQLObjectMapper");

            delegate = new GraphQLWebsocketServlet(queryInvoker.toGraphQLInvoker(), invocationInputFactory, graphQLObjectMapper);
            subscriptionProtocolFactory = new ApolloWebSocketSubscriptionProtocolFactory(graphQLObjectMapper, invocationInputFactory, queryInvoker.toGraphQLInvoker());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("java:S3011")
    private Object getter(Object object, String getter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = object.getClass().getDeclaredMethod(getter);
        method.setAccessible(true);
        return method.invoke(object);
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        Map<String, Object> userProperties = endpointConfig.getUserProperties();
        // todo : is it the correct way to inject the SubscriptionProtocolFactory .. ?
        userProperties.put(SubscriptionProtocolFactory.class.getName(), subscriptionProtocolFactory);
        HttpSession httpSession = (HttpSession) userProperties.get(HttpSession.class.getName());
        JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) httpSession.getAttribute("org.jahia.usermanager.jahiauser"));
        try {
            delegate.onOpen(session, endpointConfig);
        } finally {
            JCRSessionFactory.getInstance().setCurrentUser(null);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        delegate.onClose(session, closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        delegate.onError(session, thr);
    }

    public static class Configurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            HttpSession httpSession = (HttpSession)request.getHttpSession();
            sec.getUserProperties().put(HttpSession.class.getName(),httpSession);
        }
    }
}
