/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
@ServerEndpoint(value="/graphqlws", subprotocols = {"graphql-ws", "graphql-transport-ws"}, configurator = OsgiGraphQLWsEndpoint.Configurator.class)
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
