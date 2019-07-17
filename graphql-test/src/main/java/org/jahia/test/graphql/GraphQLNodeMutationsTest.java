/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation.ReorderedChildrenPosition;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.jahia.settings.readonlymode.ReadOnlyModeController;
import org.jahia.settings.readonlymode.ReadOnlyModeController.ReadOnlyModeStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Integration test for GraphQL mutations.
 */
public class GraphQLNodeMutationsTest extends GraphQLTestSupport {

    private static void addText(JCRNodeWrapper parent, String name, String text, String nodeType)
            throws RepositoryException {
        JCRNodeWrapper textNode = parent.addNode(name, StringUtils.defaultString(nodeType, "jnt:text"));
        textNode.setProperty("text", text);
    }

    private static <T> T inJcr(JCRCallback<T> callback) throws Exception {
        return inJcr(callback, null);
    }

    private static <T> T inJcr(JCRCallback<T> callback, Locale locale) throws Exception {
        return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE,
                locale != null ? locale : Locale.ENGLISH, callback);
    }

    private void enableFullReadOnlyMode() {
        ReadOnlyModeController.getInstance().switchReadOnlyMode(true);
    }

    private void disableReadOnlyMode() {
        ReadOnlyModeController.getInstance().switchReadOnlyMode(false);
    }

    private ReadOnlyModeStatus getReadOnlyModeStatus() {
        return ReadOnlyModeController.getInstance().getReadOnlyStatus();
    }

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();
    }

    @Before
    public void setup() throws Exception {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getRootNode().addNode("testList", "jnt:contentList");
            node.addNode("testSubList1", "jnt:contentList");
            node.addNode("testSubList2", "jnt:contentList");
            node.addNode("testSubList3", "jnt:contentList");
            node.addNode("testNode", "jnt:bigText");
            session.getRootNode().addNode("testFolder", "jnt:folder");
            session.save();
            return null;
        });
    }

    @After
    public void tearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
        inJcr(session -> {
            if (session.nodeExists("/testFolder")) {
                session.getNode("/testFolder").remove();
                session.save();
            }
            return null;
        });
        JCRSessionFactory.getInstance().closeAllSessions();
    }

    @Test
    public void addNode() throws Exception {
        // add simple node
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testList\",name:\"testNew1\",primaryNodeType:\"jnt:contentList\") {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String uuid = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getString("uuid");
        inJcr(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuid);
            assertEquals("/testList/testNew1", node.getPath());
            assertTrue(node.isNodeType("jnt:contentList"));
            return null;
        });

        // add node with mixins
        result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testList\",name:\"testNew2\",primaryNodeType:\"jnt:contentList\", mixins: [\"jmix:keywords\", \"jmix:cache\"]) {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String uuidWithMixins = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getString("uuid");
        inJcr(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuidWithMixins);
            assertEquals("/testList/testNew2", node.getPath());
            assertTrue(node.isNodeType("jnt:contentList"));
            assertTrue(node.isNodeType("jmix:keywords"));
            assertTrue(node.isNodeType("jmix:cache"));
            return null;
        });

        // add node with mixins, properties and child nodes
        result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId: \"/testList\", name: \"testNew3\", primaryNodeType: \"jnt:contentList\", mixins: [\"jmix:keywords\", \"jmix:cache\"], \n" +
                "      children: [\n" +
                "        {name: \"text1\", primaryNodeType: \"jnt:text\", \n" +
                "          properties: [{language: \"en\", name: \"text\", value: \"English text 111\"}, {language: \"de\", name: \"text\", value: \"Deutsch Text 111\"}]\n" +
                "        },\n" +
                "        {name: \"text2\", primaryNodeType: \"jnt:text\", \n" +
                "          properties: [{language: \"en\", name: \"text\", value: \"English text 222\"}, {language: \"de\", name: \"text\", value: \"Deutsch Text 222\"}]\n" +
                "        },\n" +
                "      ],\n" +
                "      properties: [\n" +
                "        {name: \"j:expiration\", value: \"60000\"},\n" +
                "        {name: \"j:keywords\", values: [\"keyword1\", \"keyword2\"]},\n" +
                "        {name: \"jcr:title\", value: \"List title English\", language: \"en\"},\n" +
                "        {name: \"jcr:title\", value: \"Listentitel Deutsch\", language: \"de\"}\n" +
                "      ]\n" +
                "    ) {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String uuidWithEverything = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getString("uuid");
        JCRCallback<Object> callback = session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuidWithEverything);
            assertEquals("/testList/testNew3", node.getPath());
            assertTrue(node.isNodeType("jnt:contentList"));

            // mixins
            assertTrue(node.isNodeType("jmix:keywords"));
            assertTrue(node.isNodeType("jmix:cache"));

            // children
            assertTrue(node.hasNode("text1"));
            assertTrue(node.hasNode("text2"));

            boolean isEnglish = session.getLocale().equals(Locale.ENGLISH);

            // properties
            assertTrue(node.hasProperty("j:expiration"));
            assertEquals(60000, node.getProperty("j:expiration").getLong());
            assertTrue(node.hasProperty("j:keywords"));
            assertEquals(2, node.getProperty("j:keywords").getValues().length);
            assertEquals("keyword1 keyword2", node.getPropertyAsString("j:keywords"));
            assertEquals(isEnglish ? "List title English" : "Listentitel Deutsch",
                    node.getProperty("jcr:title").getString());

            // i18n properties on child nodes
            assertTrue(node.getNode("text1").hasProperty("text"));
            assertEquals(isEnglish ? "English text 111" : "Deutsch Text 111",
                    node.getNode("text1").getProperty("text").getString());
            assertTrue(node.getNode("text2").hasProperty("text"));
            assertEquals(isEnglish ? "English text 222" : "Deutsch Text 222",
                    node.getNode("text2").getProperty("text").getString());
            return null;
        };
        // test in English
        inJcr(callback, Locale.ENGLISH);
        // test in German
        inJcr(callback, Locale.GERMAN);
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
                "            name: \"text1\", \n" +
                "            primaryNodeType: \"jnt:text\"\n" +
                "          }\n" +
                "        ],\n" +
                "        properties: [\n" +
                "          {\n" +
                "            name:\"jcr:title\",\n" +
                "            value:\"test\", \n" +
                "            language:\"en\"\n" +
                "          },\n" +
                "          {\n" +
                "            name:\"jcr:title\",\n" +
                "            value:\"test Deutsch\", \n" +
                "            language:\"de\"\n" +
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

        inJcr(session -> {
            JCRNodeWrapper node1 = session.getNodeByIdentifier(uuid1);
            assertEquals("/testList/testBatch1", node1.getPath());
            assertTrue(node1.isNodeType("jnt:contentList"));
            assertTrue(node1.isNodeType("jmix:renderable"));
            assertEquals("test", node1.getProperty("jcr:title").getString());
            assertTrue(node1.hasNode("text1"));
            assertTrue(node1.getNode("text1").isNodeType("jnt:text"));

            JCRNodeWrapper node2 = session.getNodeByIdentifier(uuid2);
            assertEquals("/testList/testBatch2", node2.getPath());
            assertTrue(node2.isNodeType("jnt:contentList"));
            return null;
        });
        inJcr(session -> {
            JCRNodeWrapper node1 = session.getNodeByIdentifier(uuid1);
            assertEquals("test Deutsch", node1.getProperty("jcr:title").getString());
            return null;
        }, Locale.GERMAN);
    }

    @Test
    public void mutateProperty() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId: \"/testList/testSubList1\") {\n" +
                "      addMixins(mixins:[\"jmix:renderable\", \"jmix:cache\"])\n" +
                "      mut1: mutateProperty(name: \"jcr:title\") {\n" +
                "        setValueInEN: setValue(language: \"en\", value: \"test title\")\n" +
                "        setValueInDE: setValue(language: \"de\", value: \"Test Titel\")\n" +
                "      }\n" +
                "      mut2: mutateProperty(name: \"j:view\") {\n" +
                "        setValue(value: \"my-view\")\n" +
                "      }\n" +
                "      mut3: mutateProperty(name: \"j:expiration\") {\n" +
                "        setValue(value: \"60000\")\n" +
                "      }\n" +
                "      mut4: mutateProperty(name: \"j:perUser\") {\n" +
                "        setValue(value: \"true\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertEquals("test title", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals("my-view", session.getNode("/testList/testSubList1").getProperty("j:view").getString());
            assertEquals(60000, session.getNode("/testList/testSubList1").getProperty("j:expiration").getLong());
            assertEquals(true, session.getNode("/testList/testSubList1").getProperty("j:perUser").getBoolean());
            return null;
        });
        inJcr(session -> {
            assertEquals("Test Titel", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            return null;
        }, Locale.GERMAN);

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId: \"/testList/testSubList1\") {\n" +
                "      mut1: mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"test title 2\")\n" +
                "      }\n" +
                "      mut2: mutateProperty(name: \"j:expiration\") {\n" +
                "        setValue(value: \"30000\")\n" +
                "      }\n" +
                "      mut3: mutateProperty(name: \"j:perUser\") {\n" +
                "        delete\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertEquals("test title 2", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals("my-view", session.getNode("/testList/testSubList1").getProperty("j:view").getString());
            assertEquals(30000, session.getNode("/testList/testSubList1").getProperty("j:expiration").getLong());
            assertFalse(session.getNode("/testList/testSubList1").hasProperty("j:perUser"));
            return null;
        });
    }

    @Test
    public void mutatePropertyWithNotZonedDateValue() throws Exception {
        String dateValue = "2019-07-14T21:07:25.000";

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId: \"/\", name: \"testNodeNotZonedDate\", primaryNodeType: \"nt:unstructured\") {\n" +
                "      mutateProperty(name: \"date\") {\n" +
                "        setValue(language: \"en\", type: DATE, value: \"" + dateValue + "\", notZonedDateValue: \"" + dateValue + "\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Date date = defaultDateFormat.parse(dateValue);

        inJcr(session -> {
            assertEquals(simpleDateFormat.format(date), session.getNode("/testNodeNotZonedDate").getProperty("date").getString());
            return null;
        });

        inJcr(session -> {
            session.getNode("/testNodeNotZonedDate").remove();
            session.save();
            return null;
        });
    }

    @Test
    public void queryPropertyWithNotZonedDateValue() throws Exception {
        String dateValue = "2019-07-14T21:07:25.000";

        SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Date date = defaultDateFormat.parse(dateValue);

        inJcr(session -> {
            JCRNodeWrapper node = session.getRootNode().addNode("testNodeNotZonedDate");
            node.setProperty("date", simpleDateFormat.format(date));
            session.save();
            return null;
        });

        JSONObject result = executeQuery("query {\n" +
                "  jcr {\n" +
                "    nodeByPath(path: \"/testNodeNotZonedDate\") {\n" +
                "      property(name: \"date\") {\n" +
                "        value\n" +
                "        notZonedDateValue\n" +
                "        notZonedDateValues\n" +
                "        values\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");

        String propertyNotZonedDateValue = result
                .getJSONObject("data")
                .getJSONObject("jcr")
                .getJSONObject("nodeByPath")
                .getJSONObject("property")
                .getString("notZonedDateValue");

        assertEquals(dateValue, propertyNotZonedDateValue);

        inJcr(session -> {
            session.getNode("/testNodeNotZonedDate").remove();
            session.save();
            return null;
        });
    }

    @Test
    public void mutatePropertyMultipleNodes() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodes(pathsOrIds: [\"/testList/testSubList1\",\"/testList/testSubList2\"]) {\n" +
                "      mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"test\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("test", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals("test", session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            return null;
        });
    }

    @Test
    public void mutateNodesByQuery() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodesByQuery(query:\"select * from [jnt:contentList] where isdescendantnode('/testList')\",queryLanguage:SQL2) {\n" +
                "      mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"test1\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("test1", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals("test1", session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            assertEquals("test1", session.getNode("/testList/testSubList3").getProperty("jcr:title").getString());
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodesByQuery(query:\"select * from [jnt:contentList] where isdescendantnode('/testList') order by localname()\",limit:1) {\n" +
                "      mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"test2\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("test2", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals("test1", session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            assertEquals("test1", session.getNode("/testList/testSubList3").getProperty("jcr:title").getString());
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodesByQuery(query:\"select * from [jnt:contentList] where isdescendantnode('/testList') order by localname()\",limit:1, offset:1) {\n" +
                "      mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"test3\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("test2", session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals("test3", session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            assertEquals("test1", session.getNode("/testList/testSubList3").getProperty("jcr:title").getString());
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodesByQuery(query:\"select * from [jnt:contentList] where isdescendantnode('/testList')\",queryLanguage:SQL2) {\n" +
                "      delete\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertFalse(session.nodeExists("/testList/testSubList1"));
            assertFalse(session.nodeExists("/testList/testSubList2"));
            assertFalse(session.nodeExists("/testList/testSubList3"));
            return null;
        });
    }

    @Test
    public void deleteNode() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    deleteNode(pathOrId:\"/testList/testSubList1\") \n" +
                "    mutateNode(pathOrId:\"/testList/testSubList2\") {\n" +
                "      delete\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertFalse(session.itemExists("/testList/testSubList1"));
            assertFalse(session.itemExists("/testList/testSubList2"));
            return null;
        });
    }

    @Test
    public void markUnmarkNodeForDeletion() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    markNodeForDeletion(pathOrId:\"/testList/testSubList1\",comment:\"test delete\") \n" +
                "    mutateNode(pathOrId:\"/testList/testSubList2\") {\n" +
                "      markForDeletion(comment: \"test delete 2\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertTrue(session.itemExists("/testList/testSubList1"));
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            assertTrue(node.isMarkedForDeletion());
            assertEquals("test delete", node.getProperty(Constants.MARKED_FOR_DELETION_MESSAGE).getString());

            assertTrue(session.itemExists("/testList/testSubList2"));
            JCRNodeWrapper node2 = session.getNode("/testList/testSubList2");
            assertTrue(node2.isMarkedForDeletion());
            assertEquals("test delete 2", node2.getProperty(Constants.MARKED_FOR_DELETION_MESSAGE).getString());
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    unmarkNodeForDeletion(pathOrId:\"/testList/testSubList1\") \n" +
                "    mutateNode(pathOrId:\"/testList/testSubList2\") {\n" +
                "      unmarkForDeletion\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertTrue(session.itemExists("/testList/testSubList1"));
            assertFalse(session.getNode("/testList/testSubList1").isMarkedForDeletion());
            assertTrue(session.itemExists("/testList/testSubList2"));
            assertFalse(session.getNode("/testList/testSubList2").isMarkedForDeletion());
            return null;
        });
    }

    @Test
    public void addRemoveMixin() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      addMixins(mixins:[\"jmix:renderable\", \"jmix:cache\"])\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertTrue(session.getNode("/testList/testSubList1").isNodeType("jmix:renderable"));
            assertTrue(session.getNode("/testList/testSubList1").isNodeType("jmix:cache"));
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      removeMixins(mixins:[\"jmix:renderable\"])\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertFalse(session.getNode("/testList/testSubList1").isNodeType("jmix:renderable"));
            assertTrue(session.getNode("/testList/testSubList1").isNodeType("jmix:cache"));
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      removeMixins(mixins:[\"jmix:cache\"])\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertFalse(session.getNode("/testList/testSubList1").isNodeType("jmix:renderable"));
            assertFalse(session.getNode("/testList/testSubList1").isNodeType("jmix:cache"));
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

        inJcr(session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            assertTrue(node.hasProperty("test"));
            assertTrue(node.getProperty("test").isMultiple());
            assertEquals(Arrays.asList("val1", "val2", "val3", "val4", "val5"), getPropertyStringValues(node, "test"));
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\")  {\n" +
                "      mutateProperty(name:\"test\") {\n" +
                "        removeValue(value:\"val1\")\n" +
                "        removeValues(values:[\"val3\", \"val4\"])\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            assertTrue(node.hasProperty("test"));
            assertTrue(node.getProperty("test").isMultiple());
            assertEquals(Arrays.asList("val2","val5"), getPropertyStringValues(node, "test"));
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

        inJcr(session -> {
            assertFalse(session.getNode("/testList/testSubList1").hasProperty("test"));
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
        // add simple node
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList\")  {\n" +
                "      addChild(name:\"testNew1\",primaryNodeType:\"jnt:contentList\") {\n" +
                "        uuid\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String uuid = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONObject("addChild").getString("uuid");
        inJcr(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuid);
            assertEquals("/testList/testNew1", node.getPath());
            assertTrue(node.isNodeType("jnt:contentList"));
            return null;
        });

        // add node with mixins
        result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList\")  {\n" +
                "      addChild(name:\"testNew2\",primaryNodeType:\"jnt:contentList\", mixins: [\"jmix:keywords\", \"jmix:cache\"]) {\n" +
                "        uuid\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String uuidWithMixins = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONObject("addChild").getString("uuid");
        inJcr(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuidWithMixins);
            assertEquals("/testList/testNew2", node.getPath());
            assertTrue(node.isNodeType("jnt:contentList"));
            assertTrue(node.isNodeType("jmix:keywords"));
            assertTrue(node.isNodeType("jmix:cache"));
            return null;
        });

        // add node with mixins, properties and child nodes
        result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList\")  {\n" +
                "      addChild(name: \"testNew3\", primaryNodeType: \"jnt:contentList\", mixins: [\"jmix:keywords\", \"jmix:cache\"], \n" +
                "        children: [\n" +
                "          {name: \"text1\", primaryNodeType: \"jnt:text\", \n" +
                "            properties: [{language: \"en\", name: \"text\", value: \"English text 111\"}, {language: \"de\", name: \"text\", value: \"Deutsch Text 111\"}]\n" +
                "          },\n" +
                "          {name: \"text2\", primaryNodeType: \"jnt:text\", \n" +
                "            properties: [{language: \"en\", name: \"text\", value: \"English text 222\"}, {language: \"de\", name: \"text\", value: \"Deutsch Text 222\"}]\n" +
                "          },\n" +
                "        ],\n" +
                "        properties: [\n" +
                "          {name: \"j:expiration\", value: \"60000\"},\n" +
                "          {name: \"j:keywords\", values: [\"keyword1\", \"keyword2\"]},\n" +
                "          {name: \"jcr:title\", value: \"List title English\", language: \"en\"},\n" +
                "          {name: \"jcr:title\", value: \"Listentitel Deutsch\", language: \"de\"}\n" +
                "        ]\n" +
                "      ) {\n" +
                "        uuid\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String uuidWithEverything = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONObject("addChild").getString("uuid");
        JCRCallback<Object> callback = session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuidWithEverything);
            assertEquals("/testList/testNew3", node.getPath());
            assertTrue(node.isNodeType("jnt:contentList"));

            // mixins
            assertTrue(node.isNodeType("jmix:keywords"));
            assertTrue(node.isNodeType("jmix:cache"));

            // children
            assertTrue(node.hasNode("text1"));
            assertTrue(node.hasNode("text2"));

            boolean isEnglish = session.getLocale().equals(Locale.ENGLISH);

            // properties
            assertTrue(node.hasProperty("j:expiration"));
            assertEquals(60000, node.getProperty("j:expiration").getLong());
            assertTrue(node.hasProperty("j:keywords"));
            assertEquals(2, node.getProperty("j:keywords").getValues().length);
            assertEquals("keyword1 keyword2", node.getPropertyAsString("j:keywords"));
            assertEquals(isEnglish ? "List title English" : "Listentitel Deutsch",
                    node.getProperty("jcr:title").getString());

            // i18n properties on child nodes
            assertTrue(node.getNode("text1").hasProperty("text"));
            assertEquals(isEnglish ? "English text 111" : "Deutsch Text 111",
                    node.getNode("text1").getProperty("text").getString());
            assertTrue(node.getNode("text2").hasProperty("text"));
            assertEquals(isEnglish ? "English text 222" : "Deutsch Text 222",
                    node.getNode("text2").getProperty("text").getString());
            return null;
        };
        // test in English
        inJcr(callback, Locale.ENGLISH);
        // test in German
        inJcr(callback, Locale.GERMAN);
    }

    @Test
    public void addChildrenBatch() throws Exception {
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList\")  {\n" +
                "    addChildrenBatch(nodes: [\n" +
                "      {\n" +
                "        name: \"testBatch1\", \n" +
                "        primaryNodeType: \"jnt:contentList\", \n" +
                "        children: [\n" +
                "          {\n" +
                "            name: \"text1\", \n" +
                "            primaryNodeType: \"jnt:text\"\n" +
                "          }\n" +
                "        ],\n" +
                "        properties: [\n" +
                "          {\n" +
                "            name:\"jcr:title\",\n" +
                "            value:\"test\", \n" +
                "            language:\"en\"\n" +
                "          },\n" +
                "          {\n" +
                "            name:\"jcr:title\",\n" +
                "            value:\"test Deutsch\", \n" +
                "            language:\"de\"\n" +
                "          }\n" +
                "        ],\n" +
                "        mixins: [\"jmix:renderable\"]\n" +
                "      }, {\n" +
                "        name: \"testBatch2\", \n" +
                "        primaryNodeType: \"jnt:contentList\"\n" +
                "      }\n" +
                "    ]) {\n" +
                "      uuid\n" +
                "    }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        JSONArray res = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONArray("addChildrenBatch");
        String uuid1 = res.getJSONObject(0).getString("uuid");
        String uuid2 = res.getJSONObject(1).getString("uuid");

        inJcr(session -> {
            JCRNodeWrapper node1 = session.getNodeByIdentifier(uuid1);
            assertEquals("/testList/testBatch1", node1.getPath());
            assertTrue(node1.isNodeType("jnt:contentList"));
            assertTrue(node1.isNodeType("jmix:renderable"));
            assertEquals("test", node1.getProperty("jcr:title").getString());
            assertTrue(node1.hasNode("text1"));
            assertTrue(node1.getNode("text1").isNodeType("jnt:text"));

            JCRNodeWrapper node2 = session.getNodeByIdentifier(uuid2);
            assertEquals("/testList/testBatch2", node2.getPath());
            assertTrue(node2.isNodeType("jnt:contentList"));
            return null;
        });
        inJcr(session -> {
            JCRNodeWrapper node1 = session.getNodeByIdentifier(uuid1);
            assertEquals("test Deutsch", node1.getProperty("jcr:title").getString());
            return null;
        }, Locale.GERMAN);
    }

    @Test
    public void setPropertiesBatch() throws Exception {
        executeQuery("mutation {\n" +
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

        inJcr(session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            assertEquals(false, node.getProperty("testPropString").isMultiple());
            assertEquals(PropertyType.STRING, node.getProperty("testPropString").getValue().getType());
            assertEquals("string", node.getProperty("testPropString").getValue().getString());

            assertEquals(false, node.getProperty("testPropLong").isMultiple());
            assertEquals(PropertyType.LONG, node.getProperty("testPropLong").getValue().getType());
            assertEquals(123, node.getProperty("testPropLong").getValue().getLong());

            assertEquals(true, node.getProperty("testPropMultiple").isMultiple());
            assertEquals(Arrays.asList("val1", "val2"), getPropertyStringValues(node, "testPropMultiple"));

            assertEquals(false, node.getProperty("jcr:title").isMultiple());
            assertEquals(PropertyType.STRING, node.getProperty("jcr:title").getValue().getType());
            assertEquals("en", node.getProperty("jcr:title").getValue().getString());

            return null;
        });

        inJcr(session -> {
            JCRNodeWrapper node = session.getNode("/testList/testSubList1");
            assertEquals(false, node.getProperty("jcr:title").isMultiple());
            assertEquals(PropertyType.STRING, node.getProperty("jcr:title").getValue().getType());
            assertEquals("fr", node.getProperty("jcr:title").getValue().getString());
            return null;
        }, Locale.FRENCH);
    }

    @Test
    public void moveAndRename() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId: \"/testList/testSubList1\") {\n" +
                "      rename(name: \"testRenamed\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertFalse(session.itemExists("/testList/testSubList1"));
            assertTrue(session.itemExists("/testList/testRenamed"));
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId: \"/testList/testRenamed\") {\n" +
                "      move(parentPathOrId: \"/testList/testSubList2\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertFalse(session.itemExists("/testList/testRenamed"));
            assertTrue(session.itemExists("/testList/testSubList2/testRenamed"));
            return null;
        });

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId: \"/testList/testSubList2/testRenamed\") {\n" +
                "      move(destPath: \"/testList/testRenamedNew\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertFalse(session.itemExists("/testList/testSubList2/testRenamed"));
            assertTrue(session.itemExists("/testList/testRenamedNew"));
            return null;
        });
    }

    @Test
    public void shouldSetWeakReferencePropertyByPath() throws Exception{
        JSONObject  result = executeQuery("mutation {\n"
                + " jcr {\n"
                + "     addNode(parentPathOrId:\"/testList/testSubList3\", name:\"referenceNode\", "
                + "primaryNodeType:\"jnt:contentReference\")"
                + "{\n      mutateProperty(name:\"j:node\"){ \n"
                + "     setValue(language:\"en\", value:\"/testList/testNode\")\n"
                + "             }\n "
                + "     node {\n"
                + "         property(name:\"j:node\"){ \n"
                + "                 value\n"
                + "             }\n"
                + "         }\n"
                + "       }\n"
                + "     }\n"
                + " }\n");

        String uuid = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getJSONObject("node")
                .getJSONObject("property").getString("value");

        inJcr(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(uuid);
            assertEquals("/testList/testNode", node.getPath());
            assertTrue(node.isNodeType("jnt:bigText"));

            node = session.getNode("/testList/testSubList3/referenceNode");
            assertTrue(node.hasProperty("j:node"));
            assertEquals(PropertyType.WEAKREFERENCE, node.getProperty("j:node").getType());
            assertEquals(session.getNode("/testList/testNode").getIdentifier(), node.getProperty("j:node").getString());
            assertEquals("/testList/testNode", node.getProperty("j:node").getNode().getPath());
            return null;
        });
    }


    @Test
    public void propertyBinaryValue() throws Exception{
        Map<String,List<FileItem>> files = new HashMap<>();
        DiskFileItem diskFileItem = new DiskFileItem("", "text/plain", false, "test.txt", 100, null);
        OutputStream outputStream = diskFileItem.getOutputStream();
        IOUtils.write("test text", outputStream);
        outputStream.close();
        files.put("test-binary", Collections.singletonList(diskFileItem));
        JSONObject result = executeQueryWithFiles("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testFolder\", name:\"file.txt\", primaryNodeType:\"jnt:file\") {\n" +
                "      addChild(name:\"jcr:content\", primaryNodeType:\"nt:resource\") {\n" +
                "        setData:mutateProperty(name:\"jcr:data\") {\n" +
                "          setValue(value:\"test-binary\")\n" +
                "        }\n" +
                "        setMimeType:mutateProperty(name:\"jcr:mimeType\") {\n" +
                "          setValue(value:\"text/plain\")\n" +
                "        }\n" +
                "        node {\n" +
                "          property(name:\"jcr:data\") {\n" +
                "            value\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", files);

        String value = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getJSONObject("addChild").getJSONObject("node")
                .getJSONObject("property").getString("value");

        assertEquals("test text", value);

        // test binary property by providing its value as string
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testFolder\", name:\"file2.txt\", primaryNodeType:\"jnt:file\") {\n" +
                "      addChild(name:\"jcr:content\", primaryNodeType:\"nt:resource\") {\n" +
                "        setData:mutateProperty(name:\"jcr:data\") {\n" +
                "          setValue(value:\"my text binary value\")\n" +
                "        }\n" +
                "        setMimeType:mutateProperty(name:\"jcr:mimeType\") {\n" +
                "          setValue(value:\"text/plain\")\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertTrue(session.nodeExists("/testFolder/file2.txt"));
            JCRNodeWrapper fileNode = session.getNode("/testFolder/file2.txt");
            assertTrue(fileNode.isNodeType(Constants.JAHIANT_FILE));
            assertEquals("text/plain", fileNode.getFileContent().getContentType());

            try {
                assertEquals("my text binary value", IOUtils.toString(fileNode.getFileContent().downloadFile()));
            } catch (IOException e) {
                fail(e.getMessage());
            }
            return null;
        });
    }

    @Test
    public void mutateChildren() throws Exception {
        setupTextNodes();

        // mutate children by name
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\") {\n" +
                "      mutateChildren(names: [\"text2\", \"bigText2\"]) {\n" +
                "        mutateProperty(name: \"text\") {\n" +
                "          setValue(language: \"en\", value: \"value2\")\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("value1", session.getNode("/testList/testSubList1/text1").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText1").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/bigText2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText2").getProperty("text").getString());

            return null;
        });

        // mutate children by type
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\") {\n" +
                "      mutateChildren(typesFilter: {types: [\"jnt:text\"]}) {\n" +
                "        mutateProperty(name: \"text\") {\n" +
                "          setValue(language: \"en\", value: \"value3\")\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("value3", session.getNode("/testList/testSubList1/text1").getProperty("text").getString());
            assertEquals("value3", session.getNode("/testList/testSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText1").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/bigText2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText2").getProperty("text").getString());

            // reset the modified value
            session.getNode("/testList/testSubList1/text1").setProperty("text", "value1");
            session.save();
            return null;
        });

        // mutate children by type and property value
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\") {\n" +
                "      mutateChildren(typesFilter: {types: [\"jnt:text\"]}, propertiesFilter: {filters: [{property: \"text\" language: \"en\" value: \"value3\"}]}) {\n" +
                "        mutateProperty(name: \"text\") {\n" +
                "          setValue(language: \"en\", value: \"value4\")\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("value1", session.getNode("/testList/testSubList1/text1").getProperty("text").getString());
            assertEquals("value4", session.getNode("/testList/testSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText1").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/bigText2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText2").getProperty("text").getString());

            return null;
        });
    }

    @Test
    public void mutateDescendant() throws Exception {
        setupTextNodes();

        // mutate child
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\") {\n" +
                "      mutateDescendant(relPath: \"text2\") {\n" +
                "        mutateProperty(name: \"text\") {\n" +
                "          setValue(language: \"en\", value: \"value2\")\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("value1", session.getNode("/testList/testSubList1/text1").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText2").getProperty("text").getString());

            // reset the modified value
            session.getNode("/testList/testSubList1/text2").setProperty("text", "value1");
            session.save();

            return null;
        });

        // mutate descendant
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\") {\n" +
                "      mutateDescendant(relPath: \"testSubSubList1/text2\") {\n" +
                "        mutateProperty(name: \"text\") {\n" +
                "          setValue(language: \"en\", value: \"value2\")\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("value1", session.getNode("/testList/testSubList1/text1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text1").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/testSubSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText2").getProperty("text").getString());

            return null;
        });
    }

    @Test
    public void mutateDescendants() throws Exception {
        setupTextNodes();

        // mutate descendants by type
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\") {\n" +
                "      mutateDescendants(typesFilter: {types: [\"jnt:text\"]}) {\n" +
                "        mutateProperty(name: \"text\") {\n" +
                "          setValue(language: \"en\", value: \"value2\")\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("value2", session.getNode("/testList/testSubList1/text1").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText2").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/testSubSubList1/text1").getProperty("text").getString());
            assertEquals("value2", session.getNode("/testList/testSubList1/testSubSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText2").getProperty("text").getString());

            // reset the modified value
            session.getNode("/testList/testSubList1/text1").setProperty("text", "value1");
            session.getNode("/testList/testSubList1/testSubSubList1/text1").setProperty("text", "value1");
            session.save();

            return null;
        });

        // mutate descendants by type and property value
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNode(pathOrId:\"/testList/testSubList1\") {\n" +
                "      mutateDescendants(typesFilter: {types: [\"jnt:text\"]}, propertiesFilter: {filters: [{property: \"text\" language: \"en\" value: \"value2\"}]}) {\n" +
                "        mutateProperty(name: \"text\") {\n" +
                "          setValue(language: \"en\", value: \"value3\")\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        inJcr(session -> {
            assertEquals("value1", session.getNode("/testList/testSubList1/text1").getProperty("text").getString());
            assertEquals("value3", session.getNode("/testList/testSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/bigText2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/text1").getProperty("text").getString());
            assertEquals("value3", session.getNode("/testList/testSubList1/testSubSubList1/text2").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText1").getProperty("text").getString());
            assertEquals("value1", session.getNode("/testList/testSubList1/testSubSubList1/bigText2").getProperty("text").getString());

            return null;
        });
    }

    private static String buildReorderChildNodesQuery(String... childNames) {
        return buildReorderChildNodesQuery(null, childNames);
    }

    private static String buildReorderChildNodesQuery(ReorderedChildrenPosition position, String... childNames) {
        StringBuilder query = new StringBuilder();
        query.append(
            "mutation {\n" +
            "  jcr {\n" +
            "    mutateNode(pathOrId:\"/testList\") {\n" +
            "      reorderChildren("
        );
        if (position != null) {
            query.append("position: ").append(position).append(",");
        }
        query.append("names: [");
        for (int i = 0; i < childNames.length; i++) {
            if (i != 0) {
                query.append(", ");
            }
            query.append('"').append(childNames[i]).append('"');
        }
        query.append("])\n" +
            "    }\n" +
            "  }\n" +
            "}\n");
        return query.toString();
    }

    private static List<String> getActualOrderedChildNames() throws Exception {
        LinkedList<String> orderedChildNodes = new LinkedList<>();
        inJcr(session -> {
            JCRNodeWrapper testList = session.getNode("/testList");
            JCRNodeIteratorWrapper children = testList.getNodes();
            while (children.hasNext()) {
                JCRNodeWrapper child = (JCRNodeWrapper) children.next();
                orderedChildNodes.add(child.getName());
            }
            return null;
        });
        return orderedChildNodes;
    }

    private static void validateChildNodesOrder(String... expectedOrderedChildNames) throws Exception {
        validateChildNodesOrder(Arrays.asList(expectedOrderedChildNames));
    }

    private static void validateChildNodesOrder(List<String> expectedOrderedChildNames) throws Exception {
        assertEquals(expectedOrderedChildNames, getActualOrderedChildNames());
    }

    private void setupTextNodes() throws Exception {
        inJcr(session -> {
            JCRNodeWrapper subList = session.getNode("/testList/testSubList1");
            addText(subList, "text1", "value1", "jnt:text");
            addText(subList, "text2", "value1", "jnt:text");
            addText(subList, "bigText1", "value1", "jnt:bigText");
            addText(subList, "bigText2", "value1", "jnt:bigText");
            subList = subList.addNode("testSubSubList1", "jnt:contentList");
            addText(subList, "text1", "value1", "jnt:text");
            addText(subList, "text2", "value1", "jnt:text");
            addText(subList, "bigText1", "value1", "jnt:bigText");
            addText(subList, "bigText2", "value1", "jnt:bigText");
            session.save();
            return null;
        });
    }

    @Test
    public void reorderChildrenWrongInputException() throws Exception {
        List<String> orderedChildNames = getActualOrderedChildNames();
        JSONObject result = executeQuery(buildReorderChildNodesQuery());
        validateError(result, "A non-empty list of child node names is expected");
        validateChildNodesOrder(orderedChildNames);

        result = executeQuery(buildReorderChildNodesQuery("testSubList1"));
        validateError(result, "Reorder operation expects at least two names in case target position is inplace");
        validateChildNodesOrder(orderedChildNames);

        result = executeQuery(buildReorderChildNodesQuery("", null));
        validateError(result, "Null or empty child names are not permitted");
        validateChildNodesOrder(orderedChildNames);

        result = executeQuery(buildReorderChildNodesQuery("testSubList1", "testSubList", "testSubList1"));
        validateError(result,
                "Ambigous child name order: duplicates are not expected in the list of passed child node names to reorder");
        validateChildNodesOrder(orderedChildNames);
    }

    @Test
    public void reorderChildrenNonExistingChild() throws Exception {
        List<String> orderedChildNames = getActualOrderedChildNames();
        JSONObject result = executeQuery(
                buildReorderChildNodesQuery("testSubList4", "testNode", "testSubList2", "testSubList1"));
        validateError(result,
                "javax.jcr.ItemNotFoundException: node /testList has no child node with name testSubList4");
        validateChildNodesOrder(orderedChildNames);
    }

    @Test
    public void reorderChildrenAll() throws Exception {
        executeQuery(buildReorderChildNodesQuery("testNode", "testSubList3", "testSubList2", "testSubList1"));
        validateChildNodesOrder("testNode", "testSubList3", "testSubList2", "testSubList1");
    }

    @Test
    public void reorderChildrenSelected() throws Exception {
        executeQuery(buildReorderChildNodesQuery("testNode", "testSubList3", "testSubList2"));
        validateChildNodesOrder("testSubList1", "testNode", "testSubList3", "testSubList2");

        executeQuery(buildReorderChildNodesQuery("testSubList2", "testNode"));
        validateChildNodesOrder("testSubList1", "testSubList2", "testNode", "testSubList3");
    }

    @Test
    public void reorderChildrenSelectedPositionFirst() throws Exception {
        executeQuery(buildReorderChildNodesQuery(ReorderedChildrenPosition.FIRST, "testNode", "testSubList3",
                "testSubList2"));
        validateChildNodesOrder("testNode", "testSubList3", "testSubList2", "testSubList1");

        executeQuery(buildReorderChildNodesQuery(ReorderedChildrenPosition.FIRST, "testSubList1", "testSubList2"));
        validateChildNodesOrder("testSubList1", "testSubList2", "testNode", "testSubList3");
    }

    @Test
    public void reorderChildrenSelectedPositionLast() throws Exception {
        executeQuery(buildReorderChildNodesQuery(ReorderedChildrenPosition.LAST, "testNode", "testSubList3",
                "testSubList2"));
        validateChildNodesOrder("testSubList1", "testNode", "testSubList3", "testSubList2");

        executeQuery(buildReorderChildNodesQuery(ReorderedChildrenPosition.LAST, "testSubList3", "testSubList1"));
        validateChildNodesOrder("testNode", "testSubList2", "testSubList3", "testSubList1");
    }

    @Test
    public void reorderChildrenSinglePositionFirst() throws Exception {
        executeQuery(buildReorderChildNodesQuery(ReorderedChildrenPosition.FIRST, "testSubList3"));
        validateChildNodesOrder("testSubList3", "testSubList1", "testSubList2", "testNode");
    }

    @Test
    public void reorderChildrenSinglePositionLast() throws Exception {
        executeQuery(buildReorderChildNodesQuery(ReorderedChildrenPosition.LAST, "testSubList2"));
        validateChildNodesOrder("testSubList1", "testSubList3", "testNode", "testSubList2");
    }

    @Test
    public void executionStrategyNoSaveOnError() throws Exception {
        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateExisting: mutateNode(pathOrId: \"/testList/testSubList1\") {\n" +
                "      rename(name: \"testRenamed\")\n" +
                "    }\n" +
                "    mutateNonExisting: mutateNode(pathOrId: \"/testList/testSubListX\") {\n" +
                "      rename(name: \"testRenamedX\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertTrue(session.itemExists("/testList/testSubList1"));
            assertFalse(session.itemExists("/testList/testRenamed"));
            return null;
        });
    }

    @Test
    public void copyNode() throws Exception {

        executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        first: copyNode(pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\") {\n" +
            "            uuid\n" +
            "        }\n" +
            "        second: copyNode(pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testNode2\") {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        inJcr(session -> {
            assertTrue(session.itemExists("/testList/testSubList1/testNode"));
            assertTrue(session.itemExists("/testList/testSubList1/testNode2"));
            return null;
        });
    }

    @Test
    public void copyNodes() throws Exception {

        executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        copyNodes(nodes: [\n" +
            "            {pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\"},\n" +
            "            {pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testNode2\"}\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        inJcr(session -> {
            assertTrue(session.itemExists("/testList/testSubList1/testNode"));
            assertTrue(session.itemExists("/testList/testSubList1/testNode2"));
            return null;
        });
    }

    @Test
    public void copyNodesDuplicateError() throws Exception {

        JSONObject result = executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        copyNodes(nodes: [\n" +
            "            {pathOrId: \"/testList/testSubList2\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testError\"},\n" +
            "            {pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testError\"}\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        validateError(result,
            "Errors copying nodes:\n" +
            "org.jahia.modules.graphql.provider.dxm.DataFetchingException: javax.jcr.ItemExistsException: Same name siblings are not allowed: node /testList/testSubList1/testError\n"
        );
    }

    @Test
    public void copyNodesCannotCopyToItselfError() throws Exception {

        JSONObject result = executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        copyNodes(nodes: [\n" +
            "            {pathOrId: \"/testList/testSubList2\", destParentPathOrId: \"/testList/testSubList2\"},\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        validateError(result,
            "Errors copying nodes:\n" +
            "org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException: Cannot copy or move node '/testList/testSubList2' to itself or its descendant node\n"
        );
    }

    @Test
    public void copyNodesCannotCopyToDescendantError() throws Exception {

        JSONObject result = executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        copyNodes(nodes: [\n" +
            "            {pathOrId: \"/testList\", destParentPathOrId: \"/testList/testSubList2\"},\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        validateError(result,
            "Errors copying nodes:\n" +
            "org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException: Cannot copy or move node '/testList' to itself or its descendant node\n"
        );
    }

    @Test
    public void moveNode() throws Exception {

        executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        first: moveNode(pathOrId: \"/testList/testSubList2\", destParentPathOrId: \"/testList/testSubList1\") {\n" +
            "            uuid\n" +
            "        }\n" +
            "        second: moveNode(pathOrId: \"/testList/testSubList3\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testSubList3A\") {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        inJcr(session -> {
            assertFalse(session.itemExists("/testList/testSubList2"));
            assertFalse(session.itemExists("/testList/testSubList3"));
            assertTrue(session.itemExists("/testList/testSubList1/testSubList2"));
            assertTrue(session.itemExists("/testList/testSubList1/testSubList3A"));
            return null;
        });
    }

    @Test
    public void moveNodes() throws Exception {

        executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        moveNodes(nodes: [\n" +
            "            {pathOrId: \"/testList/testSubList2\", destParentPathOrId: \"/testList/testSubList1\"},\n" +
            "            {pathOrId: \"/testList/testSubList3\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testSubList3A\"}\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        inJcr(session -> {
            assertFalse(session.itemExists("/testList/testSubList2"));
            assertFalse(session.itemExists("/testList/testSubList3"));
            assertTrue(session.itemExists("/testList/testSubList1/testSubList2"));
            assertTrue(session.itemExists("/testList/testSubList1/testSubList3A"));
            return null;
        });
    }

    @Test
    public void moveNodesDuplicateError() throws Exception {

        JSONObject result = executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        moveNodes(nodes: [\n" +
            "            {pathOrId: \"/testList/testSubList2\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testError\"},\n" +
            "            {pathOrId: \"/testList/testSubList3\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testError\"}\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        validateError(result,
            "Errors moving nodes:\n" +
            "org.jahia.modules.graphql.provider.dxm.DataFetchingException: javax.jcr.ItemExistsException: Same name siblings are not allowed: node /testList/testSubList1/testError\n"
        );
    }

    @Test
    public void moveNodesCannotMoveToItselfError() throws Exception {

        JSONObject result = executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        moveNodes(nodes: [\n" +
            "            {pathOrId: \"/testList/testSubList2\", destParentPathOrId: \"/testList/testSubList2\"},\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        validateError(result,
            "Errors moving nodes:\n" +
            "org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException: Cannot copy or move node '/testList/testSubList2' to itself or its descendant node\n"
        );
    }

    @Test
    public void moveNodesCannotMoveToDescendantError() throws Exception {

        JSONObject result = executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        moveNodes(nodes: [\n" +
            "            {pathOrId: \"/testList\", destParentPathOrId: \"/testList/testSubList2\"},\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        validateError(result,
            "Errors moving nodes:\n" +
            "org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException: Cannot copy or move node '/testList' to itself or its descendant node\n"
        );
    }

    @Test
    public void pasteCopiedNode() throws Exception {

        executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        pasteNode(mode: COPY, pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\") {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        inJcr(session -> {
            assertTrue(session.itemExists("/testList/testSubList1/testNode"));
            return null;
        });
    }

    @Test
    public void pasteCopiedNodes() throws Exception {

        executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        pasteNodes(mode: COPY, namingConflictResolution: RENAME, nodes: [\n" +
            "            {pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\"},\n" +
            "            {pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\"}\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        inJcr(session -> {
            assertTrue(session.itemExists("/testList/testSubList1/testNode"));
            assertTrue(session.itemExists("/testList/testSubList1/testNode-1"));
            return null;
        });
    }

    @Test
    public void pasteCopiedNodesDuplicateError() throws Exception {

        JSONObject result = executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        pasteNodes(mode: COPY, nodes: [\n" +
            "            {pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testError\"},\n" +
            "            {pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testError\"}\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        validateError(result, "javax.jcr.ItemExistsException: Same name siblings are not allowed: node /testList/testSubList1/testError");
    }

    @Test
    public void pasteCutNode() throws Exception {

        executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        pasteNode(mode: MOVE, pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\") {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        inJcr(session -> {
            assertFalse(session.itemExists("/testList/testNode"));
            assertTrue(session.itemExists("/testList/testSubList1/testNode"));
            return null;
        });
    }

    @Test
    public void pasteCutNodes() throws Exception {

        executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        pasteNodes(mode: MOVE, namingConflictResolution: RENAME, nodes: [\n" +
            "            {pathOrId: \"/testList/testSubList2\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testDuplicate\"},\n" +
            "            {pathOrId: \"/testList/testSubList3\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testDuplicate\"}\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        inJcr(session -> {
            assertFalse(session.itemExists("/testList/testSubList2"));
            assertFalse(session.itemExists("/testList/testSubList3"));
            assertTrue(session.itemExists("/testList/testSubList1/testDuplicate"));
            assertTrue(session.itemExists("/testList/testSubList1/testDuplicate-1"));
            return null;
        });
    }

    @Test
    public void pasteCutNodesDuplicateError() throws Exception {

        JSONObject result = executeQuery("\n" +
            "mutation {\n" +
            "    jcr {\n" +
            "        pasteNodes(mode: MOVE, nodes: [\n" +
            "            {pathOrId: \"/testList/testSubList2\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testError\"},\n" +
            "            {pathOrId: \"/testList/testSubList3\", destParentPathOrId: \"/testList/testSubList1\", destName: \"testError\"}\n" +
            "        ]) {\n" +
            "            uuid\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );

        validateError(result, "javax.jcr.ItemExistsException: Same name siblings are not allowed: node /testList/testSubList1/testError");
    }

    @Test
    public void shouldNotMutateInReadOnlyMode() throws Exception {
        enableFullReadOnlyMode();
        while (getReadOnlyModeStatus() == ReadOnlyModeStatus.PARTIAL_ON) {
            enableFullReadOnlyMode();
        }

        if (getReadOnlyModeStatus() == ReadOnlyModeStatus.ON) {
            JSONObject result = executeQuery("\n" +
                    "mutation {\n" +
                    "  jcr {\n" +
                    "    addNode(parentPathOrId: \"/testList\", primaryNodeType: \"jnt:contentList\", name: \"blabla\") {\n" +
                    "      uuid\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n"
            );

            validateError(result, "Operation is not permitted as DX is in read-only mode");

            result = executeQuery("\n" +
                    "mutation {\n" +
                    "  jcr {\n" +
                    "    deleteNode(pathOrId:\"/testList\")\n" +
                    "  }\n" +
                    "}\n"
            );

            validateError(result, "Operation is not permitted as DX is in read-only mode");

            result = executeQuery("\n" +
                    "mutation {\n" +
                    "    jcr {\n" +
                    "        pasteNode(mode: MOVE, pathOrId: \"/testList/testNode\", destParentPathOrId: \"/testList/testSubList1\") {\n" +
                    "            uuid\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n"
            );

            validateError(result, "Operation is not permitted as DX is in read-only mode");

            result = executeQuery("\n" +
                    "mutation {\n" +
                    "    jcr {\n" +
                    "        moveNodes(nodes: [\n" +
                    "            {pathOrId: \"/testList\", destParentPathOrId: \"/testList/testSubList2\"},\n" +
                    "        ]) {\n" +
                    "            uuid\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n"
            );

            validateError(result, "Operation is not permitted as DX is in read-only mode");

            result = executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    mutateNodes(pathsOrIds: [\"/testList/testSubList1\",\"/testList/testSubList2\"]) {\n" +
                    "      mutateProperty(name: \"jcr:title\") {\n" +
                    "        setValue(language: \"en\", value: \"test\")\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n"
            );

            validateError(result, "Operation is not permitted as DX is in read-only mode");

            result =  executeQuery("mutation {\n" +
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
                    "}"
            );

            validateError(result, "Operation is not permitted as DX is in read-only mode");
        }
        disableReadOnlyMode();
    }
}
