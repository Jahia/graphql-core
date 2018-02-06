/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.test.graphql;

import org.jahia.api.Constants;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GraphQLWorkspaceTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();
        Map<String, String> ids = new HashMap<>();
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            ids.put("testSubList1", node.addNode("testSubList1", "jnt:contentList").getIdentifier());
            ids.put("testSubList2", node.addNode("testSubList2", "jnt:contentList").getIdentifier());

            session.save();
            return null;
        });
        ServicesRegistry.getInstance().getJCRPublicationService().publishByMainId(ids.get("testSubList1"));

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            session.getNode("/testList/testSubList1").rename("testSubList3");
            session.save();
            return null;
        });

    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void testGetNodeDefault() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "      testSubList2:nodeByPath(path: \"/testList/testSubList2\") {"
                + "        name"
                + "      }"
                + "      testSubList3:nodeByPath(path: \"/testList/testSubList3\") {"
                + "        name"
                + "      }"
                + "    }"
                + "}");
        JSONObject node1 = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList2");
        JSONObject node3 = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList3");
        validateNode(node1, "testSubList2");
        validateNode(node3, "testSubList3");
    }

    @Test
    public void testGetNodeLive() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr(workspace:LIVE) {"
                + "      testSubList1:nodeByPath(path: \"/testList/testSubList1\") {"
                + "        name"
                + "      }"
                + "    }"
                + "}");
        JSONObject node1 = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList1");
        validateNode(node1, "testSubList1");
    }

    @Test
    public void testGetNodeInLive() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "      testSubList3:nodeByPath(path: \"/testList/testSubList3\") {"
                + "        name"
                + "        nodeInWorkspace(workspace:LIVE) {"
                + "          name"
                + "        }"
                + "      }"
                + "    }"
                + "}");
        JSONObject node3 = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList3");
        JSONObject node2 = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList3").getJSONObject("nodeInWorkspace");
        validateNode(node3, "testSubList3");
        validateNode(node2, "testSubList1");
    }


}
