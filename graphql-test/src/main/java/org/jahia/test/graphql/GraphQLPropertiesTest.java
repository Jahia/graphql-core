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
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class GraphQLPropertiesTest extends GraphQLTestSupport {

    private static String nodeUuid;
    private static String nodeTitleFr = "text FR";
    private static String nodeTitleEn = "text EN";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            node.addMixin("jmix:liveProperties");
            node.setProperty("jcr:title", nodeTitleEn);
            node.setProperty("j:liveProperties", new String[] {"liveProperty1", "liveProperty2"});
            nodeUuid = node.getIdentifier();

            session.save();
            return null;
        });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH, session -> {
            JCRNodeWrapper node = session.getNode("/testList");
            node.setProperty("jcr:title", nodeTitleFr);
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrievePropertyWithBasicFileds() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:uuid\") {"
                + "            name"
                + "            type"
                + "            node {"
                + "                path"
                + "            }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property");

        Assert.assertEquals("jcr:uuid", property.getString("name"));
        Assert.assertEquals(GqlJcrPropertyType.STRING.name(), property.getString("type"));
        Assert.assertEquals("/testList", property.getJSONObject("node").getString("path"));
    }

    @Test
    public void shouldRetrieveNonInternationalizedPropertyNotPassingLanguage() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:uuid\") {"
                + "            internationalized"
                + "            language"
                + "            value"
                + "            values"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property");
        validateSingleValuedProperty(property, false, JSONObject.NULL, nodeUuid);
    }

    @Test
    public void shouldRetrieveNonInternationalizedPropertyPassingLanguage() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:uuid\" language: \"en\") {"
                + "            internationalized"
                + "            language"
                + "            value"
                + "            values"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property");
        validateSingleValuedProperty(property, false, JSONObject.NULL, nodeUuid);
    }

    @Test
    public void shouldNotRetrieveInternationalizedPropertyNotPassingLanguage() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:title\") {"
                + "            internationalized"
                + "            language"
                + "            value"
                + "            values"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        Object property = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").get("property");
        Assert.assertEquals(JSONObject.NULL, property);
    }

    @Test
    public void shouldRetrieveInternationalizedPropertyPassingLanguage() throws Exception {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"jcr:title\" language: \"fr\") {"
                + "            internationalized"
                + "            language"
                + "            value"
                + "            values"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property");
        validateSingleValuedProperty(property, true, "fr", nodeTitleFr);
    }

    @Test
    public void shouldRetrieveNonInternationalizedPropertiesNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        properties(names: [\"jcr:uuid\", \"jcr:title\"]) {"
                + "            name"
                + "            type"
                + "            internationalized"
                + "            language"
                + "            value"
                + "            values"
                + "            node {"
                + "                path"
                + "            }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray properties = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONArray("properties");

        Assert.assertEquals(1, properties.length());
        JSONObject property = properties.getJSONObject(0);
        validateSingleValuedProperty(property, "jcr:uuid", GqlJcrPropertyType.STRING, "/testList", false, JSONObject.NULL, nodeUuid);
    }

    @Test
    public void shouldRetrieveInternationalizedAndNonInternationalizedPropertiesPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        properties(names: [\"jcr:uuid\", \"jcr:title\"] language: \"en\") {"
                + "            name"
                + "            type"
                + "            internationalized"
                + "            language"
                + "            value"
                + "            values"
                + "            node {"
                + "                path"
                + "            }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray properties = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONArray("properties");
        Map<String, JSONObject> propertyByName = toItemByKeyMap("name", properties);

        Assert.assertEquals(2, propertyByName.size());
        validateSingleValuedProperty(propertyByName.get("jcr:uuid"), "jcr:uuid", GqlJcrPropertyType.STRING, "/testList", false, JSONObject.NULL, nodeUuid);
        validateSingleValuedProperty(propertyByName.get("jcr:title"), "jcr:title", GqlJcrPropertyType.STRING, "/testList", true, "en", nodeTitleEn);
    }

    @Test
    public void shouldRetrieveAllPropertiesPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        properties(language: \"fr\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray properties = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONArray("properties");
        Map<String, JSONObject> propertyByName = toItemByKeyMap("name", properties);

        Assert.assertEquals(15, propertyByName.size());
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("j:liveProperties"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("j:nodename"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("j:originWS"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:baseVersion"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:created"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:createdBy"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:isCheckedOut"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:lastModified"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:lastModifiedBy"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:mixinTypes"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:predecessors"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:primaryType"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:uuid"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:versionHistory"));
        Assert.assertNotEquals(JSONObject.NULL, propertyByName.get("jcr:title"));
    }

    @Test
    public void shouldRetrieveMultivaluedProperty() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        property(name: \"j:liveProperties\") {"
                + "            value"
                + "            values"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property");
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

    protected static void validateSingleValuedProperty(JSONObject property, String expectedName, GqlJcrPropertyType expectedType, String expectedNodePath, boolean expectedInternationalized, Object expectedLanguage, String expectedValue) throws JSONException {
        Assert.assertEquals(expectedName, property.getString("name"));
        Assert.assertEquals(expectedType.name(), property.getString("type"));
        Assert.assertEquals(expectedNodePath, property.getJSONObject("node").getString("path"));
        validateSingleValuedProperty(property, expectedInternationalized, expectedLanguage, expectedValue);
    }

    protected static void validateSingleValuedProperty(JSONObject property, boolean expectedInternationalized, Object expectedLanguage, String expectedValue) throws JSONException {
        Assert.assertEquals(expectedInternationalized, property.getBoolean("internationalized"));
        Assert.assertEquals(expectedLanguage, property.get("language"));
        Assert.assertEquals(expectedValue, property.getString("value"));
        Assert.assertEquals(JSONObject.NULL, property.get("values"));
    }
}
