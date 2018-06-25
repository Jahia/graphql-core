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

import java.util.Locale;
import java.util.Map;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author yousria
 */
public class GraphQLCriteriaTest extends GraphQLTestSupport {

    private static String nodeUuid;
    private static String subNodeUuid1;
    private static String subNodeUuid2;
    private static String subNodeUuid3;
    private static String subNodeUuid4;
    private static String subNodeUuid41;
    private static String subNodeUuid42;
    private static String subNodeUuid43;

    private static String subnodeTitleEn1 = "text EN - subList1";
    private static String subnodeTitleFr1 = "text FR - subList1";
    private static String subnodeTitleEn2 = "text EN - subList2";
    private static String subnodeTitleFr2 = "text FR - subList2";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            nodeUuid = node.getIdentifier();

            JCRNodeWrapper subNode1 = node.addNode("testSubList1", "jnt:contentList");
            subNode1.addMixin("jmix:liveProperties");
            subNode1.setProperty("jcr:title", subnodeTitleEn1);
            subNode1.setProperty("j:liveProperties", new String[] {"liveProperty1", "liveProperty2"});
            subNodeUuid1 = subNode1.getIdentifier();

            JCRNodeWrapper subNode2 = node.addNode("testSubList2", "jnt:contentList");
            subNode2.setProperty("jcr:title", subnodeTitleEn2);
            subNodeUuid2 = subNode2.getIdentifier();

            JCRNodeWrapper subNode3 = node.addNode("testSubList3", "jnt:contentList");
            subNodeUuid3 = subNode3.getIdentifier();

            JCRNodeWrapper subNode4 = node.addNode("testSubList4", "jnt:contentList");
            subNodeUuid4 = subNode4.getIdentifier();
            subNodeUuid41 = subNode4.addNode("testSubList4_1", "jnt:contentList").getIdentifier();
            subNodeUuid42 = subNode4.addNode("testSubList4_2", "jnt:contentList").getIdentifier();
            subNodeUuid43 = subNode4.addNode("testSubList4_3", "jnt:contentList").getIdentifier();

            session.save();
            return null;
        });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH, session -> {
            JCRNodeWrapper node = session.getNode("/testList");
            node.getNode("testSubList1").setProperty("jcr:title", subnodeTitleFr1);
            node.getNode("testSubList2").setProperty("jcr:title", subnodeTitleFr2);
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrieveDescendantNodesByAncestorPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria:{paths: [\"/testList\"], nodeType: \"jnt:content\"}) {"
                + "            nodes {"
                + "                uuid"
                + "                name"
                + "                path"
                + "                parent {"
                + "                    path"
                + "                }"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(7, childByName.size());
        validateNode(childByName.get("testSubList1"), subNodeUuid1, "testSubList1", "/testList/testSubList1", "/testList");
        validateNode(childByName.get("testSubList2"), subNodeUuid2, "testSubList2", "/testList/testSubList2", "/testList");
        validateNode(childByName.get("testSubList3"), subNodeUuid3, "testSubList3", "/testList/testSubList3", "/testList");
        validateNode(childByName.get("testSubList4"), subNodeUuid4, "testSubList4", "/testList/testSubList4", "/testList");
        validateNode(childByName.get("testSubList4_1"), subNodeUuid41, "testSubList4_1", "/testList/testSubList4/testSubList4_1", "/testList/testSubList4");
        validateNode(childByName.get("testSubList4_2"), subNodeUuid42, "testSubList4_2", "/testList/testSubList4/testSubList4_2", "/testList/testSubList4");
        validateNode(childByName.get("testSubList4_3"), subNodeUuid43, "testSubList4_3", "/testList/testSubList4/testSubList4_3", "/testList/testSubList4");
    }

    @Test
    public void shouldRetrieveChildNodesByParentPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria:{paths: [\"/testList\"], pathType: PARENT, nodeType: \"jnt:content\"}) {"
                + "            nodes {"
                + "                uuid"
                + "                name"
                + "                path"
                + "                parent {"
                + "                    path"
                + "                }"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray descendants = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", descendants);

        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), subNodeUuid1, "testSubList1", "/testList/testSubList1", "/testList");
        validateNode(childByName.get("testSubList2"), subNodeUuid2, "testSubList2", "/testList/testSubList2", "/testList");
        validateNode(childByName.get("testSubList3"), subNodeUuid3, "testSubList3", "/testList/testSubList3", "/testList");
        validateNode(childByName.get("testSubList4"), subNodeUuid4, "testSubList4", "/testList/testSubList4", "/testList");
    }

    @Test
    public void shouldRetrieveNodesByPaths() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria:{paths: [\"/testList\", \"/testList/testSubList2\", \"/testList/testSubList4/testSubList4_2\"], pathType: OWN, nodeType: \"jnt:content\"}) {"
                + "            nodes {"
                + "                uuid"
                + "                name"
                + "                path"
                + "                parent {"
                + "                    path"
                + "                }"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(3, nodeByName.size());
        validateNode(nodeByName.get("testList"), nodeUuid, "testList", "/testList", "/");
        validateNode(nodeByName.get("testSubList2"), subNodeUuid2, "testSubList2", "/testList/testSubList2", "/testList");
        validateNode(nodeByName.get("testSubList4_2"), subNodeUuid42, "testSubList4_2", "/testList/testSubList4/testSubList4_2", "/testList/testSubList4");
    }

    @Test
    public void shouldNotRetrieveNodesByLanguage() throws JSONException {
        testQueryByCriteria("{nodeType:\"jnt:contentList\", paths:[\"/testList/\"], pathType:ANCESTOR, language:\"da\"}", 0);
        testQueryByCriteria("{nodeType:\"jnt:contentList\", paths:[\"/testList/\"], pathType:ANCESTOR, language:\"it\"}", 0);
    }

    @Test
    public void shouldRetrieveNodesByLanguage() throws JSONException {
        testQueryByCriteria("{nodeType:\"jnt:contentList\", paths:[\"/testList/\"], pathType:ANCESTOR, language:\"fr\"}", 2);
        testQueryByCriteria("{nodeType:\"jnt:contentList\", paths:[\"/testList/\"], pathType:ANCESTOR, language:\"en\"}", 2);
    }

    private static JSONObject runCriteriaQuery(String criteria) throws JSONException {
        return executeQuery(""
                + "{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: " + criteria + ") {"
                + "            nodes {"
                + "                name "
                + "            }"
                + "	       }"
                + "    }"
                + "}");
    }

    private static void testQueryByCriteria(String criteria, long expectedNodesNumber) throws JSONException {
        JSONObject result = runCriteriaQuery(criteria.toString());
        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertEquals(expectedNodesNumber, nodes.length());
    }
}
