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
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraphQLNodeTypesTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            node.addMixin("jmix:renderable");
            JCRNodeWrapper subNode = node.addNode("testSubList", "jnt:contentList");
            subNode.addNode("testSubSubList", "jnt:contentList");
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldGetNodeType() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr{\n" +
                "    nodeByPath(path:\"/testList\") {\n" +
                "      primaryNodeType {\n" +
                "        name\n" +
                "        mixin\n" +
                "        hasOrderableChildNodes\n" +
                "        queryable\n" +
                "        systemId\n" +
                "      }\n" +
                "      mixinTypes{\n" +
                "        name\n" +
                "        mixin\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
        JSONObject nodeType = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("primaryNodeType");
        JSONArray mixins = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONArray("mixinTypes");

        Assert.assertEquals("jnt:contentList", nodeType.getString("name"));
        Assert.assertEquals(false, nodeType.getBoolean("mixin"));
        Assert.assertEquals(true, nodeType.getBoolean("hasOrderableChildNodes"));
        Assert.assertEquals(true, nodeType.getBoolean("queryable"));
        Assert.assertEquals("system-jahia", nodeType.getString("systemId"));

        Assert.assertEquals(1, mixins.length());
        Assert.assertEquals("jmix:renderable", mixins.getJSONObject(0).getString("name"));
        Assert.assertEquals(true, mixins.getJSONObject(0).getBoolean("mixin"));
    }

    @Test
    public void shouldTestNodeType() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr {\n" +
                "    nodeByPath(path: \"/testList\") {\n" +
                "      test1: isNodeType(type: {types: [\"jnt:contentList\"]})\n" +
                "      test2: isNodeType(type: {types: [\"jmix:renderable\"]})\n" +
                "      test3: isNodeType(type: {types: [\"jnt:content\", \"jnt:virtualsite\"], multi: ALL})\n" +
                "      test4: isNodeType(type: {types: [\"jnt:content\", \"jnt:virtualsite\"], multi: ANY})\n" +
                "      test5: isNodeType(type: {types: [\"wrongInput\"], multi: ANY})\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        JSONObject node = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath");

        Assert.assertEquals(true, node.getBoolean("test1"));
        Assert.assertEquals(true, node.getBoolean("test2"));
        Assert.assertEquals(false, node.getBoolean("test3"));
        Assert.assertEquals(true, node.getBoolean("test4"));
        Assert.assertEquals(false, node.getBoolean("test5"));
    }


    @Test
    public void shouldGetNodeDefinition() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr{\n" +
                "    nodeByPath(path:\"/testList\") {\n" +
                "      definition {\n" +
                "        declaringNodeType {\n" +
                "          name\n" +
                "        }\n" +
                "        name\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");

        JSONObject definition = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("definition");

        Assert.assertEquals("nt:unstructured", definition.getJSONObject("declaringNodeType").getString("name"));
        Assert.assertEquals("*", definition.getString("name"));
    }


    @Test
    public void shouldGetPropertyDefinition() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr {\n" +
                "    nodeByPath(path: \"/testList\") {\n" +
                "      property(name: \"jcr:created\") {\n" +
                "        definition {\n" +
                "          declaringNodeType {\n" +
                "            name\n" +
                "          }\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JSONObject definition = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("property").getJSONObject("definition");

        Assert.assertEquals("mix:created", definition.getJSONObject("declaringNodeType").getString("name"));
        Assert.assertEquals("jcr:created", definition.getString("name"));
    }

    @Test
    public void shouldRetrieveNodeType() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr {\n" +
                "    nodeTypeByName(name: \"jmix:editorialContent\") {\n" +
                "      name\n" +
                "      displayName(language: \"en\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JSONObject nodeType = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeTypeByName");

        Assert.assertEquals("jmix:editorialContent", nodeType.getString("name"));
        Assert.assertEquals("Editorial content", nodeType.getString("displayName"));
    }

    @Test
    public void shouldNotRetrieveNodeType() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr {\n" +
                "    nodeTypeByName(name: \"jmix:wrong\") {\n" +
                "      name\n" +
                "      displayName(language: \"en\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        validateError(result, "javax.jcr.nodetype.NoSuchNodeTypeException: Unknown type : jmix:wrong");
    }

    @Test
    public void shouldRetrieveNodeTypesForModule() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr {\n" +
                "    nodeTypes(filter:{modules:[\"default\"]}) {\n" +
                "      nodes {\n" +
                "        name\n" +
                "        systemId\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JSONArray nodeTypes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeTypes").getJSONArray("nodes");

        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodeTypes.length(); i++) {
            JSONObject nodeType = nodeTypes.getJSONObject(i);
            names.add(nodeType.getString("name"));
            Assert.assertEquals("default", nodeType.getString("systemId"));
        }

        Assert.assertTrue(names.contains("jnt:text"));
        Assert.assertFalse(names.contains("nt:base"));
    }

    @Test
    public void shouldNotRetrieveNodeTypesForModule() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr {\n" +
                "    nodeTypes(filter:{modules:[\"wrongModule\"]}) {\n" +
                "      nodes {\n" +
                "        name\n" +
                "        systemId\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JSONArray nodeTypes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeTypes").getJSONArray("nodes");
        Assert.assertEquals(nodeTypes.length(), 0);
    }

    @Test
    public void shouldRetrieveMixins() throws Exception {
        JSONObject result = executeQuery("{\n" +
                "  jcr {\n" +
                "    nodeTypes(filter:{includeNonMixins:false}) {\n" +
                "      nodes {\n" +
                "        name\n" +
                "        mixin\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JSONArray nodeTypes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeTypes").getJSONArray("nodes");

        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodeTypes.length(); i++) {
            JSONObject nodeType = nodeTypes.getJSONObject(i);
            names.add(nodeType.getString("name"));
            Assert.assertEquals(true, nodeType.getBoolean("mixin"));
        }

        Assert.assertTrue(names.contains("mix:created"));
        Assert.assertFalse(names.contains("nt:base"));
    }

    @Test
    public void shouldRetrieveIncludedAndNotExcludedNodeTypes() throws Exception {

        JSONObject result = executeQuery("{\n" +
            "  jcr {\n" +
            "    nodeTypes(filter: {includeMixins: false, siteKey: \"systemsite\", includeTypes: [\"jmix:editorialContent\"], excludeTypes: [\"jmix:studioOnly\", \"jmix:hiddenType\"]}) {\n" +
            "      nodes {\n" +
            "        name\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n");

        JSONArray nodeTypes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeTypes").getJSONArray("nodes");

        NodeTypeRegistry nodeTypeRegistry = NodeTypeRegistry.getInstance();
        Assert.assertEquals(11, nodeTypes.length());
        for (int i = 0; i < nodeTypes.length(); i++) {
            JSONObject nodeType = nodeTypes.getJSONObject(i);
            ExtendedNodeType nt = nodeTypeRegistry.getNodeType(nodeType.getString("name"));
            Assert.assertTrue(nt.isNodeType("jmix:editorialContent"));
            Assert.assertFalse(nt.isNodeType("jmix:studioOnly"));
            Assert.assertFalse(nt.isNodeType("jmix:hiddenType"));
        }

        Assert.assertEquals(11, nodeTypes.length());
    }
}
