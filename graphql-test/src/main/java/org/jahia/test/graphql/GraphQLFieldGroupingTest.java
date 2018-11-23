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

/**
 * Test field grouping functionality
 *
 * @author akarmanov
 */
public class GraphQLFieldGroupingTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("groupingRoot", "jnt:contentFolder");
            node.addNode("nodeGroup1-0", "jnt:text");
            node.addNode("nodeGroup2-1", "jnt:contentList");
            node.addNode("noGroup1-0", "jnt:bigText");
            node.addNode("nodeGroup2-0", "jnt:contentList");
            node.addNode("noGroup1-1", "jnt:bigText");
            node.addNode("nodeGroup1-1", "jnt:text");

            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldGroupNodesWithoutSort() throws JSONException {
        JSONObject result = executeQuery("{" +
                "jcr {" +
                "    nodeByPath(path:\"/groupingRoot\") {" +
                "      result : descendants(fieldGrouping:{fieldName:\"type.value\", groups:[\"jnt:text\", \"jnt:fakeName\", \"jnt:contentList\"], groupingType:START}) {" +
                "        nodes {" +
                "          type:property(name:\"jcr:primaryType\") {" +
                "            value" +
                "          }" +
                "          name" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("result").getJSONArray("nodes");
        Assert.assertEquals(6, nodes.length());
        Assert.assertEquals("nodeGroup1-0", nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals("nodeGroup1-1", nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals("nodeGroup2-1", nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals("nodeGroup2-0", nodes.getJSONObject(3).getString("name"));
        Assert.assertEquals("noGroup1-0", nodes.getJSONObject(4).getString("name"));
        Assert.assertEquals("noGroup1-1", nodes.getJSONObject(5).getString("name"));

        result = executeQuery("{" +
                "jcr {" +
                "    nodeByPath(path:\"/groupingRoot\") {" +
                "      result : descendants(fieldGrouping:{fieldName:\"type.value\", groups:[\"jnt:fakeName\", \"jnt:text\", \"jnt:contentList\"], groupingType:END}) {" +
                "        nodes {" +
                "          type:property(name:\"jcr:primaryType\") {" +
                "            value" +
                "          }" +
                "          name" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("result").getJSONArray("nodes");
        Assert.assertEquals(6, nodes.length());
        Assert.assertEquals("noGroup1-0", nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals("noGroup1-1", nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals("nodeGroup1-0", nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals("nodeGroup1-1", nodes.getJSONObject(3).getString("name"));
        Assert.assertEquals("nodeGroup2-1", nodes.getJSONObject(4).getString("name"));
        Assert.assertEquals("nodeGroup2-0", nodes.getJSONObject(5).getString("name"));
    }

    @Test
    public void shouldGroupNodesWithSort() throws JSONException {
        JSONObject result = executeQuery("{" +
                "jcr {" +
                "    nodeByPath(path:\"/groupingRoot\") {" +
                "      result : descendants(fieldGrouping:{fieldName:\"type.value\", groups:[\"jnt:contentList\", \"jnt:text\", \"jnt:fakeName\"], groupingType:START}, fieldSorter:{sortType:ASC, fieldName: \"name\"}) {" +
                "        nodes {" +
                "          type:property(name:\"jcr:primaryType\") {" +
                "            value" +
                "          }" +
                "          name" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("result").getJSONArray("nodes");
        Assert.assertEquals(6, nodes.length());
        Assert.assertEquals("nodeGroup2-0", nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals("nodeGroup2-1", nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals("nodeGroup1-0", nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals("nodeGroup1-1", nodes.getJSONObject(3).getString("name"));
        Assert.assertEquals("noGroup1-0", nodes.getJSONObject(4).getString("name"));
        Assert.assertEquals("noGroup1-1", nodes.getJSONObject(5).getString("name"));
    }
}
