/*
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
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jcr.PathNotFoundException;

public class GraphQLPublicationTest extends GraphQLTestSupport {

    private static final long TIMEOUT_WAITING_FOR_PUBLICATION = 5000;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, session -> {
            session.getNode("/").addNode("testList", "jnt:contentList");
            session.save();

            JCRNodeWrapper testlist1 = session.getNode("/").addNode("testList1", "jnt:contentList");
            JCRNodeWrapper content1 = testlist1.addNode("text1", "jnt:text");
            JCRNodeWrapper content2 = testlist1.addNode("text2", "jnt:text");
            session.save();
            JCRPublicationService.getInstance().publishByMainId(testlist1.getIdentifier());
            content1.addMixin("jmix:cache");
            content2.remove();
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrieveAggregatedPublicationInfoFromDefault() throws Exception {

        JSONObject result = executeQuery(""
                + "{"
                + "    jcr {"
                + "        nodeByPath(path: \"/testList\") {"
                + "            aggregatedPublicationInfo(language: \"en\") {"
                + "                publicationStatus"
                + "                locked"
                + "                workInProgress"
                + "                allowedToPublishWithoutWorkflow"
                + "		       }"
                + "        }"
                + "    }"
                + "}"
        );

        JSONObject info = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("aggregatedPublicationInfo");

        Assert.assertEquals("NOT_PUBLISHED", info.getString("publicationStatus"));
        Assert.assertFalse(info.getBoolean("locked"));
        Assert.assertFalse(info.getBoolean("workInProgress"));
        Assert.assertTrue(info.getBoolean("allowedToPublishWithoutWorkflow"));
    }

    @Test
    public void shouldGetErrorNotRetrieveAggregatedPublicationInfoFromLive() throws Exception {

        JSONObject result = executeQuery(""
                + "{"
                + "    jcr(workspace: LIVE) {"
                + "        nodeByPath(path: \"/\") {"
                + "            aggregatedPublicationInfo(language: \"en\") {"
                + "                publicationStatus"
                + "		       }"
                + "        }"
                + "    }"
                + "}"
        );

        validateError(result, "Publication fields can only be used with nodes from EDIT workspace");
    }

    @Test
    public void shouldPublish() throws Exception {

        JSONObject result = executeQuery(""
                + "mutation {"
                + "    jcr {"
                + "        mutateNode(pathOrId: \"/testList\") {"
                + "            publish(languages: [\"en\"])"
                + "        }"
                + "    }"
                + "}"
        );

        JSONObject mutationResult = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode");
        Assert.assertTrue(mutationResult.getBoolean("publish"));

        // Wait until the node is published via a background job.
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE, null, session -> {
            long startedWaitingAt = System.currentTimeMillis();
            do {
                if (System.currentTimeMillis() - startedWaitingAt > TIMEOUT_WAITING_FOR_PUBLICATION) {
                    Assert.fail("Timeout waiting for node to be published");
                }
                try {
                    session.getNode("/testList");
                    break;
                } catch (PathNotFoundException e) {
                    // Continue waiting: the node hasn't been published yet.
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new JahiaRuntimeException(e);
                }
            } while (true);
            return null;
        });
    }

    @Test
    public void shouldPublishOneNde() throws Exception {
        JSONObject result = executeQuery(""
                + "mutation {"
                + "    jcr {"
                + "        mutateNode(pathOrId: \"/testList1\") {"
                + "            publish(nodeOnly: true)"
                + "        }"
                + "    }"
                + "}"
        );

        JSONObject mutationResult = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode");
        Assert.assertTrue(mutationResult.getBoolean("publish"));

        // Wait until the node is published via a background job.
        long startedWaitingAt = System.currentTimeMillis();
        do {
            if (System.currentTimeMillis() - startedWaitingAt > TIMEOUT_WAITING_FOR_PUBLICATION) {
                Assert.fail("Timeout waiting for node to be published");
            }
            if (JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE, null, session -> {
                try {
                    session.getNode("/testList1/text2");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new JahiaRuntimeException(e);
                    }
                    return false;
                } catch (PathNotFoundException e) {
                    JCRNodeWrapper text1 = session.getNode("/testList1/text1");
                    Assert.assertTrue(text1 != null && !text1.isNodeType("jmix:cache"));
                    Assert.assertTrue(!session.nodeExists("/testList1/text2"));
                    return true;
                }
            })) {
                break;
            }
        } while (true);
    }
}
