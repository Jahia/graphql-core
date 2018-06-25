/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test.graphql;

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

import java.util.Locale;
import java.util.Map;

/**
 * tests to query nodes by criteria
 *
 * @author yousria
 */
public class GraphQLCriteriaTest extends GraphQLTestSupport {

    private static String subNodeUuid1;
    private static String subNodeUuid2;
    private static String subNodeUuid3;
    private static String subNodeUuid4;
    private static String subnodeTitleEn1 = "text EN - subList1";
    private static String subnodeTitleFr1 = "text FR - subList1";
    private static String subnodeTitleEn2 = "text EN - subList2";
    private static String subnodeTitleFr2 = "text FR - subList2";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");

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
            subNode4.addNode("testSubList4_1", "jnt:contentList").addMixin("jmix:tagged");
            subNode4.addNode("testSubList4_2", "jnt:contentList");
            subNode4.addNode("testSubList4_3", "jnt:contentList");

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
    public void shouldRetrieveAllChildNodesByCriteria() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "        nodesByCriteria(criteria:{paths: [\"/testList\"], pathType:PARENT, nodeType:\"jnt:content\"}) {"
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

        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), subNodeUuid1, "testSubList1", "/testList/testSubList1", "/testList");
        validateNode(childByName.get("testSubList2"), subNodeUuid2, "testSubList2", "/testList/testSubList2", "/testList");
        validateNode(childByName.get("testSubList3"), subNodeUuid3, "testSubList3", "/testList/testSubList3", "/testList");
        validateNode(childByName.get("testSubList4"), subNodeUuid4, "testSubList4", "/testList/testSubList4", "/testList");
    }

    @Test
    public void shouldRetrieveNodesByPathCriteria() throws JSONException {
        testQueryByCriteria("{nodeType: \"jnt:contentList\", paths: [\"/testList/\"], pathType: OWN}", 1);
        testQueryByCriteria("{nodeType: \"jnt:contentList\", paths: [\"/testList/\"], pathType: PARENT}", 4);
        testQueryByCriteria("{nodeType: \"jnt:contentList\", paths: [\"/testList/\"], pathType: ANCESTOR}", 7);
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
