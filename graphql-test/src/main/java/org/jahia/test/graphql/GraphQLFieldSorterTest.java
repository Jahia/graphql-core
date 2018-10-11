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
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

/**
 * Test to verify Field Sorter functionnality
 *
 * @author yousria
 */
public class GraphQLFieldSorterTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            node.addNode("ZHello", "jnt:contentList");
            node.addNode("abonjour", "jnt:contentList");

            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }


    @Test
    public void shouldSortDisplayNames() throws Exception {
        JSONObject result = executeQuery("{"
                + "  jcr (workspace:EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", language:\"en\"}, fieldSorter: "
                + "{fieldName: \"displayName\", sortType: DESC, ignoreCase:false}) {"
                + "      nodes {"
                + "        displayName(language:\"en\")"
                + "      }"
                + "    }"
                + "  }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes.length() == 2);
        Assert.assertTrue(nodes.getJSONObject(0).getString("displayName").equals("abonjour"));
        Assert.assertTrue(nodes.getJSONObject(1).getString("displayName").equals("ZHello"));

        JSONObject result2 = executeQuery("{"
                + "  jcr (workspace:EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", language:\"en\"}, fieldSorter: "
                + "{fieldName: \"displayName\", sortType: ASC, ignoreCase:false}) {"
                + "      nodes {"
                + "        displayName(language:\"en\")"
                + "      }"
                + "    }"
                + "  }"
                + "}");

        JSONArray nodes2 = result2.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes2.length() == 2);
        Assert.assertTrue(nodes2.getJSONObject(0).getString("displayName").equals("ZHello"));
        Assert.assertTrue(nodes2.getJSONObject(1).getString("displayName").equals("abonjour"));

        JSONObject result3 = executeQuery("{"
                + "  jcr (workspace:EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", language:\"en\"}, fieldSorter: "
                + "{fieldName: \"displayName\", sortType: ASC, ignoreCase:true}) {"
                + "      nodes {"
                + "        displayName(language:\"en\")"
                + "      }"
                + "    }"
                + "  }"
                + "}");

        JSONArray nodes3 = result3.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes3.length() == 2);
        Assert.assertTrue(nodes3.getJSONObject(0).getString("displayName").equals("abonjour"));
        Assert.assertTrue(nodes3.getJSONObject(1).getString("displayName").equals("ZHello"));

        JSONObject result4 = executeQuery("{"
                + "  jcr (workspace:EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", language:\"en\"}, fieldSorter: "
                + "{fieldName: \"displayName\", sortType: DESC, ignoreCase:true}) {"
                + "      nodes {"
                + "        displayName(language:\"en\")"
                + "      }"
                + "    }"
                + "  }"
                + "}");

        JSONArray nodes4 = result4.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes4.length() == 2);
        Assert.assertTrue(nodes4.getJSONObject(0).getString("displayName").equals("ZHello"));
        Assert.assertTrue(nodes4.getJSONObject(1).getString("displayName").equals("abonjour"));



    }
}
