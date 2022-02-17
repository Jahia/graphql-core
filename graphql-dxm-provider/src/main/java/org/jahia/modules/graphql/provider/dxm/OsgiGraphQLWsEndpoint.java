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
import graphql.kickstart.execution.subscriptions.apollo.ApolloSubscriptionProtocolFactory;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage;
import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import graphql.kickstart.servlet.subscriptions.WebSocketSubscriptionProtocolFactory;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.util.BeanWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.usermanager.JahiaUser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component(immediate = true, service = Endpoint.class)
@ServerEndpoint(value="/graphqlws", subprotocols = {"graphql-ws"}, configurator = OsgiGraphQLWsEndpoint.Configurator.class)
public class OsgiGraphQLWsEndpoint extends Endpoint {
    private Logger logger = LoggerFactory.getLogger(OsgiGraphQLWsEndpoint.class);
    private GraphQLWebsocketServlet delegate;
    private WebSocketSubscriptionProtocolFactory subscriptionProtocolFactory;

    @Reference(service = HttpServlet.class, target = "(component.name=graphql.kickstart.servlet.OsgiGraphQLHttpServlet)")
    public void setServlet(HttpServlet servlet) {
        try {
            GraphQLQueryInvoker queryInvoker = (GraphQLQueryInvoker) getter(servlet, "getQueryInvoker");
            GraphQLInvocationInputFactory invocationInputFactory = (GraphQLInvocationInputFactory) getter(servlet, "getInvocationInputFactory");
            GraphQLObjectMapper graphQLObjectMapper = (GraphQLObjectMapper) getter(servlet, "getGraphQLObjectMapper");

            delegate = new GraphQLWebsocketServlet(queryInvoker.toGraphQLInvoker(), invocationInputFactory, graphQLObjectMapper);
            subscriptionProtocolFactory = BeanWrapper.wrap(delegate)
                    .call("getSubscriptionProtocolFactory", new Class[] {List.class}, new Object[] {Collections.singletonList("graphql-ws")})
                    .unwrap(WebSocketSubscriptionProtocolFactory.class);
        } catch (Exception e) {
            logger.error("Cannot get schema from GQL servlet", e);
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            BeanWrapper.wrap(subscriptionProtocolFactory)
                    .get("commandProvider", ApolloSubscriptionProtocolFactory.class)
                    .call("getByType", new Class[] {OperationMessage.Type.class}, new Object[] {OperationMessage.Type.GQL_START})
                    .get("connectionListeners")
                    .call("iterator")
                    .call("next")
                    .get("keepAliveRunner")
                    .get("executor")
                    .call("shutdown");
        } catch (ReflectiveOperationException e) {
            logger.error("Cannot get schema from GQL servlet", e);
        }

        delegate.beginShutDown();
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
        userProperties.put(SubscriptionProtocolFactory.class.getName(), subscriptionProtocolFactory);
        JCRSessionFactory.getInstance().setCurrentUser((JahiaUser) userProperties.get(Constants.SESSION_USER));

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
            sec.getUserProperties().put(Constants.SESSION_USER, JCRSessionFactory.getInstance().getCurrentUser());
        }
    }

}
