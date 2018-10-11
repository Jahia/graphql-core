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
 * Short description of the class
 *
 * @author yousria
 */
public class GraphQLOrderingTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            node.addNode("Hello", "jnt:bigText");
            node.addNode("Bonjour", "jnt:press");
            node.addNode("Hola", "jnt:linkList");

            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldOrderNodeTypes() throws Exception{
        JSONObject result = executeQuery("{"
                + "  jcr(workspace: EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", ordering: {orderType: ASC, property: "
                + "\"jcr:primaryType\"}}) {"
                + "      nodes {"
                + "        primaryNodeType {"
                + "          name"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}");
        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes.length() == 3);
        Assert.assertTrue(nodes.getJSONObject(0).getJSONObject("primaryNodeType").getString("name").equals("jnt:bigText"));
        Assert.assertTrue(nodes.getJSONObject(1).getJSONObject("primaryNodeType").getString("name").equals("jnt:linkList"));
        Assert.assertTrue(nodes.getJSONObject(2).getJSONObject("primaryNodeType").getString("name").equals("jnt:press"));

        JSONObject result2 = executeQuery("{"
                + "  jcr(workspace: EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", ordering: {orderType: DESC, property: "
                + "\"jcr:primaryType\"}}) {"
                + "      nodes {"
                + "        primaryNodeType {"
                + "          name"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}");
        JSONArray nodes2 = result2.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes.length() == 3);
        Assert.assertTrue(nodes2.getJSONObject(0).getJSONObject("primaryNodeType").getString("name").equals("jnt:press"));
        Assert.assertTrue(nodes2.getJSONObject(1).getJSONObject("primaryNodeType").getString("name").equals("jnt:linkList"));
        Assert.assertTrue(nodes2.getJSONObject(2).getJSONObject("primaryNodeType").getString("name").equals("jnt:bigText"));


    }
}
