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

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;

public class GraphQLNodeRetrievalTest extends GraphQLTestSupport {

    private static String nodeUuid;
    private static String nodeTitleFr = "text FR";
    private static String nodeTitleEn = "text EN";
    private static String subNodeUuid1;
    private static String subNodeUuid2;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            node.setProperty("jcr:title", nodeTitleEn);
            nodeUuid = node.getIdentifier();

            JCRNodeWrapper subNode1 = node.addNode("testSubList1", "jnt:contentList");
            subNodeUuid1 = subNode1.getIdentifier();

            JCRNodeWrapper subNode2 = node.addNode("testSubList2", "jnt:contentList");
            subNodeUuid2 = subNode2.getIdentifier();

            session.save();
            return null;
        });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH, session -> {
            JCRNodeWrapper node = session.getNode("/testList");
            node.setProperty("jcr:title", nodeTitleFr);
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void testGetNode() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        name"
                                       + "        path"
                                       + "        uuid"
                                       + "        displayName"
                                       + "        titleen:property(name: \"jcr:title\" language:\"en\") {"
                                       + "            value"
                                       + "        }"
                                       + "        titlefr:property(name: \"jcr:title\" language:\"fr\") {"
                                       + "           value"
                                       + "        }"
                                       + "    }"
                                       + "}");
        JSONObject nodeByPath = result.getJSONObject("data").getJSONObject("nodeByPath");

        Assert.assertEquals("/testList", nodeByPath.getString("path"));
        Assert.assertEquals("testList", nodeByPath.getString("name"));
        Assert.assertEquals(nodeUuid, nodeByPath.getString("uuid"));
        Assert.assertEquals("testList", nodeByPath.getString("displayName"));
        Assert.assertEquals(nodeTitleFr, nodeByPath.getJSONObject("titlefr").getString("value"));
        Assert.assertEquals(nodeTitleEn, nodeByPath.getJSONObject("titleen").getString("value"));
    }

    @Test
    public void shouldRetrieveNodeByPath() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList/testSubList2\") {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONObject node = result.getJSONObject("data").getJSONObject("nodeByPath");

        validateNode(node, "testSubList2");
    }

    @Test
    public void shouldNotRetrieveNodeByWrongPath() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList/wrongPath\") {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/wrongPath");
    }

    @Test
    public void shouldNotRetrieveNodeByPathInLive() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList/testSubList2\", workspace: \"live\") {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/testSubList2");
    }

    @Test
    public void shouldRetrieveNodesByPath() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/testSubList1\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray nodes = result.getJSONObject("data").getJSONArray("nodesByPath");
        Map<String, JSONObject> nodesByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodesByName.size());
        validateNode(nodesByName.get("testSubList1"), "testSubList1");
        validateNode(nodesByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldNotRetrieveNodesByWrongPath() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/wrongPath\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/wrongPath");
    }

    @Test
    public void shouldNotRetrieveNodesByPathInLive() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/testSubList1\"], workspace: \"live\") {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/testSubList2");
    }

    @Test
    public void shouldRetrieveNodeById() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeById(uuid: \"" + subNodeUuid2 + "\") {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONObject node = result.getJSONObject("data").getJSONObject("nodeById");

        validateNode(node, "testSubList2");
    }

    @Test
    public void shouldNotRetrieveNodeByWrongId() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeById(uuid: \"badId\") {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: badId");
    }

    @Test
    public void shouldNotRetrieveNodeByIdInLive() throws Exception {

        JSONObject result = executeQuery("{"
                                        + "    nodeById(uuid: \"" + subNodeUuid2 + "\", workspace: \"live\") {"
                                        + "        name"
                                        + "    }"
                                        + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: " + subNodeUuid2);
    }

    @Test
    public void shouldRetrieveNodesById() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodesById(uuids: [\"" + subNodeUuid2 + "\", \"" + subNodeUuid1 + "\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray nodes = result.getJSONObject("data").getJSONArray("nodesById");
        Map<String, JSONObject> nodesByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodesByName.size());
        validateNode(nodesByName.get("testSubList2"), "testSubList2");
        validateNode(nodesByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldNotRetrieveNodesByWrongId() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodesById(uuids: [\"" + subNodeUuid2 + "\", \"wrongId\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: wrongId");
    }

    @Test
    public void shouldNotRetrieveNodesByIdInLive() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodesById(uuids: [\"" + subNodeUuid2 + "\", \"" + subNodeUuid1 + "\"], workspace: \"live\") {"
                                       + "        name"
                                       + "    }"
                                       + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: " + subNodeUuid2);
    }
}
