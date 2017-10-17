/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 * http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 * Copyright (C) 2002-2017 Jahia Solutions Group. All rights reserved.
 *
 * This file is part of a Jahia's Enterprise Distribution.
 *
 * Jahia's Enterprise Distributions must be used in accordance with the terms
 * contained in the Jahia Solutions Group Terms & Conditions as well as
 * the Jahia Sustainable Enterprise License (JSEL).
 *
 * For questions regarding licensing, support, production usage...
 * please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test.graphql;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class GraphQLNodePathTest extends GraphQLAbstractTest {

    @Test
    public void shouldRetrieveNodeByPath() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeByPath(path: \"/testList/testSubList2\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONObject nodeByPath = result.getJSONObject("data").getJSONObject("nodeByPath");

        Assert.assertEquals("/testList/testSubList2", nodeByPath.getString("path"));
        Assert.assertEquals("testSubList2", nodeByPath.getString("name"));
        Assert.assertEquals(subNodeUuid2, nodeByPath.getString("uuid"));
    }

    @Test
    public void shouldNotRetrieveNodeByPathWhenPathIsWrong() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeByPath(path: \"/testList/wrongPath\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/wrongPath");
    }

    @Test
    public void shouldNotRetrieveNodeByPathInLive() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeByPath(path: \"/testList/testSubList2\", workspace: \"live\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/testSubList2");
    }

    @Test
    public void shouldRetrieveNodesByPath() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/testSubList1\"]) {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray nodes = result.getJSONObject("data").getJSONArray("nodesByPath");
        Map<String, JSONObject> nodesByName = toItemByKeyMap("name", nodes);

        validateNode(nodesByName.get("testSubList2"), subNodeUuid2, "testSubList2", "/testList/testSubList2", "/testList");
        validateNode(nodesByName.get("testSubList1"), subNodeUuid1, "testSubList1", "/testList/testSubList1", "/testList");
    }

    @Test
    public void shouldNotRetrieveNodesByPathWhenAPathIsWrong() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/wrongPath\"]) {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/wrongPath");
    }

    @Test
    public void shouldNotRetrieveNodesByPathInLive() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/testSubList1\"], workspace: \"live\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/testSubList2");
    }

    @Test
    public void shouldRetrieveNodeById() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeById(uuid: \"" + subNodeUuid2 + "\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONObject nodeByPath = result.getJSONObject("data").getJSONObject("nodeById");

        Assert.assertEquals("/testList/testSubList2", nodeByPath.getString("path"));
        Assert.assertEquals("testSubList2", nodeByPath.getString("name"));
        Assert.assertEquals(subNodeUuid2, nodeByPath.getString("uuid"));
    }

    @Test
    public void shouldNotRetrieveNodeByIdWhenWhenIdIsWrong() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeById(uuid: \"badId\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: badId");
    }

    @Test
    public void shouldNotRetrieveNodeByIdInLive() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeById(uuid: \"" + subNodeUuid2 + "\", workspace: \"live\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: " + subNodeUuid2);
    }

    @Test
    public void shouldRetrieveNodesById() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesById(uuids: [\"" + subNodeUuid2 + "\", \"" + subNodeUuid1 + "\"]) {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray nodes = result.getJSONObject("data").getJSONArray("nodesById");
        Map<String, JSONObject> nodesByName = toItemByKeyMap("name", nodes);

        validateNode(nodesByName.get("testSubList2"), subNodeUuid2, "testSubList2", "/testList/testSubList2", "/testList");
        validateNode(nodesByName.get("testSubList1"), subNodeUuid1, "testSubList1", "/testList/testSubList1", "/testList");
    }

    @Test
    public void shouldNotRetrieveNodesByIdWhenAnIdIsWrong() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesById(uuids: [\"" + subNodeUuid2 + "\", \"wrongId\"]) {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: wrongId");
    }

    @Test
    public void shouldNotRetrieveNodesByIdInLive() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesById(uuids: [\"" + subNodeUuid2 + "\", \"" + subNodeUuid1 + "\"], workspace: \"live\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: " + subNodeUuid2);
    }

}
