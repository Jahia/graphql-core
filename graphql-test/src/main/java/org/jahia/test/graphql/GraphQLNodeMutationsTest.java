/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.test.graphql;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation.ReorderedChildrenPosition;
import org.jahia.modules.graphql.provider.dxm.config.GraphQLLimits;
import org.jahia.services.content.*;
import org.jahia.settings.readonlymode.ReadOnlyModeController;
import org.jahia.settings.readonlymode.ReadOnlyModeController.ReadOnlyModeStatus;
import org.jahia.test.graphql.utils.TestFileUtils;
import org.jahia.utils.EncryptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
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

            if (session.nodeExists("/testList")) {
                session.getNode("/testList").remove();
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
        // add node with same name
        result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testList\",name:\"testNew1\",primaryNodeType:\"jnt:contentList\",useAvailableNodeName:true) {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String sameNameUuid = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getString("uuid");
        inJcr(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(sameNameUuid);
            assertEquals("/testList/testNew1-1", node.getPath());
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
                "        setValue(language: \"en\", type: DATE, option: NOT_ZONED_DATE, value: \"" + dateValue + "\")\n" +
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
            node.addMixin("jmix:markedForDeletionRoot");
            node.setProperty("j:deletionUser", "user");
            node.setProperty("j:deletionDate", simpleDateFormat.format(date));
            session.save();
            return null;
        });

            JSONObject result = executeQuery("query {\n" +
                    "  jcr {\n" +
                    "    nodeByPath(path: \"/testNodeNotZonedDate\") {\n" +
                    "      property(name: \"j:deletionDate\") {\n" +
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
    public void mutatePropertyWithEncryptedValue() throws Exception {
        String value = "thisIs@My>Password";

        executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId: \"/\", name: \"testEncryptedValue\", primaryNodeType: \"nt:unstructured\") {\n" +
                "      mutateProperty(name: \"password\") {\n" +
                "        setValue(language: \"en\", type: STRING, option: ENCRYPTED, value: \"" + value + "\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        inJcr(session -> {
            assertNotEquals(value, session.getNode("/testEncryptedValue").getProperty("password").getString());
            return null;
        });

        inJcr(session -> {
            session.getNode("/testEncryptedValue").remove();
            session.save();
            return null;
        });
    }

    @Test
    public void queryPropertyWithEncryptedValue() throws Exception {
        String value = "thisIs@My>Password";

        inJcr(session -> {
            JCRNodeWrapper node = session.getRootNode().addNode("testEncryptedValue");
            node.setProperty("password", EncryptionUtils.passwordBaseEncrypt(value));

            session.save();
            return null;
        });

        JSONObject result = executeQuery("query {\n" +
                "  jcr {\n" +
                "    nodeByPath(path: \"/testEncryptedValue\") {\n" +
                "      property(name: \"password\") {\n" +
                "        decryptedValue\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");

        String propertyDecryptedValue = result
                .getJSONObject("data")
                .getJSONObject("jcr")
                .getJSONObject("nodeByPath")
                .getJSONObject("property")
                .getString("decryptedValue");

        assertEquals(value, propertyDecryptedValue);

        inJcr(session -> {
            session.getNode("/testEncryptedValue").remove();
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
        validateNoErrors(mutateSubListTitlesByQuery("", TITLE_1));
        inJcr(session -> {
            assertEquals(TITLE_1, session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals(TITLE_1, session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            assertEquals(TITLE_1, session.getNode("/testList/testSubList3").getProperty("jcr:title").getString());
            return null;
        });

        validateNoErrors(mutateSubListTitlesByQuery(LIMIT_1, TITLE_2));
        inJcr(session -> {
            assertEquals(TITLE_2, session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals(TITLE_1, session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            assertEquals(TITLE_1, session.getNode("/testList/testSubList3").getProperty("jcr:title").getString());
            return null;
        });

        validateNoErrors(mutateSubListTitlesByQuery(LIMIT_1 + ",offset:1", TITLE_3));
        inJcr(session -> {
            assertEquals(TITLE_2, session.getNode("/testList/testSubList1").getProperty("jcr:title").getString());
            assertEquals(TITLE_3, session.getNode("/testList/testSubList2").getProperty("jcr:title").getString());
            assertEquals(TITLE_1, session.getNode("/testList/testSubList3").getProperty("jcr:title").getString());
            return null;
        });

        validateNoErrors(executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodesByQuery(query:\"select * from [jnt:contentList] where isdescendantnode('/testList')\",queryLanguage:SQL2) {\n" +
                "      delete\n" +
                "    }\n" +
                "  }\n" +
                "}\n"));
        inJcr(session -> {
            assertFalse(session.nodeExists("/testList/testSubList1"));
            assertFalse(session.nodeExists("/testList/testSubList2"));
            assertFalse(session.nodeExists("/testList/testSubList3"));
            return null;
        });
    }

    private static final String SUB_LIST_1 = "/testList/testSubList1";
    private static final String SUB_LIST_2 = "/testList/testSubList2";
    private static final String SUB_LIST_3 = "/testList/testSubList3";
    private static final String TITLE = "jcr:title";
    // The titles the paging and refusal tests write, so that what they assert is which nodes carry one rather than
    // what it says.
    private static final String PAGED = "paged";
    private static final String REFUSED = "refused";
    private static final String SHARED = "shared";
    // The successive titles mutateNodesByQuery writes, and the page it writes the second and third with.
    private static final String TITLE_1 = "test1";
    private static final String TITLE_2 = "test2";
    private static final String TITLE_3 = "test3";
    private static final String LIMIT_1 = ",limit:1";
    // The aggregate guard runs before execution, so an oversized batch is refused with graphql-java's wording rather
    // than by the per-field backstop inside the resolver.
    private static final String BATCH_TOO_LARGE = "maximum mutation batch size exceeded %d > %d";
    private static final String QUERY_MATCHED_TOO_MANY =
            "This mutation matched more nodes than the maximum of %d it may operate on; operate on %d or fewer at a"
                    + " time, using the limit/offset arguments.";
    // A limit argument states the size, so asking for more than the bound is refused before the query runs and the
    // message names what was asked rather than what matched.
    private static final String ASKED_TOO_MANY =
            "This mutation asked to operate on %d nodes, more than the maximum of %d it may operate on; operate on %d"
                    + " or fewer at a time, using the limit/offset arguments.";
    // A request whose whole allowance is claimed is told so, because no page this field asks for can fit.
    private static final String ALLOWANCE_ALREADY_USED =
            "This mutation matched more nodes than what this request has left of its mutation batch allowance of %d,"
                    + " which other fields in it already claim in full; move this mutation to another request.";
    // Part of the allowance claimed elsewhere in the document: the page is one the instance permits, so what is
    // reported is the room left rather than the configured limit.
    private static final String ALLOWANCE_PARTLY_CLAIMED =
            "This mutation matched more nodes than the %d that other fields in this request leave of its mutation"
                    + " batch allowance of %d; operate on %d or fewer here, and move the rest to another request.";

    /**
     * Runs the given assertions with the mutation batch limit temporarily set, restoring it afterwards. The limit is
     * global state, so every test that changes it has to put it back.
     */
    private static void withMutationBatchLimit(int limit, Callable<Void> body) throws Exception {
        int originalLimit = GraphQLLimits.getMutationBatchLimit();
        try {
            GraphQLLimits.updateMutationBatchLimit(limit);
            body.call();
        } finally {
            GraphQLLimits.updateMutationBatchLimit(originalLimit);
        }
    }

    /** The statement the query-driven mutation tests page over: the contentLists under /testList, in name order. */
    private static final String SUB_LISTS_QUERY =
            "select * from [jnt:contentList] where isdescendantnode('/testList') order by localname()";

    /** Wraps mutation fields in the JCR mutation document they need. */
    private static JSONObject executeJcrMutation(String fields) throws JSONException {
        return executeQuery("mutation {\n  jcr {\n" + fields + "  }\n}\n");
    }

    /** The selection that sets jcr:title, shared by every field below. */
    private static String titlingSelection(String value) {
        return "      mutateProperty(name: \"jcr:title\") { setValue(language: \"en\", value: \"" + value + "\") }\n";
    }

    /** A mutateNodesByQuery field, aliased when an alias is given, titling whatever it matches. */
    private static String titlingQueryField(String alias, String extraArguments, String value) {
        return "    " + (alias == null ? "" : alias + ": ")
                + "mutateNodesByQuery(query:\"" + SUB_LISTS_QUERY + "\",queryLanguage:SQL2" + extraArguments + ") {\n"
                + titlingSelection(value) + "    }\n";
    }

    /** A mutateNodes field naming its target, titling it. */
    private static String titlingNamedField(String alias, String path, String value) {
        return "    " + alias + ": mutateNodes(pathsOrIds: [\"" + path + "\"]) {\n"
                + titlingSelection(value) + "    }\n";
    }

    /**
     * Sets jcr:title on the sub-lists the query matches and, in the same document, on one named explicitly. That shape
     * is what makes an enumerated field claim part of the request's allowance before the query-driven one runs.
     */
    private static JSONObject mutateSubListTitlesBesideANamedNode(String extraArguments, String value)
            throws JSONException {
        return executeJcrMutation(titlingQueryField("q", extraArguments, value)
                + titlingNamedField("n", SUB_LIST_1, value));
    }

    /** Two aliased query-driven mutations in one document, so that they share the request's allowance. */
    private static JSONObject mutateSubListTitlesByTwoAliases(String extraArguments, String first, String second)
            throws JSONException {
        return executeJcrMutation(titlingQueryField("a", extraArguments, first)
                + titlingQueryField("b", extraArguments, second));
    }

    /** Sets jcr:title on every contentList under /testList that the query matches, optionally with extra arguments. */
    private static JSONObject mutateSubListTitlesByQuery(String extraArguments, String value) throws JSONException {
        return executeJcrMutation(titlingQueryField(null, extraArguments, value));
    }

    /** Sets jcr:title on the nodes named explicitly, i.e. through mutateNodes rather than a query. */
    private static JSONObject mutateNamedNodeTitles(String value, String... pathsOrIds) throws JSONException {
        String paths = Arrays.stream(pathsOrIds).map(p -> '"' + p + '"').collect(Collectors.joining(","));
        return executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    mutateNodes(pathsOrIds: [" + paths + "]) {\n" +
                "      mutateProperty(name: \"jcr:title\") {\n" +
                "        setValue(language: \"en\", value: \"" + value + "\")\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }

    /** Asserts which of the three sub-lists carry the given title, and that the rest carry no title at all. */
    private static void assertTitledSubLists(String expectedTitle, String... expectedTitledPaths) throws Exception {
        Set<String> titled = new HashSet<>(Arrays.asList(expectedTitledPaths));
        inJcr(session -> {
            for (String path : Arrays.asList(SUB_LIST_1, SUB_LIST_2, SUB_LIST_3)) {
                if (titled.contains(path)) {
                    assertEquals("Expected " + path + " to have been mutated",
                            expectedTitle, session.getNode(path).getProperty(TITLE).getString());
                } else {
                    assertFalse("Expected " + path + " to be outside the configured mutation batch limit",
                            session.getNode(path).hasProperty(TITLE));
                }
            }
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldNotExceedConfiguredMutationBatchLimit() throws Exception {
        // Bound below the number of matching nodes: the query matches all three sub-lists, so without a bound the
        // mutation would return a handle for every one of them.
        withMutationBatchLimit(2, () -> {
            JSONObject result = mutateSubListTitlesByQuery("", "bounded");
            validateError(result, String.format(QUERY_MATCHED_TOO_MANY, 2, 2));
            // Nothing persisted: the session is only saved when the request completes without errors.
            assertTitledSubLists("bounded");
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldRefuseAnExplicitLimitWiderThanTheBound() throws Exception {
        // A limit argument selects a slice of the matches, but it may not ask for a wider one than the instance
        // permits: over the bound the mutation is refused rather than quietly cut back down to it.
        withMutationBatchLimit(1, () -> {
            JSONObject result = mutateSubListTitlesByQuery(",limit:3", REFUSED);
            validateError(result, String.format(ASKED_TOO_MANY, 3, 1, 1));
            assertTitledSubLists(REFUSED);
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldRefuseAnExplicitLimitWiderThanTheBoundWhateverMatches() throws Exception {
        // The refusal follows from the limit alone. An offset past every match means the query returns nothing at all,
        // so a check on the result set would let this through - asking for more than the instance permits is a bad
        // request whether or not the data would have made it one.
        withMutationBatchLimit(1, () -> {
            JSONObject result = mutateSubListTitlesByQuery(",limit:3,offset:5", REFUSED);
            validateError(result, String.format(ASKED_TOO_MANY, 3, 1, 1));
            assertTitledSubLists(REFUSED);
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldTreatANonPositiveLimitAsNoPageAtAll() throws Exception {
        // A limit of zero or less asks for no page rather than for no nodes, so there is nothing to compare against
        // the bound and the request is refused on what it matched, exactly as it is with no limit argument. Taking
        // such a value as a page would set a query limit of zero and quietly mutate nothing at all.
        for (String limit : new String[]{",limit:0", ",limit:-1"}) {
            withMutationBatchLimit(2, () -> {
                JSONObject result = mutateSubListTitlesByQuery(limit, REFUSED);
                validateError(result, String.format(QUERY_MATCHED_TOO_MANY, 2, 2));
                assertTitledSubLists(REFUSED);
                return null;
            });
        }
    }

    @Test
    public void mutationBatchLimitOfZeroShouldDisableTheBound() throws Exception {
        withMutationBatchLimit(0, () -> {
            validateNoErrors(mutateSubListTitlesByQuery("", "unbounded"));
            assertTitledSubLists("unbounded", SUB_LIST_1, SUB_LIST_2, SUB_LIST_3);
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldStillPageWhenTheBoundIsDisabled() throws Exception {
        // With the bound off there is nothing to compare a limit against, but the argument is still a page: it selects
        // which of the matches to operate on whether or not the instance bounds mutations at all. Folding the two
        // together made a limit smaller than the match count fail here too, with no bound in force to justify it.
        withMutationBatchLimit(0, () -> {
            validateNoErrors(mutateSubListTitlesByQuery(LIMIT_1, PAGED));
            assertTitledSubLists(PAGED, SUB_LIST_1);
            return null;
        });
    }

    @Test
    public void mutateNodesShouldRejectABatchLargerThanTheConfiguredMutationBatchLimit() throws Exception {
        // The caller enumerated the nodes explicitly, so an oversized batch must fail rather than have a prefix of the
        // supplied list quietly mutated.
        withMutationBatchLimit(2, () -> {
            JSONObject result = mutateNamedNodeTitles("rejected", SUB_LIST_1, SUB_LIST_2, SUB_LIST_3);
            validateError(result, String.format(BATCH_TOO_LARGE, 3, 2));
            assertTitledSubLists("rejected");
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldPageWithAnExplicitLimitWithinTheBound() throws Exception {
        // limit says which slice of the matches to operate on, so a query matching more than it asks for is a page,
        // not an overrun. Three sub-lists match; a limit of 1 mutates the first of them and leaves the rest alone.
        withMutationBatchLimit(2, () -> {
            validateNoErrors(mutateSubListTitlesByQuery(LIMIT_1, PAGED));
            assertTitledSubLists(PAGED, SUB_LIST_1);
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldPageWithALimitEqualToTheBound() throws Exception {
        // The boundary between paging and refusal: a page exactly as wide as the bound is still a page, because
        // operating on that many nodes is precisely what the bound allows. Three sub-lists match, so a bound of 2 with
        // limit:2 mutates the first two and leaves the third alone rather than refusing the request.
        withMutationBatchLimit(2, () -> {
            validateNoErrors(mutateSubListTitlesByQuery(",limit:2", PAGED));
            assertTitledSubLists(PAGED, SUB_LIST_1, SUB_LIST_2);
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldPageWithLimitAndOffsetWithinTheBound() throws Exception {
        // offset picks which page, so the same limit of 1 mutates the second sub-list rather than the first.
        withMutationBatchLimit(2, () -> {
            validateNoErrors(mutateSubListTitlesByQuery(LIMIT_1 + ",offset:1", PAGED));
            assertTitledSubLists(PAGED, SUB_LIST_2);
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldPageWhateverElseTheDocumentAsksFor() throws Exception {
        // The allowance is opened at the bound minus the batch size of the whole document, so an enumerated field
        // sharing the document lowers it before any field runs. A page the instance permits stays permitted: what the
        // caller asked for is measured against the configured limit, not against the room the rest of the request left.
        withMutationBatchLimit(3, () -> {
            validateNoErrors(mutateSubListTitlesBesideANamedNode(",limit:3,offset:2", SHARED));
            // The query pages past the first sub-list, the enumerated field names it, and the middle one is untouched.
            assertTitledSubLists(SHARED, SUB_LIST_1, SUB_LIST_3);
            return null;
        });
    }

    @Test
    public void mutateNodesByQueryShouldRefuseWhenTheDocumentLeavesTooLittleOfTheAllowance() throws Exception {
        // The same document without a limit: the query matches all three sub-lists and only two of the allowance are
        // left, so the refusal reports the room left rather than the configured limit, and nothing is persisted.
        withMutationBatchLimit(3, () -> {
            JSONObject result = mutateSubListTitlesBesideANamedNode("", SHARED);
            validateError(result, String.format(ALLOWANCE_PARTLY_CLAIMED, 2, 3, 2));
            assertTitledSubLists(SHARED);
            return null;
        });
    }

    @Test
    public void aliasingAQueryDrivenMutationShouldNotMultiplyTheConfiguredMutationBatchLimit() throws Exception {
        // How many nodes a query matches is not knowable before it runs, so each alias draws from what the request has
        // left rather than from the whole bound. Both aliases page within the bound on their own; together they are
        // over it, so the second one is refused and the whole request fails.
        withMutationBatchLimit(2, () -> {
            JSONObject result = mutateSubListTitlesByTwoAliases(",limit:2", "first", "second");
            validateError(result, String.format(ALLOWANCE_ALREADY_USED, 2));
            // Nothing persisted: the session is only saved when the request completes without errors.
            assertTitledSubLists("first");
            return null;
        });
    }

    @Test
    public void aliasingAMutationShouldNotMultiplyTheConfiguredMutationBatchLimit() throws Exception {
        // The bound is on what one request asks for in total, so selecting the field three times does not raise it:
        // each alias below is within the limit on its own, while together they are not.
        withMutationBatchLimit(2, () -> {
            JSONObject result = executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    a: mutateNodes(pathsOrIds: [\"" + SUB_LIST_1 + "\"]) {\n" +
                    "      mutateProperty(name: \"jcr:title\") { setValue(language: \"en\", value: \"aliased\") }\n" +
                    "    }\n" +
                    "    b: mutateNodes(pathsOrIds: [\"" + SUB_LIST_2 + "\"]) {\n" +
                    "      mutateProperty(name: \"jcr:title\") { setValue(language: \"en\", value: \"aliased\") }\n" +
                    "    }\n" +
                    "    c: mutateNodes(pathsOrIds: [\"" + SUB_LIST_3 + "\"]) {\n" +
                    "      mutateProperty(name: \"jcr:title\") { setValue(language: \"en\", value: \"aliased\") }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n");
            validateError(result, String.format(BATCH_TOO_LARGE, 3, 2));
            assertTitledSubLists("aliased");
            return null;
        });
    }

    @Test
    public void addNodesBatchShouldRejectABatchLargerThanTheConfiguredMutationBatchLimit() throws Exception {
        withMutationBatchLimit(1, () -> {
            JSONObject result = executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    addNodesBatch(nodes: [\n" +
                    "      {parentPathOrId: \"/testList\", name: \"batched1\", primaryNodeType: \"jnt:contentList\"},\n" +
                    "      {parentPathOrId: \"/testList\", name: \"batched2\", primaryNodeType: \"jnt:contentList\"}\n" +
                    "    ]) {\n" +
                    "      uuid\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n");
            validateError(result, String.format(BATCH_TOO_LARGE, 2, 1));
            inJcr(session -> {
                assertFalse("A rejected batch must not create any of the supplied nodes",
                        session.nodeExists("/testList/batched1"));
                assertFalse("A rejected batch must not create any of the supplied nodes",
                        session.nodeExists("/testList/batched2"));
                return null;
            });
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
    public void propertyBinaryValue() throws Exception {
        String fieldName = "test-binary";
        String fileName = "filename1.txt";
        String fileContent = "test text content";
        Part uploadFile = TestFileUtils.getFilePart(fieldName, fileName, fileContent);

        JSONObject result = executeQueryWithFiles("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testFolder\", name:\"" + fileName + "\", primaryNodeType:\"jnt:file\") {\n" +
                "      addChild(name:\"jcr:content\", primaryNodeType:\"nt:resource\") {\n" +
                "        setData:mutateProperty(name:\"jcr:data\") {\n" +
                "          setValue(value:\""  + fieldName + "\")\n" +
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
                "}\n", Collections.singletonList(uploadFile));

        String value = result.getJSONObject("data").getJSONObject("jcr")
                .getJSONObject("addNode").getJSONObject("addChild").getJSONObject("node")
                .getJSONObject("property").getString("value");
        assertEquals(fileContent, value);

        inJcr(session -> {
            assertTrue(session.nodeExists("/testFolder/" + fileName));
            JCRNodeWrapper fileNode = session.getNode("/testFolder/" + fileName);
            assertTrue(fileNode.isNodeType(Constants.JAHIANT_FILE));
            assertEquals("text/plain", fileNode.getFileContent().getContentType());

            try {
                assertEquals(fileContent, IOUtils.toString(fileNode.getFileContent().downloadFile()));
            } catch (IOException e) {
                fail(e.getMessage());
            }
            return null;
        });
    }

    /** Test binary property by providing its value as string */
    @Test
    public void propertyBinaryValueAsString() throws Exception {
        String fileContent = "my text binary value";
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testFolder\", name:\"file2.txt\", primaryNodeType:\"jnt:file\") {\n" +
                "      uuid\n" +
                "      addChild(name:\"jcr:content\", primaryNodeType:\"nt:resource\") {\n" +
                "        setData:mutateProperty(name:\"jcr:data\") {\n" +
                "          setValue(value:\"" + fileContent + "\")\n" +
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
                "}\n");

        String value = result.getJSONObject("data").getJSONObject("jcr")
                .getJSONObject("addNode").getJSONObject("addChild").getJSONObject("node")
                .getJSONObject("property").getString("value");
        assertEquals(fileContent, value);

        inJcr(session -> {
            assertTrue(session.nodeExists("/testFolder/file2.txt"));
            JCRNodeWrapper fileNode = session.getNode("/testFolder/file2.txt");
            assertTrue(fileNode.isNodeType(Constants.JAHIANT_FILE));
            assertEquals("text/plain", fileNode.getFileContent().getContentType());

            try {
                assertEquals(fileContent, IOUtils.toString(fileNode.getFileContent().downloadFile()));
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

        try {
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
        } finally {
            disableReadOnlyMode();
        }
    }

    @Test
    public void testSpecialCharactersInNode() throws Exception {
        // add simple node
        JSONObject result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testList\",name:\"[]*|/%\",primaryNodeType:\"jnt:contentList\") {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String uuid = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getString("uuid");
        String finalUuid = uuid;
        inJcr(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(finalUuid);
            assertEquals("/testList/%5B%5D%2A%7C %", node.getPath());
            assertTrue(node.isNodeType("jnt:contentList"));
            return null;
        });

        result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testList\",name:\".\",primaryNodeType:\"jnt:contentList\") {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        uuid = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getString("uuid");
        String finalUuid1 = uuid;
        inJcr(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(finalUuid1);
            assertTrue(node.getPath().startsWith("/testList")); // we should improve this
            assertTrue(node.isNodeType("jnt:contentList"));
            return null;
        });

        result = executeQuery("mutation {\n" +
                "  jcr {\n" +
                "    addNode(parentPathOrId:\"/testList\",name:\"..\",primaryNodeType:\"jnt:contentList\") {\n" +
                "      uuid\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        String errorType = result.getJSONArray("errors").getJSONObject(0).getString("errorType");
        assertEquals("DataFetchingException", errorType);
        String message = result.getJSONArray("errors").getJSONObject(0).getString("message");
        assertEquals("javax.jcr.RepositoryException: Invalid last path element for adding node .. relative to node /testList", message);
    }
}
