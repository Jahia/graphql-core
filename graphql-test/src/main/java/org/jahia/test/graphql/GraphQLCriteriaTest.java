/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author yousria
 */
public class GraphQLCriteriaTest extends GraphQLTestSupport {

    private static final String NONE_OR_MULTIPLE_NODE_COMPARISONS_ERROR = "Exactly one contraint field expected, either 'like', 'contains', 'equals', 'notEquals', 'gt', 'gte', 'lt', 'lte', 'exists' or 'lastDays'";

    private static String nodeUuid = null;
    private static String subNodeUuid1 = null;
    private static String subNodeUuid2 = null;
    private static String subNodeUuid3 = null;
    private static String subNodeUuid4 = null;
    private static String subNodeUuid41 = null;
    private static String subNodeUuid42 = null;
    private static String subNodeUuid43 = null;

    private static String subnodeTitleEn1 = "text EN - subList1";
    private static String subnodeTitleEn2 = "text EN - subList2";
    private static String subnodeTitleFr1 = "text FR - subList1";
    private static String subnodeTitleFr2 = "text FR - subList2";

    private static Date subnode1Published = new Date();
    private static Date subnode2Published = DateUtils.addDays(new Date(), -1);
    private static Date subnode3Published = DateUtils.addDays(new Date(), -2);
    private static Date subnode4Published = DateUtils.addDays(new Date(), -3);

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            try{
                JCRNodeWrapper node = session.getRootNode().addNode("testList", "jnt:contentList");
                nodeUuid = node.getIdentifier();

                JCRNodeWrapper subNode1 = node.addNode("testSubList1", "jnt:contentList");
                subNode1.addMixin("jmix:liveProperties");
                subNode1.addMixin("jmix:keywords");
                subNode1.setProperty("jcr:title", subnodeTitleEn1);
                subNode1.setProperty("j:liveProperties", new String[] {"liveProperty1", "liveProperty2"});
                subNode1.setProperty("j:keywords", new String[]{"keyword1", "keyword2"});
                Calendar calendar1 = Calendar.getInstance();
                calendar1.setTime(subnode1Published);
                subNode1.setProperty("j:lastPublished", calendar1);
                subNodeUuid1 = subNode1.getIdentifier();

                JCRNodeWrapper subNode2 = node.addNode("testSubList2", "jnt:contentList");
                subNode1.addMixin("jmix:keywords");
                subNode1.setProperty("j:keywords", new String[]{"keyword3", "keyword4"});
                subNode2.setProperty("jcr:title", subnodeTitleEn2);
                Calendar calendar2 = Calendar.getInstance();
                calendar2.setTime(subnode2Published);
                subNode2.setProperty("j:lastPublished", calendar2);
                subNodeUuid2 = subNode2.getIdentifier();

                JCRNodeWrapper subNode3 = node.addNode("testSubList3", "jnt:contentList");
                Calendar calendar3 = Calendar.getInstance();
                calendar3.setTime(subnode3Published);
                subNode3.setProperty("j:lastPublished", calendar3);
                subNodeUuid3 = subNode3.getIdentifier();

                JCRNodeWrapper subNode4 = node.addNode("testSubList4", "jnt:contentList");
                subNode4.addMixin("jmix:liveProperties");
                subNode4.setProperty("j:liveProperties", new String[] {"liveProperty3", "liveProperty4"});
                Calendar calendar4 = Calendar.getInstance();
                calendar4.setTime(subnode4Published);
                subNode4.setProperty("j:lastPublished", calendar4);
                subNodeUuid4 = subNode4.getIdentifier();
                subNodeUuid41 = subNode4.addNode("testSubList4_1", "jnt:contentList").getIdentifier();
                subNodeUuid42 = subNode4.addNode("testSubList4_2", "jnt:contentList").getIdentifier();
                subNodeUuid43 = subNode4.addNode("testSubList4_3", "jnt:contentList").getIdentifier();

                session.save();
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRANCE, session -> {
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
                + "        nodesByCriteria(criteria: {language: \"en\", paths: [\"/testList\"], nodeType: \"jnt:content\"}) {"
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
                + "        nodesByCriteria(criteria: {paths: [\"/testList\"], pathType: PARENT, nodeType: \"jnt:content\"}) {"
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
                + "        nodesByCriteria(criteria: {paths: [\"/testList\", \"/testList/testSubList2\", \"/testList/testSubList4/testSubList4_2\"], pathType: OWN, nodeType: \"jnt:content\"}) {"
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
    public void shouldRetrieveNodesByPropertyContainsExpression() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"jcr:title\", contains: \"SUBLIST1\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(1, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveNodesByNodeContainsExpression() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "            nodeConstraint: {contains: \"SUBLIST1\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(1, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveNodesByLikeExpression() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"jcr:title\", like: \"%subList%\"}}) {"
                + "            nodes {"
                + "                 name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
        validateNode(nodeByName.get("testSubList2"), "testSubList2");
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodeByDefaultConstraint() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"fr\", nodeConstraint: {property: \"jcr:title\", like: \"%subList2%\"}}) {"
                + "            nodes {"
                + "                 name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);
    }
    /**
     * test case for 'equals to' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodeByEqualsExpression() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:contentList\", language: \"en\", "
                + "              paths: \"/testList\", pathType: ANCESTOR, "
                + "             nodeConstraint: {property: \"jcr:title\", equals: \"" + subnodeTitleEn1 + "\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(1, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");

    }

    /**
     * test case for 'not equals to' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodeByNotEqualsExpression() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: ANCESTOR, "
                + "            nodeConstraint: {property: \"jcr:title\", notEquals: \"" + subnodeTitleEn1 + "\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(1, nodeByName.size());
        validateNode(nodeByName.get("testSubList2"), "testSubList2");

    }

    /**
     * test case for 'less than' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodeByLessThanExpression() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "           nodeConstraint: {property: \"j:lastPublished\", lt: \"" + datetimeToString(subnode2Published) + "\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodeByName.size());
        validateNode(nodeByName.get("testSubList3"), "testSubList3");
        validateNode(nodeByName.get("testSubList4"), "testSubList4");

    }

    /**
     * test case for 'less than or equals to' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodeByLessThanOrEqualsToExpression() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "             nodeConstraint: {property: \"j:lastPublished\", lte: \"" + datetimeToString(subnode2Published) + "\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertTrue(nodeByName.size()>=2);
        validateNode(nodeByName.get("testSubList3"), "testSubList3");
        validateNode(nodeByName.get("testSubList4"), "testSubList4");

    }

    /**
     * test case for 'greater than' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodeByGreaterThanExpression() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "              nodeConstraint: {property: \"j:lastPublished\", gt: \"" + datetimeToString(subnode2Published) + "\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertTrue(nodeByName.size()>=1);
        validateNode(nodeByName.get("testSubList1"), "testSubList1");

    }

    /**
     * test case for 'greater than or equals to' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodeByGreaterThanOrEqualsToExpression() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", gte: \""+datetimeToString(subnode2Published)+"\"}}) {"
                + "            nodes {"
                + "                name"
                + "                lastPublished: property(name: \"j:lastPublished\") {"
                + "                  value"
                + "                }"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
        validateNode(nodeByName.get("testSubList2"), "testSubList2");

    }

    /**
     * test case for 'exists' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodesByExistsExpression() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "                 nodeConstraint: {property: \"j:liveProperties\", exists: true}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
        validateNode(nodeByName.get("testSubList4"), "testSubList4");

    }

    /**
     * test case for 'last days' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodesByLastDaysExpression() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "                 nodeConstraint: {property: \"j:lastPublished\", lastDays: 2}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
        validateNode(nodeByName.get("testSubList2"), "testSubList2");
    }


    /**
     * test case for 'exists' constraints comparison
     *
     * @throws Exception
     */
    @Test
    public void shouldRetrieveNodesByExistsExpressionWhenPropertyDoesNotExist() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:liveProperties\", exists: false}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodeByName.size());
        validateNode(nodeByName.get("testSubList2"), "testSubList2");
        validateNode(nodeByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldRetrieveNodeByAllConstraints() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", exists: true}, "
                + "               all: [{property: \"j:liveProperties\", exists: true}, {property:\"j:keywords\", exists: true}]"
                + "            }) {"
                + "            nodes {"
                + "                name"
                + "                title: property(name: \"jcr:title\") {value}"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(1, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByAllConstraintsWhenPropertyIsEmpty() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", exists: true}, "
                + "               all: [{property: \"j:liveProperties\", exists: true}, {like: \"%subList1%\"}]"
                + "            }) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        validateError(result, "'property' field is required");
    }

    @Test
    public void shouldRetrieveNodeByAnyConstraints() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", exists: true}, "
                + "               any: [{property: \"j:liveProperties\", exists: true}, {property:\"j:keywords\", exists: true}]"
                + "            }) {"
                + "            nodes {"
                + "                name"
                + "                title: property(name: \"jcr:title\") {value}"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
        validateNode(nodeByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByAnyConstraintsWhenPropertyIsEmpty() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", exists: true}, "
                + "               any: [{property: \"j:liveProperties\", exists: true}, {like: \"%subList1%\"}]"
                + "            }) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        validateError(result, "'property' field is required");
    }

    @Test
    public void shouldRetrieveNodeByNoneConstraints() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", exists: true}, "
                + "               none: [{property: \"j:liveProperties\", exists: true}, {property:\"j:keywords\", exists: true}]"
                + "            }) {"
                + "            nodes {"
                + "                name"
                + "                title: property(name: \"jcr:title\") {value}"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(2, nodeByName.size());
        validateNode(nodeByName.get("testSubList2"), "testSubList2");
        validateNode(nodeByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByNoneConstraintsWhenPropertyIsEmpty() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", exists: true}, "
                + "               none: [{property: \"j:liveProperties\", exists: true}, {like: \"%subList2%\"}]"
                + "            }) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        validateError(result, "'property' field is required");
    }

    @Test
    public void shouldRetrieveNodeByAllAnyNoneConstraints() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", exists: true}, "
                + "               all: [{property: \"j:liveProperties\", exists: true}], "
                + "               any: [{property: \"j:keywords\", exists: true}, {property: \"j:tagList\", exists: true}], "
                + "               none: [{property: \"j:lastPublished\", lte: \""+datetimeToString(subnode3Published)+"\"}]"
                + "            }) {"
                + "            nodes {"
                + "                name"
                + "                title: property(name: \"jcr:title\") {value}"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(1, nodeByName.size());
        validateNode(nodeByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodeByAllAnyNoneConstraints() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "               nodeConstraint: {property: \"j:lastPublished\", exists: true}, "
                + "               all: [{property: \"j:liveProperties\", exists: true}, {like: \"%subList2%\"}], "
                + "               any: [{property: \"j:keywords\", exists: true}, {like: \"%subList2%\"}], "
                + "               none: [{property: \"j:lastPublished\", lte: \""+datetimeToString(subnode3Published)+"\"}, {like: \"%subList2%\"}]"
                + "            }) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        validateError(result, "'property' field is required");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByLikeExpressionWhenPropertyIsEmpty() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", paths: \"/testList\", nodeConstraint: {like: \"%subList1%\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        validateError(result, "'property' field is required");
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByNodeConstraintWhenNoComparisonSpecified() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", paths: \"/testList\", nodeConstraint: {property: \"property\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        validateError(result, NONE_OR_MULTIPLE_NODE_COMPARISONS_ERROR);
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByNodeConstraintWhenMultipleComparisonsSpecified() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", paths: \"/testList\", nodeConstraint: {property: \"property\", contains: \"contains\", like: \"like\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        validateError(result, NONE_OR_MULTIPLE_NODE_COMPARISONS_ERROR);
    }

    @Test
    public void shouldRetrieveNodesByInternationalizedPropertyValuePassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"en\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "             nodeConstraint: {property: \"jcr:title\", like: \"%subList2%\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Map<String, JSONObject> nodeByName = toItemByKeyMap("name", nodes);

        Assert.assertEquals(1, nodeByName.size());
        validateNode(nodeByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldNotRetrieveNodesByInternationalizedPropertyValuePassingDifferentLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria: {nodeType: \"jnt:content\", language: \"fr\", "
                + "              paths: \"/testList\", pathType: PARENT, "
                + "           nodeConstraint: {property: \"jcr:title\", contains: \"SUBLIST3\"}}) {"
                + "            nodes {"
                + "                name"
                + "		       }"
                + "        }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");

        Assert.assertEquals(0, nodes.length());
    }

    private static String datetimeToString(Date date){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");
        return dateFormat.format(date);
    }

}
