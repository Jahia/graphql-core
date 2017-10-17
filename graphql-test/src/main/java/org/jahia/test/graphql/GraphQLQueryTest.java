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

import java.util.Locale;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public class GraphQLQueryTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");

            node.addNode("testSubList1", "jnt:contentList");
            node.addNode("testSubList2", "jnt:contentList");
            node.addNode("testSubList3", "jnt:contentList");

            JCRNodeWrapper subNode4 = node.addNode("testSubList4", "jnt:contentList");
            subNode4.addNode("testSubList4_1", "jnt:contentList");
            subNode4.addNode("testSubList4_2", "jnt:contentList");
            subNode4.addNode("testSubList4_3", "jnt:contentList");

            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrieveNodesUsingSQL2Query() throws Exception {
        testQuery("select * from [jnt:contentList] where isdescendantnode('/testList')", "SQL2", 7);
    }

    @Test
    public void shouldRetrieveNodesUsingXPATHQuery() throws Exception {
        testQuery("/jcr:root/testList//element(*, jnt:contentList)", "XPATH", 7);
    }

    private void testQuery(String query, String language, long expectedNumber) throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodesByQuery(query: \"" + query + "\", queryLanguage: " + language + ") {"
                + "        edges {"
                + "            node {"
                + "                name"
                + "            }"
                + "		  }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("nodesByQuery").getJSONArray("edges");
        Assert.assertEquals(expectedNumber, nodes.length());
    }
}
