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

import org.awaitility.core.ConditionFactory;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.junit.rules.TestName;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.springframework.util.CollectionUtils;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.*;
import static org.awaitility.Duration.ONE_SECOND;

public class GraphQLPublicationTest extends GraphQLTestSupport {

    @Rule public TestName name = new TestName();

    private static final long TIMEOUT_WAITING_FOR_PUBLICATION = 5000;
    private static final String PUBLICATION_JOB_NAME = "PublicationJob";
    private static final String TESTSITE_NAME = "graphqlPublicationTestSite";

    private JCRSessionWrapper defaultSession;
    private JCRSessionWrapper liveSession;
    private String testListIdentifier;
    private JahiaUser user;
    private String siteName;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();
    }

    @Before
    public void setUp() throws Exception {
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
            Assert.fail("/sites/" + siteName + "/testList is not found");
        }
    }

    @Test
    public void publishWithoutAllTree() throws Exception {
        JSONObject result =  publishPage(false);

        JSONObject mutationResult = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode");
        Assert.assertTrue(mutationResult.getBoolean("publish"));

        Assert.assertTrue(liveSession.nodeExists("/sites/" + siteName + "/test_page"));
        Assert.assertFalse(liveSession.nodeExists("/sites/" + siteName + "/test_page/sub_page"));
    }
    @Test
    public void publishAllTree() throws Exception {
        JSONObject result =  publishPage(true);

        JSONObject mutationResult = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode");
        Assert.assertTrue(mutationResult.getBoolean("publish"));

        Assert.assertTrue(liveSession.nodeExists("/sites/" + siteName + "/test_page"));
        Assert.assertTrue(liveSession.nodeExists("/sites/" + siteName + "/test_page/sub_page"));
    }

    private JSONObject publishPage(Boolean includeSubTree) throws JSONException {
        JSONObject result = executeQuery(""
                + "mutation {"
                + "    jcr {"
                + "        mutateNode(pathOrId: \"/sites/" + siteName + "/test_page\") {"
                + "            publish(languages: [\"en\"], includeSubTree: " + includeSubTree + ")"
                + "        }"
                + "    }"
                + "}"
        );

        waitForPublicationToFinish();
        return result;
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

        JCRNodeWrapper testPage = defaultSession.getNode("/sites/" + siteName).addNode("test_page", "jnt:page");
        testPage.setProperty("jcr:title", "test_page");
        testPage.setProperty("j:isHomePage", false);
        testPage.setProperty("j:templateName", "default");
        JCRNodeWrapper subPage = testPage.addNode("sub_page", "jnt:page");
        subPage.setProperty("jcr:title", "sub_page");
        subPage.setProperty("j:isHomePage", false);
        subPage.setProperty("j:templateName", "default");
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

    private void waitForPublicationToFinish() {
        SchedulerService schedulerService = BundleUtils.getOsgiService(SchedulerService.class, null);

        final ConditionFactory conditionFactory = with().pollInterval(ONE_SECOND).await()
                .atMost(TIMEOUT_WAITING_FOR_PUBLICATION, MILLISECONDS);
        conditionFactory.until(() -> Arrays.stream(schedulerService.getScheduler().getTriggerNames(SchedulerService.INSTANT_TRIGGER_GROUP))
                .noneMatch(triggerName -> triggerName.contains(PUBLICATION_JOB_NAME)));
        conditionFactory.until(() -> schedulerService.getAllActiveJobs().stream()
                .noneMatch(job -> job.getJobClass().getName().contains(PUBLICATION_JOB_NAME)));
    }
}
