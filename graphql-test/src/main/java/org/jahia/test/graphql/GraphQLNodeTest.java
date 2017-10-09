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

import graphql.servlet.OsgiGraphQLServlet;

import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.test.JahiaTestCase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.Servlet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;

public class GraphQLNodeTest extends JahiaTestCase {

    private static OsgiGraphQLServlet servlet;

    private static String testedNodeUUID = null;
    private static String testedNodeTitleFR = "text FR";
    private static String testedNodeTitleEN = "text EN";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        servlet = (OsgiGraphQLServlet) BundleUtils.getOsgiService(Servlet.class, "(component.name=graphql.servlet.OsgiGraphQLServlet)");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH,
                session -> {
                    if (session.getNode("/").hasNode("testList")) {
                        session.getNode("/testList").remove();
                        session.save();
                    }
                    JCRNodeWrapper testedNode = session.getNode("/").addNode("testList", "jnt:contentList");
                    testedNode.addMixin("jmix:liveProperties");
                    testedNode.setProperty("jcr:title", testedNodeTitleEN);
                    testedNode.setProperty("j:liveProperties", new String[] {"liveProperty1", "liveProperty2"});
                    testedNodeUUID = testedNode.getIdentifier();
                    session.save();
                    return null;
                });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH,
                session -> {
                    JCRNodeWrapper testedNode = session.getNode("/testList");
                    testedNode.setProperty("jcr:title", testedNodeTitleFR);
                    session.save();
                    return null;
                }
        );
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH,
                session -> {
                    if (session.getNode("/").hasNode("testList")) {
                        session.getNode("/testList").remove();
                        session.save();
                    }
                    return null;
                });
    }

    @Test
    public void testGetNode() throws Exception {

        JSONObject result = executeQuery("{ nodeByPath(path: \"/testList\") { name path uuid displayName \t titleen:property(name: \"jcr:title\", " +
                "language:\"en\") {\n" +
                "        value\n" +
                "      } \n" +
                "    \t titlefr:property(name: \"jcr:title\", language:\"fr\") {\n" +
                "        value\n" +
                "      } \n" +
                "    } }");
        JSONObject nodeByPath = result.getJSONObject("data").getJSONObject("nodeByPath");

        Assert.assertEquals("/testList", nodeByPath.getString("path"));
        Assert.assertEquals("testList", nodeByPath.getString("name"));
        Assert.assertEquals(testedNodeUUID, nodeByPath.getString("uuid"));
        Assert.assertEquals("testList", nodeByPath.getString("displayName"));
        Assert.assertEquals(testedNodeTitleFR, nodeByPath.getJSONObject("titlefr").getString("value"));
        Assert.assertEquals(testedNodeTitleEN, nodeByPath.getJSONObject("titleen").getString("value"));
    }

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
    public void shouldRetrieveNonInternationalizedPropertyNotPassingLanguage() throws Exception {
        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        property(name: \"jcr:uuid\") {"
                                       + "            internationalized"
                                       + "            language"
                                       + "            value"
                                       + "            values"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("property");
        validateSingleValuedProperty(property, false, JSONObject.NULL, testedNodeUUID);
    }

    @Test
    public void shouldRetrieveNonInternationalizedPropertyPassingLanguage() throws Exception {
        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        property(name: \"jcr:uuid\" language: \"en\") {"
                                       + "            internationalized"
                                       + "            language"
                                       + "            value"
                                       + "            values"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("property");
        validateSingleValuedProperty(property, false, JSONObject.NULL, testedNodeUUID);
    }

    @Test
    public void shouldNotRetrieveInternationalizedPropertyNotPassingLanguage() throws Exception {
        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        property(name: \"jcr:title\") {"
                                       + "            internationalized"
                                       + "            language"
                                       + "            value"
                                       + "            values"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        Object property = result.getJSONObject("data").getJSONObject("nodeByPath").get("property");
        Assert.assertEquals(JSONObject.NULL, property);
    }

    @Test
    public void shouldRetrieveInternationalizedPropertyPassingLanguage() throws Exception {
        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        property(name: \"jcr:title\" language: \"fr\") {"
                                       + "            internationalized"
                                       + "            language"
                                       + "            value"
                                       + "            values"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONObject property = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("property");
        validateSingleValuedProperty(property, true, "fr", testedNodeTitleFR);
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

    @Test
    public void shouldRetrieveNonInternationalizedPropertiesNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        properties(names: [\"jcr:uuid\", \"jcr:title\"]) {"
                                       + "            name"
                                       + "            type"
                                       + "            internationalized"
                                       + "            language"
                                       + "            value"
                                       + "            values"
                                       + "            parentNode {"
                                       + "                path"
                                       + "            }"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray properties = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("properties");

        Assert.assertEquals(1, properties.length());
        JSONObject property = properties.getJSONObject(0);
        validateSingleValuedProperty(property, "jcr:uuid", GqlJcrPropertyType.STRING, false, JSONObject.NULL, testedNodeUUID);
    }

    @Test
    public void shouldRetrieveInternationalizedAndNonInternationalizedPropertiesPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        properties(names: [\"jcr:uuid\", \"jcr:title\"] language: \"en\") {"
                                       + "            name"
                                       + "            type"
                                       + "            internationalized"
                                       + "            language"
                                       + "            value"
                                       + "            values"
                                       + "            parentNode {"
                                       + "                path"
                                       + "            }"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray properties = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("properties");
        Map<String, JSONObject> propertyByName = toPropertyByNameMap(properties);

        Assert.assertEquals(2, propertyByName.size());
        validateSingleValuedProperty(propertyByName.get("jcr:uuid"), "jcr:uuid", GqlJcrPropertyType.STRING, false, JSONObject.NULL, testedNodeUUID);
        validateSingleValuedProperty(propertyByName.get("jcr:title"), "jcr:title", GqlJcrPropertyType.STRING, true, "en", testedNodeTitleEN);
    }

    @Test
    public void shouldRetrieveAllPropertiesPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        properties(language: \"fr\") {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray properties = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("properties");
        Map<String, JSONObject> propertyByName = toPropertyByNameMap(properties);

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

    private static Map<String, JSONObject> toPropertyByNameMap(JSONArray properties) throws JSONException {
        HashMap<String, JSONObject> propertyByName = new HashMap<>(properties.length());
        for (int i = 0; i < properties.length(); i++) {
            JSONObject property = properties.getJSONObject(i);
            propertyByName.put(property.getString("name"), property);
        }
        return propertyByName;
    }

    private static void validateSingleValuedProperty(JSONObject property, boolean expectedInternationalized, Object expectedLanguage, String expectedValue) throws JSONException {
        Assert.assertEquals(expectedInternationalized, property.getBoolean("internationalized"));
        Assert.assertEquals(expectedLanguage, property.get("language"));
        Assert.assertEquals(expectedValue, property.getString("value"));
        Assert.assertEquals(JSONObject.NULL, property.get("values"));
    }

    private static void validateSingleValuedProperty(JSONObject property, String expectedName, GqlJcrPropertyType expectedType, boolean expectedInternationalized, Object expectedLanguage, String expectedValue) throws JSONException {
        Assert.assertEquals(expectedName, property.getString("name"));
        Assert.assertEquals(expectedType.name(), property.getString("type"));
        Assert.assertEquals("/testList", property.getJSONObject("parentNode").getString("path"));
        validateSingleValuedProperty(property, expectedInternationalized, expectedLanguage, expectedValue);
    }

    private JSONObject executeQuery(String query) throws JSONException {
        return new JSONObject(servlet.executeQuery(query));
    }
}