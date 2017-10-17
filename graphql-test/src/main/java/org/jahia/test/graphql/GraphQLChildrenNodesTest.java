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

public class GraphQLChildrenNodesTest extends GraphQLAbstractTest {

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
                + "            path"
                + "		  }"
                + "    }"
                + "}");
        JSONArray descendants = result.getJSONObject("data").getJSONObject("nodeByPath").getJSONArray("descendants");
        Map<String, JSONObject> descendantsByPath = toItemByKeyMap("path", descendants);

        Assert.assertEquals(16, descendantsByPath.size());
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_1"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_2"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList4/testSubList4_3"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList2"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList3"));
        Assert.assertTrue(descendantsByPath.containsKey("/testList/testSubList1"));
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

}
