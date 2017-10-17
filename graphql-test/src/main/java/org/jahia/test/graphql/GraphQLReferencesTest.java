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
            node.setProperty("jcr:title", "/nonExistingPath");

            JCRNodeWrapper subNode1 = node.addNode("testSubList1", "jnt:contentList");

            JCRNodeWrapper subNode2 = node.addNode("testSubList2", "jnt:contentList");
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
                + "    nodeByPath(path: \"/testList/testSubList1\") {"
                + "        references {"
                + "            parentNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "}");
        JSONArray references = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("references");
        Map<String, JSONObject> referenceByNodeName = toItemByKeyMap("parentNode", references);

        Assert.assertEquals(3, referenceByNodeName.size());
        validateNode(referenceByNodeName.get("{\"name\":\"reference1\"}").getJSONObject("parentNode"), "reference1");
        validateNode(referenceByNodeName.get("{\"name\":\"reference2\"}").getJSONObject("parentNode"), "reference2");
        validateNode(referenceByNodeName.get("{\"name\":\"reference3\"}").getJSONObject("parentNode"), "reference3");
    }

    @Test
    public void shouldRetrieveReferencedNodeByReference() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/reference1\") {"
                + "        property(name: \"j:node\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "}");
        JSONObject refNode = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("property").getJSONObject("refNode");

        validateNode(refNode, "testSubList1");
    }

    @Test
    public void shouldRetrieveReferencedNodeByString() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/reference2\") {"
                + "        property(name: \"jcr:uuid\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "}");
        JSONObject refNode = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("property").getJSONObject("refNode");

        validateNode(refNode, "reference2");
    }

    @Test
    public void shouldGetErrorNotRetrieveReferencedNodeFromPropertyOfWrongType() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:lastModified\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(1, errors.length());
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "The 'jcr:lastModified' property is not of a reference type");
    }

    @Test
    public void shouldGetErrorNotRetrieveReferencedNodeByWrongString() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:title\" language: \"en\") {"
                + "            refNode {"
                + "                name"
                + "            }"
                + "        }"
                + "    }"
                + "}");
        JSONArray errors = result.getJSONArray("errors");

        Assert.assertEquals(1, errors.length());
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "The value of the 'jcr:title' property does not reference an existing node");
    }
}
