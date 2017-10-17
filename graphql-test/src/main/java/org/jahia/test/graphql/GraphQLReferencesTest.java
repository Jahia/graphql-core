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
            JCRNodeWrapper subNode = node.addNode("testSubList1", "jnt:contentList");

            JCRNodeWrapper ref1 = node.addNode("reference1", "jnt:contentReference");
            ref1.setProperty("j:node", subNode);
            JCRNodeWrapper ref2 = node.addNode("reference2", "jnt:contentReference");
            ref2.setProperty("j:node", subNode);

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

        Assert.assertEquals(2, referenceByNodeName.size());
        validateNode(referenceByNodeName.get("{\"name\":\"reference1\"}").getJSONObject("parentNode"), "reference1");
        validateNode(referenceByNodeName.get("{\"name\":\"reference2\"}").getJSONObject("parentNode"), "reference2");
    }
}
