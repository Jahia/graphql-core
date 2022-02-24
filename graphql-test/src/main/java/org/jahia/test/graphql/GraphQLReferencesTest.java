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
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;

public class GraphQLReferencesTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            node.addMixin("jmix:liveProperties");
            node.setProperty("j:liveProperties", new String[] {"/testList/testSubList1", "/testList/testSubList2"});

            JCRNodeWrapper subNode1 = node.addNode("testSubList1", "jnt:contentList");
            subNode1.setProperty("jcr:title", "/testList/testSubList2");

            JCRNodeWrapper subNode2 = node.addNode("testSubList2", "jnt:contentList");
            subNode2.setProperty("jcr:title", "/nonExistingPath");
            subNode2.addMixin("jmix:unstructured");

            JCRNodeWrapper ref1 = node.addNode("reference1", "jnt:contentReference");
            ref1.setProperty("j:node", subNode1);
            JCRNodeWrapper ref2 = node.addNode("reference2", "jnt:contentReference");
            ref2.setProperty("j:node", subNode1);
            JCRNodeWrapper ref3 = subNode2.addNode("reference3", "nt:linkedFile");
            ref3.setProperty("jcr:content", subNode1);

            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrieveReferences() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList/testSubList1\") {"
                + "        references {"
                + "            nodes {"
                + "            node {"
                + "                name"
                + "            }"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");
        JSONArray references = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("references").getJSONArray("nodes");
        Map<String, JSONObject> referenceByNodeName = toItemByKeyMap("node", references);

        Assert.assertEquals(3, referenceByNodeName.size());
        validateNode(referenceByNodeName.get("{\"name\":\"reference1\"}").getJSONObject("node"), "reference1");
        validateNode(referenceByNodeName.get("{\"name\":\"reference2\"}").getJSONObject("node"), "reference2");
        validateNode(referenceByNodeName.get("{\"name\":\"reference3\"}").getJSONObject("node"), "reference3");
    }

    @Test
    public void shouldRetrieveReferencedNodeByReference() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList/reference1\") {"
                + "        property(name: \"j:node\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");
        JSONObject refNode = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").getJSONObject("refNode");

        validateNode(refNode, "testSubList1");
    }

    @Test
    public void shouldRetrieveReferencedNodeByUuidString() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList/reference2\") {"
                + "        property(name: \"jcr:uuid\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");
        JSONObject refNode = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").getJSONObject("refNode");

        validateNode(refNode, "reference2");
    }

    @Test
    public void shouldRetrieveReferencedNodeByPathString() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList/testSubList1\") {"
                + "        property(name: \"jcr:title\" language: \"en\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");
        JSONObject refNode = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").getJSONObject("refNode");

        validateNode(refNode, "testSubList2");
    }

    @Test
    public void shouldNotRetrieveReferencedNodeFromPropertyOfWrongType() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:lastModified\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");

        Object refNode = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").get("refNode");

        Assert.assertEquals(JSONObject.NULL, refNode);
    }

    @Test
    public void shouldNotRetrieveReferencedNodeByWrongPathString() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList/testSubList2\") {"
                + "        property(name: \"jcr:title\" language: \"en\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");

        Object refNode = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").get("refNode");

        Assert.assertEquals(JSONObject.NULL, refNode);
    }

    @Test
    public void shouldNotRetrieveReferencedNodeFromMultipleValuedProperty() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"j:liveProperties\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");
        Object refNode = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").get("refNode");

        Assert.assertEquals(JSONObject.NULL, refNode);
    }

    @Test
    public void shouldRetrieveReferencedNodes() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"j:liveProperties\") {"
                + "            refNodes {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");
        JSONArray refNodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").getJSONArray("refNodes");
        Map<String, JSONObject> refNodeByName = toItemByKeyMap("name", refNodes);

        Assert.assertEquals(2, refNodeByName.size());
        validateNode(refNodeByName.get("testSubList1"), "testSubList1");
        validateNode(refNodeByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldNotRetrieveReferencedNodesFromSingleValuedProperty() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList/reference1\") {"
                + "        property(name: \"j:node\") {"
                + "            refNodes {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "    }"
                + "}");
        Object refNodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").get("refNodes");

        Assert.assertEquals(JSONObject.NULL, refNodes);
    }
}
