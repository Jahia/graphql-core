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

import java.util.Map;

public class GraphQLReferencesTest extends GraphQLTestSupport {

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
}
