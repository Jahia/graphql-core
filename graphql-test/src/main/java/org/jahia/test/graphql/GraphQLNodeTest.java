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

import org.drools.core.command.assertion.AssertEquals;
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

    private static String nodeUuid;
    private static String nodeTitleFr = "text FR";
    private static String nodeTitleEn = "text EN";
    private static String subNodeUuid1;
    private static String subNodeUuid2;
    private static String subNodeUuid3;
    private static String subNodeUuid4;
    private static String subnodeTitleFr1 = "text FR - subList1";
    private static String subnodeTitleEn1 = "text EN - subList1";
    private static String subnodeTitleFr2 = "text FR - subList2";
    private static String subnodeTitleEn2 = "text EN - subList2";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        servlet = (OsgiGraphQLServlet) BundleUtils.getOsgiService(Servlet.class, "(component.name=graphql.servlet.OsgiGraphQLServlet)");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH,
                session -> {

                    if (session.getNode("/").hasNode("testList")) {
                        session.getNode("/testList").remove();
                        session.save();
                    }

                    JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
                    node.addMixin("jmix:liveProperties");
                    node.setProperty("jcr:title", nodeTitleEn);
                    node.setProperty("j:liveProperties", new String[] {"liveProperty1", "liveProperty2"});
                    nodeUuid = node.getIdentifier();

                    JCRNodeWrapper subNode1 = node.addNode("testSubList1", "jnt:contentList");
                    subNode1.addMixin("jmix:liveProperties");
                    subNode1.setProperty("jcr:title", subnodeTitleEn1);
                    subNode1.setProperty("j:liveProperties", new String[] {"liveProperty1", "liveProperty2"});
                    subNodeUuid1 = subNode1.getIdentifier();

                    JCRNodeWrapper subNode2 = node.addNode("testSubList2", "jnt:contentList");
                    subNode2.setProperty("jcr:title", subnodeTitleEn2);
                    subNodeUuid2 = subNode2.getIdentifier();

                    JCRNodeWrapper subNode3 = node.addNode("testSubList3", "jnt:contentList");
                    subNodeUuid3 = subNode3.getIdentifier();

                    JCRNodeWrapper subNode4 = node.addNode("testSubList4", "jnt:contentList");
                    subNodeUuid4 = subNode4.getIdentifier();
                    subNode4.addNode("testSubList4_1", "jnt:contentList");
                    subNode4.addNode("testSubList4_2", "jnt:contentList");
                    subNode4.addNode("testSubList4_3", "jnt:contentList");

                    // Add references to subNode4
                    JCRNodeWrapper ref1 = node.addNode("reference1", "jnt:contentReference");
                    ref1.setProperty("j:node", subNode1);
                    JCRNodeWrapper ref2 = node.addNode("reference2", "jnt:contentReference");
                    ref2.setProperty("j:node", subNode1);
                    JCRNodeWrapper ref3 = node.addNode("reference3", "jnt:contentReference");
                    ref3.setProperty("j:node", subNode1);


                    session.save();
                    return null;
                });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH,
                session -> {
                    JCRNodeWrapper node = session.getNode("/testList");
                    node.setProperty("jcr:title", nodeTitleFr);
                    node.getNode("testSubList1").setProperty("jcr:title", subnodeTitleFr1);
                    node.getNode("testSubList2").setProperty("jcr:title", subnodeTitleFr2);
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
        Assert.assertEquals(nodeUuid, nodeByPath.getString("uuid"));
        Assert.assertEquals("testList", nodeByPath.getString("displayName"));
        Assert.assertEquals(nodeTitleFr, nodeByPath.getJSONObject("titlefr").getString("value"));
        Assert.assertEquals(nodeTitleEn, nodeByPath.getJSONObject("titleen").getString("value"));
    }

    @Test
    public void shouldRetrieveNodeByPath() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeByPath(path: \"/testList/testSubList2\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONObject nodeByPath = result.getJSONObject("data").getJSONObject("nodeByPath");

        Assert.assertEquals("/testList/testSubList2", nodeByPath.getString("path"));
        Assert.assertEquals("testSubList2", nodeByPath.getString("name"));
        Assert.assertEquals(subNodeUuid2, nodeByPath.getString("uuid"));
    }

    @Test
    public void shouldNotRetrieveNodeByPathWhenPathIsWrong() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeByPath(path: \"/testList/wrongPath\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/wrongPath");
    }

    @Test
    public void shouldNotRetrieveNodeByPathInLive() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeByPath(path: \"/testList/testSubList2\", workspace: \"live\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/testSubList2");
    }

    @Test
    public void shouldRetrieveNodesByPath() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/testSubList1\"]) {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray nodes = result.getJSONObject("data").getJSONArray("nodesByPath");
        Map<String, JSONObject> nodesByName = toItemByKeyMap("name", nodes);

        validateNode(nodesByName.get("testSubList2"), subNodeUuid2, "testSubList2","/testList/testSubList2", "/testList");
        validateNode(nodesByName.get("testSubList1"), subNodeUuid1, "testSubList1","/testList/testSubList1", "/testList");
    }

    @Test
    public void shouldNotRetrieveNodesByPathWhenAPathIsWrong() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/wrongPath\"]) {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/wrongPath");
    }

    @Test
    public void shouldNotRetrieveNodesByPathInLive() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesByPath(paths: [\"/testList/testSubList2\", \"/testList/testSubList1\"], workspace: \"live\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.PathNotFoundException: /testList/testSubList2");
    }

    @Test
    public void shouldRetrieveNodeById() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeById(uuid: \"" + subNodeUuid2 + "\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONObject nodeByPath = result.getJSONObject("data").getJSONObject("nodeById");

        Assert.assertEquals("/testList/testSubList2", nodeByPath.getString("path"));
        Assert.assertEquals("testSubList2", nodeByPath.getString("name"));
        Assert.assertEquals(subNodeUuid2, nodeByPath.getString("uuid"));
    }

    @Test
    public void shouldNotRetrieveNodeByIdWhenWhenIdIsWrong() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeById(uuid: \"badId\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: badId");
    }

    @Test
    public void shouldNotRetrieveNodeByIdInLive() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodeById(uuid: \"" + subNodeUuid2 + "\", workspace: \"live\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: " + subNodeUuid2);
    }

    @Test
    public void shouldRetrieveNodesById() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesById(uuids: [\"" + subNodeUuid2 + "\", \"" + subNodeUuid1 + "\"]) {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray nodes = result.getJSONObject("data").getJSONArray("nodesById");
        Map<String, JSONObject> nodesByName = toItemByKeyMap("name", nodes);

        validateNode(nodesByName.get("testSubList2"), subNodeUuid2, "testSubList2","/testList/testSubList2", "/testList");
        validateNode(nodesByName.get("testSubList1"), subNodeUuid1, "testSubList1","/testList/testSubList1", "/testList");
    }

    @Test
    public void shouldNotRetrieveNodesByIdWhenAnIdIsWrong() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesById(uuids: [\"" + subNodeUuid2 + "\", \"wrongId\"]) {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: wrongId");
    }

    @Test
    public void shouldNotRetrieveNodesByIdInLive() throws Exception {

        JSONObject result = executeQuery("{\n" +
                "  nodesById(uuids: [\"" + subNodeUuid2 + "\", \"" + subNodeUuid1 + "\"], workspace: \"live\") {\n" +
                "    name\n" +
                "    path\n" +
                "    uuid\n" +
                "    parent {\n" +
                "       path\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(errors.getJSONObject(0).getString("message"), "javax.jcr.ItemNotFoundException: " + subNodeUuid2);
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
        validateSingleValuedProperty(property, false, JSONObject.NULL, nodeUuid);
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
        validateSingleValuedProperty(property, false, JSONObject.NULL, nodeUuid);
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
        validateSingleValuedProperty(property, true, "fr", nodeTitleFr);
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
        validateSingleValuedProperty(property, "jcr:uuid", GqlJcrPropertyType.STRING, "/testList", false, JSONObject.NULL, nodeUuid);
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
        Map<String, JSONObject> propertyByName = toItemByKeyMap("name", properties);

        Assert.assertEquals(2, propertyByName.size());
        validateSingleValuedProperty(propertyByName.get("jcr:uuid"), "jcr:uuid", GqlJcrPropertyType.STRING, "/testList", false, JSONObject.NULL, nodeUuid);
        validateSingleValuedProperty(propertyByName.get("jcr:title"), "jcr:title", GqlJcrPropertyType.STRING, "/testList", true, "en", nodeTitleEn);
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
    public void shouldRetrieveAllChildNodes() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children {"
                                       + "            uuid"
                                       + "            name"
                                       + "            path"
                                       + "            parent {"
                                       + "                path"
                                       + "            }"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        // Three sub-list nodes plus two translation nodes.
        Assert.assertEquals(9, childByName.size());
        validateNode(childByName.get("testSubList1"), subNodeUuid1, "testSubList1", "/testList/testSubList1", "/testList");
        validateNode(childByName.get("testSubList2"), subNodeUuid2, "testSubList2", "/testList/testSubList2", "/testList");
        validateNode(childByName.get("testSubList3"), subNodeUuid3, "testSubList3", "/testList/testSubList3", "/testList");
        validateNode(childByName.get("testSubList4"), subNodeUuid4, "testSubList4", "/testList/testSubList4", "/testList");
    }

    @Test
    public void shouldRetrieveAllDescendantNodes() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList\") {"
                + "        descendants {"
                + "            uuid"
                + "            name"
                + "            path"
                + "            parent {"
                + "                path"
                + "            }"
                + "		  }"
                + "    }"
                + "}");
        JSONArray descendants = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("descendants");
        Map<String, JSONObject> descendantsByPath = toItemByKeyMap("path", descendants);

        // Three sub-list nodes plus two translation nodes.
        Assert.assertEquals(16, descendantsByPath.size());
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList2/j:translation_en"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList2/j:translation_fr"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/j:translation_fr"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_1"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_2"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_3"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList1/j:translation_en"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/j:translation_en"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList2"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList3"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList1"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList1/j:translation_fr"));
    }

    @Test
    public void shouldRetrieveChildNodesByNames() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(names: [\"testSubList1\", \"testSubList2\"]) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(2, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldRetrieveChildNodesByAnyType() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(typesFilter: {types: [\"jnt:contentList\", \"nonExistingType\"]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldRetrieveChildNodesByAllTypes() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(typesFilter: {multi: ALL types: [\"jnt:contentList\", \"jmix:liveProperties\"]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveChildNodesByPresentNonIternationalizedPropertyNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"j:liveProperties\" evaluation: PRESENT}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveChildNodesByPresentNonIternationalizedPropertyPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"j:liveProperties\" evaluation: PRESENT language:\"en\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldNotRetrieveChildNodesByPresentIternationalizedPropertyNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:title\" evaluation: PRESENT}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        // Just two translation nodes, no sub-list nodes.
        Assert.assertEquals(2, childByName.size());
        Assert.assertFalse(childByName.containsKey("testSubList1"));
    }

    @Test
    public void shouldRetrieveChildNodesByPresentIternationalizedPropertyPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:title\" evaluation: PRESENT language:\"en\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        // Two sub-list node, plus two translation nodes.
        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldRetrieveChildNodesByAbsentNonIternationalizedProperty() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"j:liveProperties\" evaluation: ABSENT}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        // Two sub-list nodes, plus two translation nodes.
        Assert.assertEquals(8, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldRetrieveChildNodesByAbsentIternationalizedPropertyNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:title\" evaluation: ABSENT}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(7, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldRetrieveChildNodesByAbsentIternationalizedPropertyPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:title\" evaluation: ABSENT language:\"fr\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(5, childByName.size());
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldRetrieveChildNodesByEqualNonIternationalizedPropertyValueNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:uuid\" value: \"" + subNodeUuid1 + "\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveChildNodesByEqualNonIternationalizedPropertyValuePassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:uuid\" value: \"" + subNodeUuid1 + "\" language: \"en\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldNotRetrieveChildNodesByEqualInternationalizedPropertyValueNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn1 + "\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(0, childByName.size());
    }

    @Test
    public void shouldRetrieveChildNodesByEqualternationalizedPropertyValuePassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn1 + "\" language: \"en\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveChildNodesByDifferentNonIternationalizedPropertyValueNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:uuid\" value: \"" + subNodeUuid1 + "\" evaluation: DIFFERENT}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        // Two sub-list nodes, plus two translation nodes.
        Assert.assertEquals(8, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldRetrieveChildNodesByDifferentNonIternationalizedPropertyValuePassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:uuid\" value: \"" + subNodeUuid1 + "\" evaluation: DIFFERENT language: \"en\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        // Two sub-list nodes, plus two translation nodes.
        Assert.assertEquals(8, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldRetrieveChildNodesByDifferentInternationalizedPropertyValueNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn1 + "\" evaluation: DIFFERENT}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        // Three sub-list nodes, plus two translation nodes.
        Assert.assertEquals(9, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldRetrieveChildNodesByDifferentInternationalizedPropertyValuePassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn1 + "\" evaluation: DIFFERENT language: \"en\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        // Two sub-list nodes, plus two translation nodes.
        Assert.assertEquals(8, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldRetrieveChildNodesByAllPropertyValues() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {filters: ["
                                       + "            {property: \"jcr:primaryType\" value: \"jnt:contentList\"}"
                                       + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn2 + "\" language: \"en\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldRetrieveChildNodesByAnyPropertyValue() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children(propertiesFilter: {multi: ANY filters: ["
                                       + "            {property: \"jcr:primaryType\" value: \"jnt:contentList\"}"
                                       + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn2 + "\" language: \"en\"}"
                                       + "        ]}) {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    // TODO: Add tests to verify that a validation exception is thrown in case null property value is passed in combination with EQUAL or DIFFERENT evaluation type.
    // Note, currently exceptions handling/reporting is broken in the graphql-java causing StackOverflowError.

    @Test
    public void shouldRetrieveChildNodesByNameTypeAndPropertyValue() throws Exception {

        JSONObject result = executeQuery("{"
                                       + "    nodeByPath(path: \"/testList\") {"
                                       + "        children("
                                       + "            typesFilter: {types: [\"jmix:liveProperties\"]}"
                                       + "            propertiesFilter: {filters: ["
                                       + "                {property: \"jcr:title\" value: \"" + subnodeTitleFr1 + "\" language: \"fr\"}"
                                       + "            ]})"
                                       + "        {"
                                       + "            name"
                                       + "		  }"
                                       + "    }"
                                       + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("children");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveParent() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        parent {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONObject parent = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONObject("parent");
        validateNode(parent, "testSubList4");
    }

    @Test
    public void shouldRetrieveAncestors() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        ancestors {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONArray ancestors = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("ancestors");
        Map<String, JSONObject> ancestorsByName = toItemByKeyMap("name", ancestors);

        Assert.assertEquals(3, ancestorsByName.size());
        validateNode(ancestorsByName.get(""), "");
        validateNode(ancestorsByName.get("testList"), "testList");
        validateNode(ancestorsByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldRetrieveReferences() throws Exception {

        JSONObject result = executeQuery("{ \n" +
                "  nodeByPath(path: \"/testList/testSubList1\" ) {" +
                "    references {" +
                "     parentNode {" +
                "        name" +
                "      }" +
                "    }" +
                "  }" +
                "}");
        JSONArray referencesByName = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("references");
        Map<String, JSONObject> referenceByName = toItemByKeyMap("parentNode", referencesByName);

        Assert.assertEquals(3, referenceByName.size());
        Assert.assertEquals(referenceByName.get("{\"name\":\"reference1\"}").getJSONObject("parentNode").getString("name"), "reference1");
        Assert.assertEquals(referenceByName.get("{\"name\":\"reference2\"}").getJSONObject("parentNode").getString("name"), "reference2");
        Assert.assertEquals(referenceByName.get("{\"name\":\"reference3\"}").getJSONObject("parentNode").getString("name"), "reference3");
    }

    @Test
    public void shouldRetrieveAncestorsUpToPath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList/testSubList4/testSubList4_1\") {"
                + "        ancestors(upToPath: \"/testList\") {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONArray ancestors = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("ancestors");
        Map<String, JSONObject> ancestorsByName = toItemByKeyMap("name", ancestors);

        Assert.assertEquals(2, ancestorsByName.size());
        validateNode(ancestorsByName.get("testList"), "testList");
        validateNode(ancestorsByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldRetrieveNodesUsingSQL2Query() throws Exception {
        testQuery("select * from [jnt:contentList] where isdescendantnode('/testList')", "SQL2", 7);
    }

    @Test
    public void shouldRetrieveNodesUsingXPATHQuery() throws Exception {
        testQuery("/jcr:root/testList//element(*, jnt:contentList)", "XPATH", 13);
    }

    private void testQuery(String query, String language, long expectedNumber) throws Exception {
        JSONObject result = executeQuery("{"
                + "    nodesByQuery(query: \"" + query + "\", queryLanguage: " + language + ") {"
                + "        edges {"
                + "            node {"
                + "                name"
                + "                path"
                + "            }"
                + "		  }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("nodesByQuery").getJSONArray("edges");
        Assert.assertEquals(expectedNumber, nodes.length());
    }

    private static Map<String, JSONObject> toItemByKeyMap(String key, JSONArray items) throws JSONException {
        HashMap<String, JSONObject> itemByName = new HashMap<>(items.length());
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            itemByName.put(item.getString(key), item);
        }
        return itemByName;
    }

    private static void validateSingleValuedProperty(JSONObject property, boolean expectedInternationalized, Object expectedLanguage, String expectedValue) throws JSONException {
        Assert.assertEquals(expectedInternationalized, property.getBoolean("internationalized"));
        Assert.assertEquals(expectedLanguage, property.get("language"));
        Assert.assertEquals(expectedValue, property.getString("value"));
        Assert.assertEquals(JSONObject.NULL, property.get("values"));
    }

    private static void validateSingleValuedProperty(JSONObject property, String expectedName, GqlJcrPropertyType expectedType, String expectedParentNodePath, boolean expectedInternationalized, Object expectedLanguage, String expectedValue) throws JSONException {
        Assert.assertEquals(expectedName, property.getString("name"));
        Assert.assertEquals(expectedType.name(), property.getString("type"));
        Assert.assertEquals(expectedParentNodePath, property.getJSONObject("parentNode").getString("path"));
        validateSingleValuedProperty(property, expectedInternationalized, expectedLanguage, expectedValue);
    }

    private static void validateNode(JSONObject node, String expectedName) throws JSONException {
        Assert.assertEquals(expectedName, node.getString("name"));
    }

    private static void validateNode(JSONObject node, String expectedUuid, String expectedName, String expectedPath, String expectedParentNodePath) throws JSONException {
        validateNode(node, expectedName);
        Assert.assertEquals(expectedUuid, node.getString("uuid"));
        Assert.assertEquals(expectedPath, node.getString("path"));
        Assert.assertEquals(expectedParentNodePath, node.getJSONObject("parent").getString("path"));
    }

    private JSONObject executeQuery(String query) throws JSONException {
        return new JSONObject(servlet.executeQuery(query));
    }
}