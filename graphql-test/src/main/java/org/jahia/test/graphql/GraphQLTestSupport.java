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
package org.jahia.test.graphql;

import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.OsgiGraphQLHttpServlet;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContext;
import graphql.kickstart.servlet.context.GraphQLServletContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.jahia.api.Constants;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.securityfilter.PermissionService;
import org.jahia.test.JahiaTestCase;
import org.jahia.test.graphql.context.CustomGraphQLServletContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GraphQLTestSupport extends JahiaTestCase {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLTestSupport.class);

    private static OsgiGraphQLHttpServlet servlet;

    protected static void init() {
        https://github.com/Jahia/jahia-private/pull/998
        servlet = (OsgiGraphQLHttpServlet) BundleUtils.getOsgiService(Servlet.class, "(component.name=graphql.kickstart.servlet.OsgiGraphQLHttpServlet)");
    }

    protected static void removeTestNodes() throws RepositoryException {
        removeTestNodes(Constants.EDIT_WORKSPACE);
        removeTestNodes(Constants.LIVE_WORKSPACE);
    }

    private static void removeTestNodes(String workspace) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, workspace, null, session -> {
            if (session.itemExists("/testList")) {
                session.getNode("/testList").remove();
                session.save();
            }
            if (session.itemExists("/testList1")) {
                session.getNode("/testList1").remove();
                session.save();
            }
            return null;
        });
    }

    protected static JSONObject executeQuery(String query) throws JSONException {
        return executeQuery(query, new MockHttpServletRequest());
    }

    protected static JSONObject executeQueryWithFiles(String query, List<Part> files) throws JSONException {
        try {
            servlet.setContextProvider(getCustomContextProvider(files));
            return executeQuery(query);
        } finally {
            servlet.unsetContextProvider(null);
        }
    }

    /*
     * Creating a multipart request with a file attachment WIP
     * https://stackoverflow.com/a/30541653
     */
    private MockMultipartHttpServletRequest buildMultipartRequest(String fileName) {
        byte[] data = "test text".getBytes();
        final String boundary = "myBoundary";
        MockMultipartFile file = new MockMultipartFile("test-binary", fileName, MediaType.TEXT_PLAIN_VALUE, data);
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.addFile(file);
        req.setContentType("multipart/form-data; boundary=" + boundary);
        req.setContent(createFileContent(data, boundary, MediaType.TEXT_PLAIN_VALUE, fileName));

        return req;
    }

    private static byte[] createFileContent(byte[] data, String boundary, String contentType, String fileName) {
        String start = "--" + boundary + "\r\n Content-Disposition: form-data; name=\"file\"; filename=\""+fileName+"\"\r\n"
                + "Content-type: "+contentType+"\r\n\r\n";;
        String end = "\r\n--" + boundary + "--";
        return ArrayUtils.addAll(start.getBytes(), ArrayUtils.addAll(data, end.getBytes()));
    }

    protected static JSONObject executeQuery(String query, MockHttpServletRequest req) throws JSONException {
        req.setMethod("POST");
        req.setServerPort(8080);
        req.setRequestURI("http://localhost:8080/modules/graphql");
        req.addHeader("Origin", "http://localhost:8080");

        MockHttpServletResponse res = new MockHttpServletResponse();

        PermissionService service = BundleUtils.getOsgiService(PermissionService.class, null);
        if (service != null) {
            service.initScopes(req);
        }

        req.setContentType("application/json");
        StringWriter writer = new StringWriter();
        new JSONObject(Collections.singletonMap("query", query)).write(writer);
        req.setContent(writer.getBuffer().toString().getBytes(StandardCharsets.UTF_8));
        String result = null;
        try {
            servlet.service(req, res);
            result = res.getContentAsString();
        } catch (ServletException | IOException e) {
            throw new RuntimeException(e);
        }

        if (service != null) {
            try {
                service.getClass().getMethod("resetScopes").invoke(service);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // Ignore
            }
        }

        if (result != null) {
            if (result.contains("Validation error")) {
                logger.error("Validation error {} for query: {}", result, query);
            } else if (result.contains("Invalid Syntax")) {
                logger.error("Invalid Syntax\" {} for query: {}", result, query);
            }
        }
        return new JSONObject(result);
    }

    private static GraphQLServletContextBuilder getCustomContextProvider(List<Part> files) {
        return new GraphQLServletContextBuilder() {
            @Override public GraphQLContext build(HttpServletRequest request, HttpServletResponse response) {
                GraphQLServletContext context = DefaultGraphQLServletContext
                        .createServletContext().with(request).with(response).build();
                return new CustomGraphQLServletContext(context, files);
            }
            @Override public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) { return null; }
            @Override public GraphQLContext build() { return null; }
        };
    }

    protected static Map<String, JSONObject> toItemByKeyMap(String key, JSONArray items) throws JSONException {
        HashMap<String, JSONObject> itemByName = new HashMap<>(items.length());
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            itemByName.put(item.getString(key), item);
        }
        return itemByName;
    }

    protected static void validateNode(JSONObject node, String expectedName) throws JSONException {
        Assert.assertEquals(expectedName, node.getString("name"));
    }

    protected static void validateNode(JSONObject node, String expectedUuid, String expectedName, String expectedPath, String expectedParentNodePath) throws JSONException {
        validateNode(node, expectedName);
        Assert.assertEquals(expectedUuid, node.getString("uuid"));
        Assert.assertEquals(expectedPath, node.getString("path"));
        Assert.assertEquals(expectedParentNodePath, node.getJSONObject("parent").getString("path"));
    }

    protected static void validateError(JSONObject result, String errorMessage) throws JSONException {
        validateErrors(result, new String[]{errorMessage});
    }

    protected static void validateErrors(JSONObject result, String[] errorMessages) throws JSONException {
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errorMessages.length, errors.length());
        for (int i = 0; i < errorMessages.length; i++) {
            Assert.assertTrue(errors.getJSONObject(i).getString("message").contains(errorMessages[i]));
        }
    }
}
