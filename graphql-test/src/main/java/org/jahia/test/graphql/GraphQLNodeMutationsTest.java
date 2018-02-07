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
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GraphQLNodeMutationsTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();
    }

    @Before
    public void setup() throws Exception {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            JCRNodeWrapper subNode1 = node.addNode("testSubList1", "jnt:contentList");
            JCRNodeWrapper subNode2 = node.addNode("testSubList2", "jnt:contentList");
            session.save();
            return null;
        });
    }

    @After
    public void tearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
        JCRSessionFactory.getInstance().closeAllSessions();
    }

    @Test
    public void addNode() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testList\",name:\"testNew\",primaryNodeType:\"jnt:contentList\") {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String uuid = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getString("uuid");
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuid);
            Assert.assertEquals("/testList/testNew", node.getPath());
            Assert.assertTrue(node.isNodeType("jnt:contentList"));
            return null;
        });
    }

    @Test
    public void addNodesBatch() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNodesBatch(nodes: [\n" +
                "      {\n" +
                "        parentPathOrId: \"/testList\", \n" +
                "        name: \"testBatch1\", \n" +
                "        primaryNodeType: \"jnt:contentList\", \n" +
                "        children: [\n" +
                "          {\n" +
                "            name: \"subList1\", \n" +
                "            primaryNodeType: \"jnt:contentList\"\n" +
                "          }\n" +
                "        ],\n" +
                "        properties: [\n" +
                "          {\n" +
                "            name:\"jcr:title\",\n" +
                "            value:\"test\", \n" +
                "            language:\"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        mixins: [\"jmix:renderable\"]\n" +
                "      }, {\n" +
                "        parentPathOrId: \"/testList\", \n" +
                "        name: \"testBatch2\", \n" +
                "        primaryNodeType: \"jnt:contentList\"\n" +
                "      }\n" +
                "    ]) {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        JSONArray res = result.getJSONObject("data").getJSONObject("jcr").getJSONArray("addNodesBatch");
        String uuid1 = res.getJSONObject(0).getString("uuid");
        String uuid2 = res.getJSONObject(1).getString("uuid");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node1 = session.getNodeByIdentifier(uuid1);
            Assert.assertEquals("/testList/testBatch1", node1.getPath());
            Assert.assertTrue(node1.isNodeType("jnt:contentList"));
            Assert.assertTrue(node1.isNodeType("jmix:renderable"));
            Assert.assertEquals("test", node1.getProperty("jcr:title").getString());
            JCRNodeWrapper node2 = JCRSessionFactory.getInstance().getCurrentUserSession().getNodeByIdentifier(uuid2);
            Assert.assertEquals("/testList/testBatch2", node2.getPath());
            return null;
        });
    }

    @Test
    public void mutateNode() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId: \"/testList/testSubList1\") {\n" +
                "      mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"test\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertEquals("test", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            return null;
        });
    }

    @Test
    public void mutateNodes() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodes(pathsOrIds: [\"/testList/testSubList1\",\"/testList/testSubList2\"]) {\n" +
                "      mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"test\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertEquals("test", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            Assert.assertEquals("test", session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            return null;
        });
    }

    @Test
    public void mutateNodesByQuery() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodesByQuery(query:\"select * from [jnt:contentList] where isdescendantnode('/testList')\",queryLanguage:SQL2) {\n" +
                "      mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"test\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertEquals("test", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            Assert.assertEquals("test", session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            return null;
        });
    }

    @Test
    public void deleteNode() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    deleteNode(pathOrId:\"/testList/testSubList1\") \n" +
                "    mutateNode(pathOrId:\"/testList/testSubList2\") {\n" +
                "      delete\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertFalse(session.itemExists("/testList/testSubList1"));
            Assert.assertFalse(session.itemExists("/testList/testSubList2"));
            return null;
        });
    }

    @Test
    public void markDeleteUndeleteNode() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    deleteNode(pathOrId:\"/testList/testSubList1\",markForDeletion:true, markForDeletionComment:\"test delete\") \n" +
                "  }\n" +
                "}\n");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertTrue(session.itemExists("/testList/testSubList1"));
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            Assert.assertTrue(session.getNode("/testList/testSubList1").isMarkedForDeletion());
            Assert.assertEquals("test delete", node.getProperty(Constants.MARKED_FOR_DELETION_MESSAGE).getString());
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    undeleteNode(pathOrId:\"/testList/testSubList1\") \n" +
                "  }\n" +
                "}\n");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertTrue(session.itemExists("/testList/testSubList1"));
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            Assert.assertFalse(session.getNode("/testList/testSubList1").isMarkedForDeletion());
            return null;
        });
    }

    @Test
    public void addRemoveMixin() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      addMixins(mixins:[\"jmix:renderable\"])\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertTrue(session.getNode("/testList/testSubList1").isNodeType("jmix:renderable"));
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      removeMixins(mixins:[\"jmix:renderable\"])\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertFalse(session.getNode("/testList/testSubList1").isNodeType("jmix:renderable"));
            return null;
        });
    }

    @Test
    public void setPropertyMultiple() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      addMixins(mixins:[\"jmix:unstructured\"])\n" +
                "      mutateProperty(name:\"test\") {\n" +
                "        setValues(values:[\"val1\",\"val2\"])\n" +
                "        addValue(value:\"val3\")\n" +
                "        addValues(values:[\"val4\",\"val5\"])\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            Assert.assertTrue(node.hasProperty("test"));
            Assert.assertTrue(node.getProperty("test").isMultiple());
            Assert.assertEquals(Arrays.asList("val1","val2","val3","val4","val5"), getPropertyStringValues(node, "test"));
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      mutateProperty(name:\"test\") {\n" +
                "        removeValue(value:\"val3\")\n" +
                "        removeValues(values:[\"val4\", \"val5\"])\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            Assert.assertTrue(node.hasProperty("test"));
            Assert.assertTrue(node.getProperty("test").isMultiple());
            Assert.assertEquals(Arrays.asList("val1","val2"), getPropertyStringValues(node, "test"));
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      mutateProperty(name:\"test\") {\n" +
                "        delete\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            Assert.assertFalse(node.hasProperty("test"));
            return null;
        });
    }

    private List<String> getPropertyStringValues(JCRNodeWrapper node, String propertyName) throws RepositoryException {
        return Arrays.stream(node.getProperty(propertyName).getValues()).map(p -> {
                    try {
                        return p.getString();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    @Test
    public void addChild() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList\")  {\n" +
                "      addChild(name:\"testNew\",primaryNodeType:\"jnt:contentList\") {\n" +
                "        uuid\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "\n");
        String uuid = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONObject("addChild").getString("uuid");
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuid);
            Assert.assertEquals("/testList/testNew", node.getPath());
            Assert.assertTrue(node.isNodeType("jnt:contentList"));
            return null;
        });
    }


    @Test
    public void setPropertiesBatch() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      addMixins(mixins:[\"mix:title\",\"jmix:unstructured\"])\n" +
                "      setPropertiesBatch(properties:[\n" +
                "        {name:\"testPropString\", value:\"string\"}, \n" +
                "        {name:\"testPropLong\", value:\"123\", type:LONG}, \n" +
                "        {name:\"testPropMultiple\", values:[\"val1\",\"val2\"]},\n" +
                "        {name:\"jcr:title\", value:\"en\", language:\"en\"},\n" +
                "        {name:\"jcr:title\", value:\"fr\", language:\"fr\"},\n" +
                "      ]) {\n" +
                "        path\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");

        JSONArray array = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONArray("setPropertiesBatch");
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            Assert.assertEquals(false, node.getProperty("testPropString").isMultiple());
            Assert.assertEquals(PropertyType.STRING, node.getProperty("testPropString").getValue().getType());
            Assert.assertEquals("string", node.getProperty("testPropString").getValue().getString());

            Assert.assertEquals(false, node.getProperty("testPropLong").isMultiple());
            Assert.assertEquals(PropertyType.LONG, node.getProperty("testPropLong").getValue().getType());
            Assert.assertEquals(123, node.getProperty("testPropLong").getValue().getLong());

            Assert.assertEquals(true, node.getProperty("testPropMultiple").isMultiple());
            Assert.assertEquals(Arrays.asList("val1", "val2"), getPropertyStringValues(node, "testPropMultiple"));

            Assert.assertEquals(false, node.getProperty("jcr:title").isMultiple());
            Assert.assertEquals(PropertyType.STRING, node.getProperty("jcr:title").getValue().getType());
            Assert.assertEquals("en", node.getProperty("jcr:title").getValue().getString());

            return null;
        });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH, session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            Assert.assertEquals(false, node.getProperty("jcr:title").isMultiple());
            Assert.assertEquals(PropertyType.STRING, node.getProperty("jcr:title").getValue().getType());
            Assert.assertEquals("fr", node.getProperty("jcr:title").getValue().getString());
            return null;
        });
    }


    @Test
    public void moveAndRename() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId: \"/testList/testSubList1\") {\n" +
                "      move(parentPathOrId: \"/testList/testSubList2\")\n" +
                "      rename(name: \"testRenamed\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertFalse(session.itemExists("/testList/testSubList1"));
            Assert.assertTrue(session.itemExists("/testList/testSubList2/testRenamed"));
            return null;
        });
    }

}
