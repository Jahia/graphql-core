/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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

    // @Test
    // public void shouldRetrieveParent() throws Exception {

    //     JSONObject result = executeQuery("{"
    //             + "    jcr {"
    //             + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
    //             + "        parent {"
    //             + "            name"
    //             + "		  }"
    //             + "    }"
    //             + "    }"
    //             + "}");
    //     JSONObject parent = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("parent");

    //     validateNode(parent, "testSubList");
    // }

    // @Test
    // public void shouldRetrieveAllAncestors() throws Exception {

    //     JSONObject result = executeQuery("{"
    //             + "    jcr {"
    //             + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
    //             + "        ancestors {"
    //             + "            name"
    //             + "		  }"
    //             + "    }"
    //             + "    }"
    //             + "}");
    //     JSONArray ancestors = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONArray("ancestors");

    //     Assert.assertEquals(3, ancestors.length());
    //     validateNode(ancestors.getJSONObject(0), "");
    //     validateNode(ancestors.getJSONObject(1), "testList");
    //     validateNode(ancestors.getJSONObject(2), "testSubList");
    // }

    // @Test
    // public void shouldRetrieveAncestorsUpToPath() throws Exception {

    //     JSONObject result = executeQuery("{"
    //             + "    jcr {"
    //             + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
    //             + "        ancestors(upToPath: \"/testList\") {"
    //             + "            name"
    //             + "		  }"
    //             + "    }"
    //             + "    }"
    //             + "}");
    //     JSONArray ancestors = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONArray("ancestors");

    //     Assert.assertEquals(2, ancestors.length());
    //     validateNode(ancestors.getJSONObject(0), "testList");
    //     validateNode(ancestors.getJSONObject(1), "testSubList");
    // }

    // @Test
    // public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsEmpty() throws Exception {

    //     JSONObject result = executeQuery("{"
    //             + "    jcr {"
    //             + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
    //             + "        ancestors(upToPath: \"\") {"
    //             + "            name"
    //             + "		  }"
    //             + "    }"
    //             + "    }"
    //             + "}");

    //     validateError(result, "'' is not a valid node path");
    // }

    // @Test
    // public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsNotAncestorPath() throws Exception {

    //     JSONObject result = executeQuery("{"
    //             + "    jcr {"
    //             + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
    //             + "        ancestors(upToPath: \"/nonExistingPath\") {"
    //             + "            name"
    //             + "		  }"
    //             + "    }"
    //             + "    }"
    //             + "}");

    //     validateError(result, "'/nonExistingPath' does not reference an ancestor node of '/testList/testSubList/testSubSubList'");
    // }

    // @Test
    // public void shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsThisNodePath() throws Exception {

    //     JSONObject result = executeQuery("{"
    //             + "    jcr {"
    //             + "    nodeByPath(path: \"/testList/testSubList/testSubSubList\") {"
    //             + "        ancestors(upToPath: \"/testList/testSubList/testSubSubList\") {"
    //             + "            name"
    //             + "		  }"
    //             + "    }"
    //             + "    }"
    //             + "}");

    //     validateError(result, "'/testList/testSubList/testSubSubList' does not reference an ancestor node of '/testList/testSubList/testSubSubList'");
    // }
}
