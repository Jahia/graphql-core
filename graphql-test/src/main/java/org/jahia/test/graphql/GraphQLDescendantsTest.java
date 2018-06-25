/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
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
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children {"
                + "        nodes {"
                + "            uuid"
                + "            name"
                + "            path"
                + "            parent {"
                + "                path"
                + "            }"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
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
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(names: [\"testSubList1\", \"testSubList2\"]) {"
                + "        nodes {"
                + "            name"
                + "	       }"
                + "	       }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(2, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldRetrieveChildNodesByAnyType() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(typesFilter: {types: [\"jnt:contentList\", \"nonExistingType\"]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
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
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(typesFilter: {multi: ALL types: [\"jnt:contentList\", \"jmix:liveProperties\"]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveChildNodesByPresentNonIternationalizedPropertyNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"j:liveProperties\" evaluation: PRESENT}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveChildNodesByPresentNonIternationalizedPropertyPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"j:liveProperties\" evaluation: PRESENT language:\"en\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldNotRetrieveChildNodesByPresentIternationalizedPropertyNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:title\" evaluation: PRESENT}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(0, childByName.size());
    }

    @Test
    public void shouldRetrieveChildNodesByPresentIternationalizedPropertyPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:title\" evaluation: PRESENT language:\"en\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(2, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldRetrieveChildNodesByAbsentNonIternationalizedProperty() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"j:liveProperties\" evaluation: ABSENT}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(3, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldRetrieveChildNodesByAbsentIternationalizedPropertyNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:title\" evaluation: ABSENT}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
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
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:title\" evaluation: ABSENT language:\"fr\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(2, childByName.size());
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldRetrieveChildNodesByEqualNonIternationalizedPropertyValueNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:uuid\" value: \"" + subNodeUuid1 + "\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveChildNodesByEqualNonIternationalizedPropertyValuePassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:uuid\" value: \"" + subNodeUuid1 + "\" language: \"en\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldNotRetrieveChildNodesByEqualInternationalizedPropertyValueNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn1 + "\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(0, childByName.size());
    }

    @Test
    public void shouldRetrieveChildNodesByEqualternationalizedPropertyValuePassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn1 + "\" language: \"en\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveChildNodesByDifferentNonIternationalizedPropertyValueNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:uuid\" value: \"" + subNodeUuid1 + "\" evaluation: DIFFERENT}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(3, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldRetrieveChildNodesByDifferentNonIternationalizedPropertyValuePassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:uuid\" value: \"" + subNodeUuid1 + "\" evaluation: DIFFERENT language: \"en\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(3, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldRetrieveChildNodesByDifferentInternationalizedPropertyValueNotPassingLanguage() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn1 + "\" evaluation: DIFFERENT}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
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
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn1 + "\" evaluation: DIFFERENT language: \"en\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(3, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
        validateNode(childByName.get("testSubList4"), "testSubList4");
    }

    @Test
    public void shouldRetrieveChildNodesByAllPropertyValues() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:primaryType\" value: \"jnt:contentList\"}"
                + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn2 + "\" language: \"en\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList2"), "testSubList2");
    }

    @Test
    public void shouldRetrieveChildNodesByAnyPropertyValue() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {multi: ANY filters: ["
                + "            {property: \"jcr:primaryType\" value: \"jnt:contentList\"}"
                + "            {property: \"jcr:title\" value: \"" + subnodeTitleEn2 + "\" language: \"en\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(4, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
        validateNode(childByName.get("testSubList2"), "testSubList2");
        validateNode(childByName.get("testSubList3"), "testSubList3");
    }

    @Test
    public void shouldGetErrorNotRetrieveChildNodesByInconsistentEqualPropertyFilter() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:primaryType\"}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");

        validateError(result, "Property value is required for EQUAL evaluation");
    }

    @Test
    public void shouldGetErrorNotRetrieveChildNodesByInconsistentDifferentPropertyFilter() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children(propertiesFilter: {filters: ["
                + "            {property: \"jcr:primaryType\" evaluation: DIFFERENT}"
                + "        ]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");

        validateError(result, "Property value is required for DIFFERENT evaluation");
    }

    @Test
    public void shouldRetrieveChildNodesByNameTypeAndPropertyValue() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        children("
                + "            typesFilter: {types: [\"jmix:liveProperties\"]}"
                + "            propertiesFilter: {filters: ["
                + "                {property: \"jcr:title\" value: \"" + subnodeTitleFr1 + "\" language: \"fr\"}"
                + "            ]})"
                + "        {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray children = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
        Map<String, JSONObject> childByName = toItemByKeyMap("name", children);

        Assert.assertEquals(1, childByName.size());
        validateNode(childByName.get("testSubList1"), "testSubList1");
    }

    @Test
    public void shouldRetrieveAllDescendantNodes() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        descendants {"
                + "        nodes {"
                + "            path"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray descendants = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("descendants").getJSONArray("nodes");
        Map<String, JSONObject> descendantsByPath = toItemByKeyMap("path", descendants);

        Assert.assertEquals(7, descendantsByPath.size());
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
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        descendants(typesFilter: {multi: ALL, types: [\"jnt:contentList\", \"jmix:tagged\"]}) {"
                + "        nodes {"
                + "            name"
                + "		  }"
                + "		  }"
                + "    }"
                + "    }"
                + "}");
        JSONArray descendants = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("descendants").getJSONArray("nodes");
        Map<String, JSONObject> descendantsByName = toItemByKeyMap("name", descendants);

        Assert.assertEquals(1, descendantsByName.size());
        validateNode(descendantsByName.get("testSubList4_1"), "testSubList4_1");
    }

    @Test
    public void shouldRetrieveChildNodeByName() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "      nodeByPath(path: \"/testList\") {"
                + "        descendant(relPath:\"testSubList1\") {"
                + "          name"
                + "		   }"
                + "      }"
                + "    }"
                + "}");

        JSONObject child = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("descendant");
        validateNode(child, "testSubList1");
    }

    @Test
    public void shouldNotRetrieveChildNodeByName() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "      nodeByPath(path: \"/testList\") {"
                + "        descendant(relPath:\"testSubList99\") {"
                + "          name"
                + "		   }"
                + "      }"
                + "    }"
                + "}");

        Object child = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").get("descendant");
        Assert.assertEquals(JSONObject.NULL, child);
    }

    @Test
    public void shouldRetrieveDescendantNodeByRelativePath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "      nodeByPath(path: \"/testList\") {"
                + "        descendant(relPath:\"testSubList4/testSubList4_1\") {"
                + "          name"
                + "		   }"
                + "      }"
                + "    }"
                + "}");

        JSONObject descendant = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("descendant");
        validateNode(descendant, "testSubList4_1");
    }

    @Test
    public void shouldGetErrorNotRetrieveAncestorNodeByRelativePath() throws Exception {

        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "      nodeByPath(path: \"/testList\") {"
                + "        descendant(relPath:\"..\") {"
                + "          name"
                + "		   }"
                + "      }"
                + "    }"
                + "}");

        validateError(result, "No navigation outside of the node sub-tree is supported");
    }
}
