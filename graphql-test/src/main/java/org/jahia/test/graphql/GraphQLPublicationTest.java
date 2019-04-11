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

import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaException;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.test.TestHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.*;

public class GraphQLPublicationTest extends GraphQLTestSupport {

    private static final long TIMEOUT_WAITING_FOR_PUBLICATION = 5000;
    private String testListIdentifier;
    private static JahiaUser user;
    private static final String TESTSITE_NAME = "graphqlPublicationTestSite";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();

        createTestSite(TESTSITE_NAME, TestHelper.DX_BASE_DEMO_TEMPLATES, new HashSet<>(Arrays.asList("en", "fr")),
                Collections.singleton("en"), false);

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRPublicationService.getInstance().publishByMainId(session.getNode("/sites/" + TESTSITE_NAME).getIdentifier());
            user = JahiaUserManagerService.getInstance().createUser("testUser", null, "testPassword", new Properties(), session).getJahiaUser();
            JahiaGroupManagerService.getInstance().lookupGroup(null, "privileged", session).addMember(user);
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        TestHelper.deleteSite(TESTSITE_NAME);

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JahiaUserManagerService.getInstance().deleteUser(user.getLocalPath(), session);
            session.save();
            return null;
        });
    }

    @Test
    public void shouldRetrieveAggregatedPublicationInfoFromDefault() throws Exception {
        testAggregatedPublicationInfo("EDIT", "/sites/" + TESTSITE_NAME + "/testList/publicationTestList", null);
    }

    @Test
    public void shouldGetErrorNotRetrieveAggregatedPublicationInfoFromLive() throws Exception {
        testAggregatedPublicationInfo("LIVE", "/", "Publication fields can only be used with nodes from EDIT workspace");
    }

    private void testAggregatedPublicationInfo(String workspace, String path, String expectedErrorMessage) throws RepositoryException, JSONException {
        resetData();

        JSONObject result = executeQuery(""
                + "{"
                + "    jcr(workspace: " + workspace + ")  {"
                + "        nodeByPath(path: \"" + path + "\") {"
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

        if (expectedErrorMessage != null) {
            validateError(result, "Publication fields can only be used with nodes from EDIT workspace");
        } else {
            JSONObject info = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("aggregatedPublicationInfo");

            Assert.assertEquals("NOT_PUBLISHED", info.getString("publicationStatus"));
            Assert.assertFalse(info.getBoolean("locked"));
            Assert.assertFalse(info.getBoolean("workInProgress"));
            Assert.assertTrue(info.getBoolean("allowedToPublishWithoutWorkflow"));
        }
    }

    @Test
    public void shouldPublish() throws Exception {
        testPublication(true);
    }

    @Test
    public void shouldPublishNoSubtree() throws Exception {
        testPublication(false);
    }

    private void testPublication(boolean publishSubNodes) throws RepositoryException, JSONException {
        resetData();

        JSONObject result = executeQuery(""
                + "mutation {"
                + "    jcr {"
                + "        mutateNode(pathOrId: \"/sites/" + TESTSITE_NAME + "/testList\") {"
                + "            publish(publishSubNodes: " + publishSubNodes + ")"
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
                    session.getNode("/sites/" + TESTSITE_NAME + "/testList");
                    Assert.assertEquals(publishSubNodes, session.nodeExists("/sites/" + TESTSITE_NAME + "/testList/publicationTestList"));
                    Assert.assertEquals(publishSubNodes, session.nodeExists("/sites/" + TESTSITE_NAME + "/testList/publicationTestList"));
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
    public void shouldUnpublishInAllLanguage() throws Exception {
        testUnpublish("[\"en\", \"fr\"]", true, true, false);
    }

    @Test
    public void shouldUnpublishInOneLanguage() throws Exception {
        testUnpublish("[\"en\"]", false, true, false);
    }

    private void testUnpublish(String languages, boolean expectFrUnpublished, boolean expectEnUnpublished, boolean expectedNodeUnpublished) throws RepositoryException, JSONException {
        resetData();
        JCRPublicationService.getInstance().publishByMainId(testListIdentifier);

        JSONObject result = executeQuery(""
                + "mutation {"
                + "    jcr {"
                + "        mutateNode(pathOrId: \"/sites/" + TESTSITE_NAME + "/testList/publicationTestListI18n\") {"
                + "            unpublish(languages: " + languages + ")"
                + "        }"
                + "    }"
                + "}"
        );

        JSONObject mutationResult = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode");
        Assert.assertTrue(mutationResult.getBoolean("unpublish"));

        JCRTemplate.getInstance().doExecute(user, Constants.LIVE_WORKSPACE, null, session -> {
            Assert.assertEquals(expectedNodeUnpublished, !session.nodeExists("/sites/" + TESTSITE_NAME + "/testList/publicationTestListI18n"));
            return null;
        });
        JCRTemplate.getInstance().doExecute(user, Constants.LIVE_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertEquals(expectEnUnpublished, !session.nodeExists("/sites/" + TESTSITE_NAME + "/testList/publicationTestListI18n"));
            return null;
        });
        JCRTemplate.getInstance().doExecute(user, Constants.LIVE_WORKSPACE, Locale.FRENCH, session -> {
            Assert.assertEquals(expectFrUnpublished, !session.nodeExists("/sites/" + TESTSITE_NAME + "/testList/publicationTestListI18n"));
            return null;
        });
    }

    private void resetData() throws RepositoryException {
        removeData();
        createData();
        JCRSessionFactory.getInstance().closeAllSessions();
    }

    private void createData() throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, session -> {
            JCRNodeWrapper testList = session.getNode("/sites/" + TESTSITE_NAME).addNode("testList", "jnt:contentList");
            JCRNodeWrapper publicationTestList = testList.addNode("publicationTestList", "jnt:contentList");
            publicationTestList.addNode("subList", "jnt:contentList");
            publicationTestList.addNode("subList2", "jnt:contentList");
            session.save();
            testListIdentifier = testList.getIdentifier();
            return null;
        });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper publicationTestListI18n = session.getNode("/sites/" + TESTSITE_NAME + "/testList").addNode("publicationTestListI18n", "jnt:contentList");
            publicationTestListI18n.setProperty("jcr:title", "en_title");
            JCRNodeWrapper subList = publicationTestListI18n.addNode("subList", "jnt:contentList");
            subList.setProperty("jcr:title", "en_sub_title");
            JCRNodeWrapper subList2 = publicationTestListI18n.addNode("subList2", "jnt:contentList");
            subList2.setProperty("jcr:title", "en_sub2_title");
            session.save();
            return null;
        });
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH, session -> {
            session.getNode("/sites/" + TESTSITE_NAME + "/testList/publicationTestListI18n").setProperty("jcr:title", "fr_title");
            session.getNode("/sites/" + TESTSITE_NAME + "/testList/publicationTestListI18n/subList").setProperty("jcr:title", "fr_sub_title");
            session.getNode("/sites/" + TESTSITE_NAME + "/testList/publicationTestListI18n/subList").setProperty("jcr:title", "fr_sub2_title");
            session.save();
            return null;
        });
    }

    private void removeData() throws RepositoryException {
        removeData(Constants.EDIT_WORKSPACE);
        removeData(Constants.LIVE_WORKSPACE);
    }

    private void removeData(String workspace) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, workspace, null, session -> {
            if (session.itemExists("/sites/" + TESTSITE_NAME + "/testList")) {
                session.getNode("/sites/" + TESTSITE_NAME + "/testList").remove();
                session.save();
            }
            return null;
        });
    }

    private static JCRSiteNode createTestSite(String name, String template, Set<String> languages, Set<String> mandatoryLanguages, boolean mixLanguagesActive) throws JahiaException, IOException, RepositoryException {
        TestHelper.createSite(name, template);

        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
        JCRSiteNode site = (JCRSiteNode)session.getNode("/sites/" + name);
        if (!CollectionUtils.isEmpty(languages) && !languages.equals(site.getLanguages())) {
            site.setLanguages(languages);
        }

        if (!CollectionUtils.isEmpty(mandatoryLanguages) && !mandatoryLanguages.equals(site.getMandatoryLanguages())) {
            site.setMandatoryLanguages(mandatoryLanguages);
        }

        if (mixLanguagesActive != site.isMixLanguagesActive()) {
            site.setMixLanguagesActive(mixLanguagesActive);
        }

        session.save();
        return site;
    }
}
