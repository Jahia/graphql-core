/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
                + "    jcr {"
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
                                       + "    }"
                                       + "}");
        JSONObject node = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath");

        Assert.assertEquals("/testList", node.getString("path"));
        Assert.assertEquals("testList", node.getString("name"));
        Assert.assertEquals(nodeUuid, node.getString("uuid"));
        Assert.assertEquals("testList", node.getString("displayName"));
        Assert.assertEquals(nodeTitleFr, node.getJSONObject("titlefr").getString("value"));
        Assert.assertEquals(nodeTitleEn, node.getJSONObject("titleen").getString("value"));
    }

    @Test
    public void shouldRetrieveNodeByPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                                       + "    nodeByPath(path: \"/testList/testSubList2\") {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");
        JSONObject node = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath");

        validateNode(node, "testSubList2");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodeByWrongPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                                       + "    nodeByPath(path: \"/testList/wrongPath\") {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");

        validateError(result, "javax.jcr.PathNotFoundException: /testList/wrongPath");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodeByPathInLive() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr (workspace: LIVE) {"
                                       + "    nodeByPath(path: \"/testList/testSubList2\") {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");

        validateError(result, "javax.jcr.PathNotFoundException: /testList/testSubList2");
    }

    @Test
    public void shouldRetrieveNodesByPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                                       + "    nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/testSubList1\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");
        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONArray("nodesByPath");
        Map<String, JSONObject> nodesByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodesByName.size());
        validateNode(nodesByName.get("testSubList1"), "testSubList1");
        validateNode(nodesByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByWrongPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                                       + "    nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/wrongPath\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");

        validateError(result, "javax.jcr.PathNotFoundException: /testList/wrongPath");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByPathInLive() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr (workspace: LIVE) {"
                                       + "    nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/testSubList1\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");

        validateError(result, "javax.jcr.PathNotFoundException: /testList/testSubList2");
    }

    @Test
    public void shouldRetrieveNodeById() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                                       + "    nodeById(uuid: \"" + subNodeUuid2 + "\") {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");
        JSONObject node = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeById");

        validateNode(node, "testSubList2");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodeByWrongId() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                                       + "    nodeById(uuid: \"badId\") {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");

        validateError(result, "javax.jcr.ItemNotFoundException: badId");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodeByIdInLive() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr (workspace: LIVE) {"
                                        + "    nodeById(uuid: \"" + subNodeUuid2 + "\") {"
                                        + "        name"
                                        + "    }"
                                        + "    }"
                                        + "}");

        validateError(result, "javax.jcr.ItemNotFoundException: " + subNodeUuid2);
    }

    @Test
    public void shouldRetrieveNodesById() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                                       + "    nodesById(uuids: [\"" + subNodeUuid2 + "\", \"" + subNodeUuid1 + "\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");
        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONArray("nodesById");
        Map<String, JSONObject> nodesByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodesByName.size());
        validateNode(nodesByName.get("testSubList2"), "testSubList2");
        validateNode(nodesByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByWrongId() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                                       + "    nodesById(uuids: [\"" + subNodeUuid2 + "\", \"wrongId\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");

        validateError(result, "javax.jcr.ItemNotFoundException: wrongId");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByIdInLive() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr (workspace: LIVE) {"
                                       + "    nodesById(uuids: [\"" + subNodeUuid2 + "\", \"" + subNodeUuid1 + "\"]) {"
                                       + "        name"
                                       + "    }"
                                       + "    }"
                                       + "}");

        validateError(result, "javax.jcr.ItemNotFoundException: " + subNodeUuid2);
    }
}
