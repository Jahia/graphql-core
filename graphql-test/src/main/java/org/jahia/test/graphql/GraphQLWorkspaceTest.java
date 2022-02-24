/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public void shouldRetrieveNodeFromDefault() throws Exception {

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

        JSONObject jcr = result.getJSONObject("data").getJSONObject("jcr");
        JSONObject subList2 = jcr.getJSONObject("testSubList2");
        JSONObject subList3 = jcr.getJSONObject("testSubList3");

        validateNode(subList2, "testSubList2");
        validateNode(subList3, "testSubList3");
    }

    @Test
    public void shouldRetrieveNodeFromLive() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr(workspace:LIVE) {"
                + "      testSubList1:nodeByPath(path: \"/testList/testSubList1\") {"
                + "        name"
                + "      }"
                + "    }"
                + "}");

        JSONObject subList1 = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList1");
        validateNode(subList1, "testSubList1");
    }

    @Test
    public void shouldRetrieveLiveCounterpartOfNode() throws Exception {

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

        JSONObject subList3Default = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList3");
        JSONObject subList1Live = subList3Default.getJSONObject("nodeInWorkspace");

        validateNode(subList3Default, "testSubList3");
        validateNode(subList1Live, "testSubList1");
    }

    @Test
    public void shouldNotRetrieveLiveCounterpartOfNode() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "      testSubList2:nodeByPath(path: \"/testList/testSubList2\") {"
                + "        name"
                + "        nodeInWorkspace(workspace:LIVE) {"
                + "          name"
                + "        }"
                + "      }"
                + "    }"
                + "}");

        JSONObject subList2Default = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList2");
        Object subList1Live = subList2Default.get("nodeInWorkspace");

        validateNode(subList2Default, "testSubList2");
        Assert.assertEquals(JSONObject.NULL, subList1Live);
    }

    @Test
    public void shouldRetrieveEDITWorkspaceFields() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "      workspace"
                + "      testSubList3:nodeByPath(path: \"/testList/testSubList3\") {"
                + "        name"
                + "        workspace"
                + "        nodeInWorkspace(workspace:LIVE) {"
                + "          name"
                + "          workspace"
                + "        }"
                + "      }"
                + "    }"
                + "}");

        JSONObject subList3Default = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList3");
        JSONObject subList1Live = subList3Default.getJSONObject("nodeInWorkspace");

        Assert.assertEquals(result.getJSONObject("data").getJSONObject("jcr").getString("workspace"), "EDIT");
        Assert.assertEquals(subList3Default.getString("workspace"), "EDIT");
        Assert.assertEquals(subList1Live.getString("workspace"), "LIVE");
    }

    @Test
    public void shouldRetrieveLIVEWorkspaceFields() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr(workspace:LIVE) {"
                + "      workspace"
                + "      testSubList1:nodeByPath(path: \"/testList/testSubList1\") {"
                + "        name"
                + "        workspace"
                + "        nodeInWorkspace(workspace:EDIT) {"
                + "          name"
                + "          workspace"
                + "        }"
                + "      }"
                + "    }"
                + "}");


        JSONObject subList1Live = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("testSubList1");
        JSONObject subList3Default = subList1Live.getJSONObject("nodeInWorkspace");

        Assert.assertEquals(result.getJSONObject("data").getJSONObject("jcr").getString("workspace"), "LIVE");
        Assert.assertEquals(subList3Default.getString("workspace"), "EDIT");
        Assert.assertEquals(subList1Live.getString("workspace"), "LIVE");
    }
}
