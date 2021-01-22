/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.test.TestHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.junit.rules.TestName;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.*;
import static org.awaitility.Duration.ONE_SECOND;
import static org.hamcrest.Matchers.equalTo;

public class GraphQLPublicationTest extends GraphQLTestSupport {

    @Rule public TestName name = new TestName();

    private final Logger logger = LoggerFactory.getLogger(GraphQLPublicationTest.class);
    private static final long TIMEOUT_WAITING_FOR_PUBLICATION = 5000;
    private static final String TESTSITE_NAME = "graphqlPublicationTestSite";

    private JCRSessionWrapper defaultSession;
    private JCRSessionWrapper liveSession;
    private String testListIdentifier;
    private JahiaUser user;
    private String siteName;
    private SchedulerService schedulerService;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();
    }

    @Before
    public void setUp() throws Exception {
        schedulerService = BundleUtils.getOsgiService(SchedulerService.class, null);
        defaultSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH);
        liveSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.LIVE_WORKSPACE, Locale.ENGLISH);
        siteName = TESTSITE_NAME + name.getMethodName();
        createTestSite(siteName, TestHelper.DX_BASE_DEMO_TEMPLATES, new HashSet<>(Arrays.asList("en", "fr")),
                Collections.singleton("en"), false);
        JCRPublicationService.getInstance().publishByMainId(defaultSession.getNode("/sites/" + siteName).getIdentifier());
        user = JahiaUserManagerService.getInstance().createUser("testUser", null, "testPassword", new Properties(), defaultSession).getJahiaUser();
        JahiaGroupManagerService.getInstance().lookupGroup(null, "privileged", defaultSession).addMember(user);

        createData();
    }

    @After
    public void tearDown() throws Exception {
        JCRSessionFactory.getInstance().closeAllSessions();
        removeData();
    }

    @Test
    public void shouldRetrieveAggregatedPublicationInfoFromDefault() throws Exception {
        testAggregatedPublicationInfo("EDIT", "/sites/" + siteName + "/testList/publicationTestList", null);
    }

    @Test
    public void shouldGetErrorNotRetrieveAggregatedPublicationInfoFromLive() throws Exception {
        testAggregatedPublicationInfo("LIVE", "/", "Publication fields can only be used with nodes from EDIT workspace");
    }

    private void testAggregatedPublicationInfo(String workspace, String path, String expectedErrorMessage) throws RepositoryException, JSONException {

        JSONObject result = executeQuery(""
                + "{"
                + "    jcr(workspace: " + workspace + ")  {"
                + "        nodeByPath(path: \"" + path + "\") {"
                + "            aggregatedPublicationInfo(language: \"en\") {"
                + "                publicationStatus"
                + "                locked"
                + "                workInProgress"
                + "                allowedToPublishWithoutWorkflow"
                + "                existsInLive"
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
            Assert.assertFalse(info.getBoolean("existsInLive"));
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

    private void testPublication(boolean publishSubNodes) throws RepositoryException, JSONException, SchedulerException {

        JSONObject result = executeQuery(""
                + "mutation {"
                + "    jcr {"
                + "        mutateNode(pathOrId: \"/sites/" + siteName + "/testList\") {"
                + "            publish(publishSubNodes: " + publishSubNodes + ")"
                + "        }"
                + "    }"
                + "}"
        );

        waitForPublicationToFinish();

        JSONObject mutationResult = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode");
        Assert.assertTrue(mutationResult.getBoolean("publish"));

        try {
            liveSession.getNode("/sites/" + siteName + "/testList");
            Assert.assertEquals(publishSubNodes, liveSession.nodeExists("/sites/" + siteName + "/testList/publicationTestList"));
            Assert.assertEquals(publishSubNodes, liveSession.nodeExists("/sites/" + siteName + "/testList/publicationTestList"));
        } catch (PathNotFoundException e) {
            debugSession(liveSession);
            Assert.fail("/sites/" + siteName + "/testList is not found");
        }
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
        JCRPublicationService.getInstance().publishByMainId(testListIdentifier);

        JSONObject result = executeQuery(""
                + "mutation {"
                + "    jcr {"
                + "        mutateNode(pathOrId: \"/sites/" + siteName + "/testList/publicationTestListI18n\") {"
                + "            unpublish(languages: " + languages + ")"
                + "        }"
                + "    }"
                + "}"
        );

        JSONObject mutationResult = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode");
        Assert.assertTrue(mutationResult.getBoolean("unpublish"));

        JCRTemplate.getInstance().doExecute(user, Constants.LIVE_WORKSPACE, null, session -> {
            Assert.assertEquals(expectedNodeUnpublished, !session.nodeExists("/sites/" + siteName + "/testList/publicationTestListI18n"));
            return null;
        });
        JCRTemplate.getInstance().doExecute(user, Constants.LIVE_WORKSPACE, Locale.ENGLISH, session -> {
            Assert.assertEquals(expectEnUnpublished, !session.nodeExists("/sites/" + siteName + "/testList/publicationTestListI18n"));
            return null;
        });
        JCRTemplate.getInstance().doExecute(user, Constants.LIVE_WORKSPACE, Locale.FRENCH, session -> {
            Assert.assertEquals(expectFrUnpublished, !session.nodeExists("/sites/" + siteName + "/testList/publicationTestListI18n"));
            return null;
        });
    }

    private void resetData() throws RepositoryException {

    }

    private void createData() throws RepositoryException {
        JCRNodeWrapper testList = defaultSession.getNode("/sites/" + siteName).addNode("testList", "jnt:contentList");
        testListIdentifier = testList.getIdentifier();
        JCRNodeWrapper publicationTestList = testList.addNode("publicationTestList", "jnt:contentList");
        publicationTestList.addNode("subList", "jnt:contentList");
        publicationTestList.addNode("subList2", "jnt:contentList");
        JCRNodeWrapper publicationTestListI18n = defaultSession.getNode("/sites/" + siteName + "/testList").addNode("publicationTestListI18n", "jnt:contentList");
        publicationTestListI18n.setProperty("jcr:title", "en_title");
        JCRNodeWrapper subList = publicationTestListI18n.addNode("subList", "jnt:contentList");
        subList.setProperty("jcr:title", "en_sub_title");
        JCRNodeWrapper subList2 = publicationTestListI18n.addNode("subList2", "jnt:contentList");
        subList2.setProperty("jcr:title", "en_sub2_title");
        defaultSession.save();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH, session -> {
            session.getNode("/sites/" + siteName + "/testList/publicationTestListI18n").setProperty("jcr:title", "fr_title");
            session.getNode("/sites/" + siteName + "/testList/publicationTestListI18n/subList").setProperty("jcr:title", "fr_sub_title");
            session.getNode("/sites/" + siteName + "/testList/publicationTestListI18n/subList").setProperty("jcr:title", "fr_sub2_title");
            session.save();
            return null;
        });
    }

    private void removeData() throws Exception {
        // Use dedicated session to clean up everything.
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH);
        removeData(session);
        removeData(JCRSessionFactory.getInstance().getCurrentUserSession(Constants.LIVE_WORKSPACE, Locale.ENGLISH));
        TestHelper.deleteSite(siteName);
        JahiaUserManagerService.getInstance().deleteUser(user.getLocalPath(), session);
        session.save();
        JCRSessionFactory.getInstance().closeAllSessions();
    }

    private void removeData(JCRSessionWrapper session) throws RepositoryException {
        if (session.itemExists("/sites/" + siteName + "/testList")) {
            session.getNode("/sites/" + siteName + "/testList").remove();
            session.save();
        }
    }

    private JCRSiteNode createTestSite(String name, String template, Set<String> languages, Set<String> mandatoryLanguages, boolean mixLanguagesActive) throws JahiaException, IOException, RepositoryException {
        TestHelper.createSite(name, template);

        JCRSiteNode site = (JCRSiteNode) defaultSession.getNode("/sites/" + name);
        if (!CollectionUtils.isEmpty(languages) && !languages.equals(site.getLanguages())) {
            site.setLanguages(languages);
        }

        if (!CollectionUtils.isEmpty(mandatoryLanguages) && !mandatoryLanguages.equals(site.getMandatoryLanguages())) {
            site.setMandatoryLanguages(mandatoryLanguages);
        }

        if (mixLanguagesActive != site.isMixLanguagesActive()) {
            site.setMixLanguagesActive(mixLanguagesActive);
        }

        defaultSession.save();
        return site;
    }

    private void debugSession(JCRSessionWrapper session) throws RepositoryException, SchedulerException, JSONException {
        Assert.assertNotNull("Sitename should not be null", siteName);
        Assert.assertNotNull("testListIdentifier should not be null", testListIdentifier);
        if (session.nodeExists("/sites/" + siteName)) {
            logger.info("Trigger names: {}",
                    Arrays.toString(schedulerService.getScheduler().getTriggerNames(SchedulerService.INSTANT_TRIGGER_GROUP)));
            JSONObject query = executeQuery(""
                    + "query {"
                    + "  jcr(workspace: LIVE) {"
                    + "     nodeByPath(path: \"/sites/"+ siteName + "/testList\") {"
                    + "         property(name: \"j:published\") {"
                    + "            value"
                    + "         }"
                    +"      }"
                    +"   }"
                    +"}"
            );
            JSONObject property = query.getJSONObject("data").getJSONObject("jcr")
                    .getJSONObject("nodeByPath").getJSONObject("property");
            logger.info("Path: /sites/{}/testList\nProperty: j:published\nValue: {}", siteName, property.getBoolean("value"));

            final JCRNodeWrapper node = session.getNode("/sites/" + siteName);
            Assert.assertTrue("Site" + siteName + " should have node testList", node.hasNode("testList"));
            logger.info("Site: {} has node 'testList': {}", siteName, node.hasNode("testList"));
            if (!node.hasNode("testList")) {
                query = executeQuery(""
                        + "query {"
                        + "  jcr(workspace: LIVE) {"
                        + "     nodeByPath(path: \"/sites/"+ siteName + "\") {"
                        + "         children(names: [\"testList\"]) {"
                        + "            nodes {"
                        + "              name"
                        + "            }"
                        + "         }"
                        +"      }"
                        +"   }"
                        +"}"
                );
                JSONArray nodes = query.getJSONObject("data").getJSONObject("jcr")
                        .getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes");
                for (int index = 0; index < nodes.length(); index++ ) {
                    logger.info("Node names for /sites/{}: {}", siteName, nodes.getString(index));
                }
            }
        } else {
            logger.error("/sites/{} not found. It may have been deleted", siteName);
        }
    }

    private void waitForPublicationToFinish() throws SchedulerException {

        // Method #1
        long startedWaitingAt = System.currentTimeMillis();

        // Wait until the node is published via a background job.
        while(Arrays.stream(schedulerService.getScheduler().getTriggerNames(SchedulerService.INSTANT_TRIGGER_GROUP))
                .anyMatch(t -> t.contains("Publication"))) {
            if (System.currentTimeMillis() - startedWaitingAt > TIMEOUT_WAITING_FOR_PUBLICATION) {
                Assert.fail("Timeout waiting for node to be published");
            }
        }


        // Method #2
        /*
        with().pollInterval(ONE_SECOND).await().atMost(TIMEOUT_WAITING_FOR_PUBLICATION, MILLISECONDS)
                .ignoreExceptions()
                .until(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        final String[] triggerNames = schedulerService.getScheduler()
                                .getTriggerNames(SchedulerService.INSTANT_TRIGGER_GROUP);
                        return Arrays.stream(triggerNames).noneMatch(t -> t.contains("Publication"));
                    }
                }, equalTo(true));
         */
    }
}
