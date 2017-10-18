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

public class GraphQLDescendantsTest extends GraphQLTestSupport {

    private static String subNodeUuid1;
    private static String subNodeUuid2;
    private static String subNodeUuid3;
    private static String subNodeUuid4;
    private static String subnodeTitleEn1 = "text EN - subList1";
    private static String subnodeTitleFr1 = "text FR - subList1";
    private static String subnodeTitleEn2 = "text EN - subList2";
    private static String subnodeTitleFr2 = "text FR - subList2";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");

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
            subNode4.addNode("testSubList4_1", "jnt:contentList").addMixin("jmix:tagged");
            subNode4.addNode("testSubList4_2", "jnt:contentList");
            subNode4.addNode("testSubList4_3", "jnt:contentList");

            session.save();
            return null;
        });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH, session -> {
            JCRNodeWrapper node = session.getNode("/testList");
            node.getNode("testSubList1").setProperty("jcr:title", subnodeTitleFr1);
            node.getNode("testSubList2").setProperty("jcr:title", subnodeTitleFr2);
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
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

        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), subNodeUuid1, "testSubList1", "/testList/testSubList1", "/testList");
        validateNode(childByName.get("testSubList2"), subNodeUuid2, "testSubList2", "/testList/testSubList2", "/testList");
        validateNode(childByName.get("testSubList3"), subNodeUuid3, "testSubList3", "/testList/testSubList3", "/testList");
        validateNode(childByName.get("testSubList4"), subNodeUuid4, "testSubList4", "/testList/testSubList4", "/testList");
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

        Assert.assertEquals(0, childByName.size());
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

        Assert.assertEquals(2, childByName.size());
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

        Assert.assertEquals(3, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
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

        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
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

        Assert.assertEquals(2, childByName.size());
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
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

        Assert.assertEquals(3, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
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

        Assert.assertEquals(3, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
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

        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
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

        Assert.assertEquals(3, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
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
    public void shouldRetrieveAllDescendantNodes() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList\") {"
                + "        descendants {"
                + "            path"
                + "		  }"
                + "    }"
                + "}");
        JSONArray descendants = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("descendants");
        Map<String, JSONObject> descendantsByPath = toItemByKeyMap("path", descendants);

        Assert.assertEquals(11, descendantsByPath.size());
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList1"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList2"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList3"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_1"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_2"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_3"));
    }

    @Test
    public void shouldRetrieveDescendantNodesByAllTypes() throws Exception {

        JSONObject result = executeQuery("{"
                + "    nodeByPath(path: \"/testList\") {"
                + "        descendants(typesFilter: {multi: ALL, types: [\"jnt:contentList\", \"jmix:tagged\"]}) {"
                + "            name"
                + "		  }"
                + "    }"
                + "}");
        JSONArray descendants = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("descendants");
        Map<String, JSONObject> descendantsByName = toItemByKeyMap("name", descendants);

        Assert.assertEquals(1, descendantsByName.size());
        validateNode(descendantsByName.get("testSubList4_1"), "testSubList4_1");
    }
}
