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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GraphQLAncestorsTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            JCRNodeWrapper subNode = node.addNode("testSubList", "jnt:contentList");
            subNode.addNode("testSubSubList", "jnt:contentList");
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrieveParent() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
                + "        parent {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONObject parent = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("parent");

        validateNode(parent, "testSubList");
    }

    @Test
    public void shouldRetrieveAllAncestors() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
                + "        ancestors {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONArray ancestors = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("ancestors");

        Assert.assertEquals(3, ancestors.length());
        validateNode(ancestors.getJSONObject(0), "");
        validateNode(ancestors.getJSONObject(1), "testList");
        validateNode(ancestors.getJSONObject(2), "testSubList");
    }

    @Test
    public void shouldRetrieveAncestorsUpToPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
                + "        ancestors(upToPath: \"/testList\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONArray ancestors = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("ancestors");

        Assert.assertEquals(2, ancestors.length());
        validateNode(ancestors.getJSONObject(0), "testList");
        validateNode(ancestors.getJSONObject(1), "testSubList");
    }

    @Test
    public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsEmpty() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
                + "        ancestors(upToPath: \"\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");

        validateError(result, "'' is not a valid node path");
    }

    @Test
    public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsNotAncestorPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
                + "        ancestors(upToPath: \"/nonExistingPath\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");

        validateError(result, "'/nonExistingPath' does not reference an ancestor node of '/testList/testSubList/testSubSubList'");
    }

    @Test
    public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsThisNodePath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
                + "        ancestors(upToPath: \"/testList/testSubList/testSubSubList\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");

        validateError(result, "'/testList/testSubList/testSubSubList' does not reference an ancestor node of '/testList/testSubList/testSubSubList'");
    }
}
