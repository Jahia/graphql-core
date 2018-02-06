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

import javax.jcr.RepositoryException;
import java.util.Locale;

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
            assertNodeType(node, "jnt:contentList");
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
            assertNodeType(node1, "jnt:contentList");
            assertNodeType(node1, "jmix:renderable");
            assertPropertyValue(node1, "jcr:title", "test");
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
            assertPropertyValue(session.getNode("/testList/testSubList1"), "jcr:title", "test");
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
            assertPropertyValue(session.getNode("/testList/testSubList1"), "jcr:title", "test");
            assertPropertyValue(session.getNode("/testList/testSubList2"), "jcr:title", "test");
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
            assertPropertyValue(session.getNode("/testList/testSubList1"), "jcr:title", "test");
            assertPropertyValue(session.getNode("/testList/testSubList2"), "jcr:title", "test");
            return null;
        });
    }

    private void assertPropertyValue(JCRNodeWrapper node, String property, String value) throws RepositoryException {
        Assert.assertEquals(value, node.getProperty(property).getString());
    }

    private void assertNodeType(JCRNodeWrapper node1, String typeName) throws RepositoryException {
        Assert.assertTrue(node1.isNodeType(typeName));
    }


}
