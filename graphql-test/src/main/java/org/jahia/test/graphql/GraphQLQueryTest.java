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
import org.junit.Test;
import org.junit.Assert;

public class GraphQLQueryTest extends GraphQLTestSupport {

    @Test
    public void shouldRetrieveNodesUsingSQL2Query() throws Exception {
        testQuery("select * from [jnt:contentList] where isdescendantnode('/testList')", "SQL2", 7);
    }

    @Test
    public void shouldRetrieveNodesUsingXPATHQuery() throws Exception {
        testQuery("/jcr:root/testList//element(*, jnt:contentList)", "XPATH", 13);
    }

    protected void testQuery(String query, String language, long expectedNumber) throws Exception {
        JSONObject result = executeQuery("{"
                + "    nodesByQuery(query: \"" + query + "\", queryLanguage: " + language + ") {"
                + "        edges {"
                + "            node {"
                + "                name"
                + "                path"
                + "            }"
                + "		  }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("nodesByQuery").getJSONArray("edges");
        Assert.assertEquals(expectedNumber, nodes.length());
    }
}
