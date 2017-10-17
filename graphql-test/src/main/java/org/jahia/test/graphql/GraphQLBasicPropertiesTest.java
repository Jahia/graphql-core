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

import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

public class GraphQLBasicPropertiesTest extends GraphQLAbstractTest {

    @Test
    public void shouldRetrievePropertyWithBasicFileds() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:uuid\") {"
                + "            name"
                + "            type"
                + "            parentNode {"
                + "                path"
                + "            }"
                + "		  }"
                + "    }"
                + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("property");

        Assert.assertEquals("jcr:uuid", property.getString("name"));
        Assert.assertEquals(GqlJcrPropertyType.STRING.name(), property.getString("type"));
        Assert.assertEquals("/testList", property.getJSONObject("parentNode").getString("path"));
    }

    @Test
    public void shouldRetrieveMultivaluedProperty() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"j:liveProperties\") {"
                + "            value"
                + "            values"
                + "		  }"
                + "    }"
                + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("property");
        JSONArray values = property.getJSONArray("values");
        HashSet<String> vals = new HashSet<>(values.length());
        for (int i = 0; i < values.length(); i++) {
            vals.add(values.getString(i));
        }

        Assert.assertEquals(JSONObject.NULL, property.get("value"));
        Assert.assertEquals(2, vals.size());
        Assert.assertTrue(vals.contains("liveProperty1"));
        Assert.assertTrue(vals.contains("liveProperty2"));
    }
}
