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
import org.junit.Assert;
import org.junit.Test;

public class GraphQLAncestorsTest extends GraphQLTestSupport {

    @Test
    public void shouldRetrieveParent() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        parent {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONObject parent = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("parent");
        validateNode(parent, "testSubList4");
    }

    @Test
    public void shouldRetrieveAllAncestors() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        ancestors {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONArray ancestors = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("ancestors");

        Assert.assertEquals(3, ancestors.length());
        validateNode(ancestors.getJSONObject(0), "");
        validateNode(ancestors.getJSONObject(1), "testList");
        validateNode(ancestors.getJSONObject(2), "testSubList4");
    }

    @Test
    public void shouldRetrieveAncestorsUpToPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        ancestors(upToPath: \"/testList\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONArray ancestors = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("ancestors");

        Assert.assertEquals(2, ancestors.length());
        validateNode(ancestors.getJSONObject(0), "testList");
        validateNode(ancestors.getJSONObject(1), "testSubList4");
    }

    @Test
    public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsEmpty() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        ancestors(upToPath: \"\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");

        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(1, errors.length());
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "'' is not a valid node path");
    }

    @Test
    public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsNotAncestorPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        ancestors(upToPath: \"/nonExistingPath\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");

        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(1, errors.length());
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "'/nonExistingPath' does not reference an ancestor node of '/testList/testSubList4/testSubList4_1'");
    }

    @Test
    public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsThisNodePath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        ancestors(upToPath: \"/testList/testSubList4/testSubList4_1\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");

        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(1, errors.length());
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "'/testList/testSubList4/testSubList4_1' does not reference an ancestor node of '/testList/testSubList4/testSubList4_1'");
    }
}
