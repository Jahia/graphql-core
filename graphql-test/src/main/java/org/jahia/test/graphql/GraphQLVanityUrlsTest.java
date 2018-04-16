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

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.VanityUrlService;
import org.jahia.test.TestHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

import java.util.*;

public class GraphQLVanityUrlsTest extends GraphQLTestSupport {

    private static final String SITE_NAME = "graphql_test_vanity";

    private static JCRSessionWrapper session;
    private static VanityUrlService vanityUrlService;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();
        session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, null);

        vanityUrlService = BundleUtils.getOsgiService(VanityUrlService.class, null);

        TestHelper.createSite(SITE_NAME);

        session.save();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        try {
            session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, null);
            session.getNode("/sites/" + SITE_NAME).remove();
            session.save();
        } finally {
            session.logout();
        }
    }

    @Before
    public void before() throws Exception {
        JCRTemplate.getInstance().getSessionFactory().closeAllSessions();
        session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, null);
    }

    @Test
    public void setDefaultVanity() throws Exception {
        createPage("page1");

        try {
            VanityUrl vanity1 = createVanity(true, true, "/vanity1");
            VanityUrl vanity2 = createVanity(true, false, "/vanity2");

            List<VanityUrl> vanityUrls = new ArrayList<>();
            vanityUrls.add(vanity1);
            vanityUrls.add(vanity2);

            JCRNodeWrapper page1 = session.getNode(getPagePath("page1"));
            vanityUrlService.saveVanityUrlMappings(page1, vanityUrls, Collections.singleton("en"));
            vanityUrls = vanityUrlService.getVanityUrls(page1, null, session);

            vanity2 = vanityUrls.stream().filter((v)->v.getUrl().equals("/vanity2")).findFirst().get();

            Assert.assertFalse("Vanity url is default", vanity2.isDefaultMapping());

            updateVanity(Arrays.asList(vanity2), true, null, null, null);

            vanityUrls = vanityUrlService.getVanityUrls(page1, null, session);
            vanity1 = vanityUrls.stream().filter((v)->v.getUrl().equals("/vanity1")).findFirst().get();
            vanity2 = vanityUrls.stream().filter((v)->v.getUrl().equals("/vanity2")).findFirst().get();

            Assert.assertTrue("Vanity url has not been updated", vanity2.isDefaultMapping());
            Assert.assertFalse("Previous default url has not been updated", vanity1.isDefaultMapping());
        } finally {
            session.getNode(getPagePath("page1")).remove();
            session.save();
        }
    }

    @Test
    public void setActiveVanity() throws Exception {
        createPage("page1");

        try {
            VanityUrl vanity1 = createVanity(true, true, "/vanity1");
            VanityUrl vanity2 = createVanity(true, false, "/vanity2");

            List<VanityUrl> vanityUrls = new ArrayList<>();
            vanityUrls.add(vanity1);
            vanityUrls.add(vanity2);

            JCRNodeWrapper page1 = session.getNode(getPagePath("page1"));
            vanityUrlService.saveVanityUrlMappings(page1, vanityUrls, Collections.singleton("en"));
            vanityUrls = vanityUrlService.getVanityUrls(page1, null, session);

            vanity2 = vanityUrls.stream().filter((v)->v.getUrl().equals("/vanity2")).findFirst().get();

            Assert.assertTrue("Vanity url is not active", vanity2.isActive());

            updateVanity(Arrays.asList(vanity2), null, false, null, null);

            vanityUrls = vanityUrlService.getVanityUrls(page1, null, session);
            vanity2 = vanityUrls.stream().filter((v)->v.getUrl().equals("/vanity2")).findFirst().get();

            Assert.assertFalse("Vanity url has not been updated", vanity2.isActive());
        } finally {
            session.getNode(getPagePath("page1")).remove();
            session.save();
        }
    }

    @Test
    public void setLanguage() throws Exception {
        createPage("page1");

        try {
            VanityUrl vanity1 = createVanity(true, true, "/vanity1");
            VanityUrl vanity2 = createVanity(true, false, "/vanity2");

            List<VanityUrl> vanityUrls = new ArrayList<>();
            vanityUrls.add(vanity1);
            vanityUrls.add(vanity2);

            JCRNodeWrapper page1 = session.getNode(getPagePath("page1"));
            vanityUrlService.saveVanityUrlMappings(page1, vanityUrls, Collections.singleton("en"));
            vanityUrls = vanityUrlService.getVanityUrls(page1, null, session);

            updateVanity(vanityUrls, true, null, null, "fr");

            vanityUrls = vanityUrlService.getVanityUrls(page1, null, session);
            for (VanityUrl vanityUrl : vanityUrls) {
                Assert.assertEquals("fr", vanityUrl.getLanguage());
            }
        } finally {
            session.getNode(getPagePath("page1")).remove();
            session.save();
        }
    }

    @Test
    public void setMapping() throws Exception {
        createPage("page1");

        try {
            VanityUrl vanity1 = createVanity(true, true, "/vanity1");
            VanityUrl vanity2 = createVanity(true, false, "/vanity2");

            List<VanityUrl> vanityUrls = new ArrayList<>();
            vanityUrls.add(vanity1);
            vanityUrls.add(vanity2);

            JCRNodeWrapper page1 = session.getNode(getPagePath("page1"));
            vanityUrlService.saveVanityUrlMappings(page1, vanityUrls, Collections.singleton("en"));
            vanityUrls = vanityUrlService.getVanityUrls(page1, null, session);
            vanity2 = vanityUrls.stream().filter((v)->v.getUrl().equals("/vanity2")).findFirst().get();

            updateVanity(Arrays.asList(vanity2), true, null, "/vanity3", null);

            vanityUrls = vanityUrlService.getVanityUrls(page1, null, session);
            Assert.assertEquals(2, vanityUrls.size());
            Assert.assertTrue(vanityUrls.stream().anyMatch((v)->v.getUrl().equals("/vanity3")));

            JSONObject object = updateVanity(Arrays.asList(vanity2), true, null, "/vanity1", null);
            Assert.assertEquals(1, object.getJSONArray("errors").length());
        } finally {
            session.getNode(getPagePath("page1")).remove();
            session.save();
        }
    }

    @Test
    public void shouldCreateVanityUrl() throws Exception {
        try {
            createPage("page10");
            session.save();
            VanityUrl v1 = createVanity(true, true, "/vanity1");
            VanityUrl v2 = createVanity(true, false, "/vanity2");

            JSONObject result = executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    mutateNode(pathOrId: \"" +  getPagePath("page10") + "\") {\n" +
                    "      vanity: addVanityUrl(vanityUrlInputList: [{defaultMapping: " + v1.isDefaultMapping() +
                    ", active: " + v1.isActive()  + ", url: \"" + v1.getUrl() + "\" " +
                    ", language: \"" + v1.getLanguage() + "\"},\n" +
                    "      {defaultMapping: " + v2.isDefaultMapping() +
                    ", active: " + v2.isActive()  + ", url: \"" + v2.getUrl() + "\" " +
                    ", language: \"" + v2.getLanguage() + "\"}])\n" +
                    "    }  \n" +
                    "  }" +
                    "}");

            JCRNodeWrapper page10 = session.getNode(getPagePath("page10"));
            List<VanityUrl> vanityUrls = vanityUrlService.getVanityUrls(page10, null, session);
            Assert.assertEquals(2, vanityUrls.size());
            VanityUrl vanity1 = vanityUrls.stream().filter((v)->v.getUrl().equals("/vanity1")).findFirst().get();
            VanityUrl vanity2 = vanityUrls.stream().filter((v)->v.getUrl().equals("/vanity2")).findFirst().get();
            Assert.assertTrue("Vanity url is not active", vanity2.isActive());
            Assert.assertTrue("Vanity url is not default", vanity1.isDefaultMapping());

        } finally {
            session.getNode(getPagePath("page10")).remove();
            session.save();
        }

    }

    @Test
    public void shouldMoveALLVanityUrlsFromOneSource() throws Exception {
        try {
            createPage("page1");
            createPage("page2");

            List<VanityUrl> vanityUrls = new ArrayList<>();
            vanityUrls.add(createVanity(true, true, "/vanity1"));
            vanityUrls.add(createVanity(true, false, "/vanity2"));

            JCRNodeWrapper page1 = session.getNode(getPagePath("page1"));
            vanityUrlService.saveVanityUrlMappings(page1, vanityUrls, Collections.singleton("en"));

            List<VanityUrl> savedVanityUrls = vanityUrlService.getVanityUrls(page1, "en", session);
            Assert.assertEquals(2, savedVanityUrls.size());

            JSONObject result = moveVanities(savedVanityUrls, getPagePath("page2"));
            JSONArray mutations = result.getJSONObject("data").getJSONObject("jcr").getJSONArray("mutateVanityUrls");

            Assert.assertEquals(2, mutations.length());
            for (int i = 0; i < mutations.length(); i++) {
                JSONObject mutation = mutations.getJSONObject(i);
                Assert.assertTrue(mutation.getBoolean("move"));
            }

            // vanity urls should be moved
            List<VanityUrl> movedVanityUrls = vanityUrlService.getVanityUrls(session.getNode(getPagePath("page2")), "en", session);
            Assert.assertEquals(2, movedVanityUrls.size());
            Assert.assertEquals(0, vanityUrlService.getVanityUrls(page1, "en", session).size());

            // moved vanity urls should not be default anymore
            for (VanityUrl movedVanityUrl : movedVanityUrls) {
                Assert.assertFalse(movedVanityUrl.isDefaultMapping());
                Assert.assertTrue(movedVanityUrl.isActive());
            }

            // vanity source node should be cleaned
            // TODO clean, we can't clean because for now we use the mixin to found node that do not have vanity anymore in default but may be still in live
            // Assert.assertFalse(page1.isNodeType(VanityUrlManager.JAHIAMIX_VANITYURLMAPPED));
            // Assert.assertFalse(page1.hasNode(VanityUrlManager.VANITYURLMAPPINGS_NODE));
        } finally {
            session.getNode(getPagePath("page1")).remove();
            session.getNode(getPagePath("page2")).remove();
            session.save();
        }
    }

    @Test
    public void shouldMoveVanityUrlsFromMultipleSources() throws Exception {
        try {
            createPage("page3");
            createPage("page4");
            createPage("page5");

            List<VanityUrl> vanityUrls1 = new ArrayList<>();
            vanityUrls1.add(createVanity(true, true, "/vanity1"));
            vanityUrls1.add(createVanity(true, false, "/vanity2"));

            List<VanityUrl> vanityUrls2 = new ArrayList<>();
            vanityUrls2.add(createVanity(true, true, "/vanity3"));
            vanityUrls2.add(createVanity(true, false, "/vanity4"));

            JCRNodeWrapper page1 = session.getNode(getPagePath("page3"));
            vanityUrlService.saveVanityUrlMappings(page1, vanityUrls1, Collections.singleton("en"));

            JCRNodeWrapper page2 = session.getNode(getPagePath("page4"));
            vanityUrlService.saveVanityUrlMappings(page2, vanityUrls2, Collections.singleton("en"));

            List<VanityUrl> savedVanityUrls1 = vanityUrlService.getVanityUrls(page1, "en", session);
            Assert.assertEquals(2, savedVanityUrls1.size());

            List<VanityUrl> savedVanityUrls2 = vanityUrlService.getVanityUrls(page2, "en", session);
            Assert.assertEquals(2, savedVanityUrls1.size());

            List<VanityUrl> allSaved = new ArrayList<>(savedVanityUrls1);
            allSaved.addAll(savedVanityUrls2);

            JSONObject result = moveVanities(allSaved, getPagePath("page5"));
            JSONArray mutations = result.getJSONObject("data").getJSONObject("jcr").getJSONArray("mutateVanityUrls");

            Assert.assertEquals(4, mutations.length());
            for (int i = 0; i < mutations.length(); i++) {
                JSONObject mutation = mutations.getJSONObject(i);
                Assert.assertTrue(mutation.getBoolean("move"));
            }

            // vanity urls should be moved
            List<VanityUrl> movedVanityUrls = vanityUrlService.getVanityUrls(session.getNode(getPagePath("page5")), "en", session);
            Assert.assertEquals(4, movedVanityUrls.size());
            Assert.assertEquals(0, vanityUrlService.getVanityUrls(page1, "en", session).size());
            Assert.assertEquals(0, vanityUrlService.getVanityUrls(page2, "en", session).size());

            // moved vanity urls should not be default anymore
            for (VanityUrl movedVanityUrl : movedVanityUrls) {
                Assert.assertFalse(movedVanityUrl.isDefaultMapping());
                Assert.assertTrue(movedVanityUrl.isActive());
            }

            // vanity source node should be cleaned
            // TODO clean, we can't clean because for now we use the mixin to found node that do not have vanity anymore in default but may be still in live
            // Assert.assertFalse(page1.isNodeType(VanityUrlManager.JAHIAMIX_VANITYURLMAPPED));
            // Assert.assertFalse(page1.hasNode(VanityUrlManager.VANITYURLMAPPINGS_NODE));
            // Assert.assertFalse(page2.isNodeType(VanityUrlManager.JAHIAMIX_VANITYURLMAPPED));
            // Assert.assertFalse(page2.hasNode(VanityUrlManager.VANITYURLMAPPINGS_NODE));
        } finally {
            session.getNode(getPagePath("page3")).remove();
            session.getNode(getPagePath("page4")).remove();
            session.getNode(getPagePath("page5")).remove();
            session.save();
        }
    }

    @Test
    public void shouldMoveOneVanityUrls() throws Exception {

        try {
            createPage("page6");
            createPage("page7");

            List<VanityUrl> vanityUrls = new ArrayList<>();
            vanityUrls.add(createVanity(true, true, "/vanity1"));
            vanityUrls.add(createVanity(true, false, "/vanity2"));

            JCRNodeWrapper page1 = session.getNode(getPagePath("page6"));
            vanityUrlService.saveVanityUrlMappings(page1, vanityUrls, Collections.singleton("en"));

            List<VanityUrl> savedVanityUrls = vanityUrlService.getVanityUrls(page1, "en", session);
            Assert.assertEquals(2, savedVanityUrls.size());

            JSONObject result = moveVanities(Collections.singletonList(savedVanityUrls.get(0)), getPagePath("page7"));
            JSONArray mutations = result.getJSONObject("data").getJSONObject("jcr").getJSONArray("mutateVanityUrls");

            Assert.assertEquals(1, mutations.length());
            for (int i = 0; i < mutations.length(); i++) {
                JSONObject mutation = mutations.getJSONObject(i);
                Assert.assertTrue(mutation.getBoolean("move"));
            }

            // vanity urls should be moved
            List<VanityUrl> movedVanityUrls = vanityUrlService.getVanityUrls(session.getNode(getPagePath("page7")), "en", session);
            Assert.assertEquals(1, movedVanityUrls.size());
            Assert.assertEquals(1, vanityUrlService.getVanityUrls(page1, "en", session).size());

            // moved vanity urls should not be default anymore
            for (VanityUrl movedVanityUrl : movedVanityUrls) {
                Assert.assertFalse(movedVanityUrl.isDefaultMapping());
                Assert.assertTrue(movedVanityUrl.isActive());
            }
        } finally {
            session.getNode(getPagePath("page6")).remove();
            session.getNode(getPagePath("page7")).remove();
            session.save();
        }

    }

    @Test
    public void shouldNotMoveJcrNode() throws Exception {

        try {
            createPage("page8");
            createPage("page9");

            session.save();

            JSONObject result = executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    mutateVanityUrls(pathsOrIds: [\"" + getPagePath("page9") + "\"]) {\n" +
                    "      move(target: \"" + getPagePath("page8") + "\")\n" +
                    "    }\n" +
                    "  }\n" +
                    "}");

            JSONArray errors = result.getJSONArray("errors");

            Assert.assertTrue(errors.length() == 1);
        } finally {
            session.getNode(getPagePath("page8")).remove();
            session.getNode(getPagePath("page9")).remove();
            session.save();
        }

    }

    private JSONObject updateVanity(List<VanityUrl> vanityUrls, Boolean defaultMapping, Boolean active, String url, String language) throws Exception {

        return JCRTemplate.getInstance().doExecuteWithSystemSession((JCRCallback<JSONObject>) session -> {
            StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
            for (VanityUrl vanityUrl : vanityUrls) {
                stringJoiner.add("\"" + vanityUrl.getIdentifier() + "\"");
            }

            List<String> params = new ArrayList<>();
            if (defaultMapping != null) {
                params.add("defaultMapping:" + defaultMapping);
            }
            if (active != null) {
                params.add("active:" + active);
            }
            if (url != null) {
                params.add("url:\"" + url + "\"");
            }
            if (language != null) {
                params.add("language:\"" + language + "\"");
            }
            try {
                return executeQuery("mutation {\n" +
                        "  jcr {\n" +
                        "    mutateVanityUrls(pathsOrIds: " + stringJoiner.toString() + ") {\n" +
                        "      update(" + StringUtils.join(params, ",") + ")\n" +
                        "    }\n" +
                        "  }\n" +
                        "}");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private JSONObject moveVanities(List<VanityUrl> vanityUrls, String target) throws Exception {

        return JCRTemplate.getInstance().doExecuteWithSystemSession((JCRCallback<JSONObject>) session -> {
            StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
            for (VanityUrl vanityUrl : vanityUrls) {
                stringJoiner.add("\"" + vanityUrl.getIdentifier() + "\"");
            }

            try {
                return executeQuery("mutation {\n" +
                        "  jcr {\n" +
                        "    mutateVanityUrls(pathsOrIds: " + stringJoiner.toString() + ") {\n" +
                        "      move(target: \"" + target + "\")\n" +
                        "    }\n" +
                        "  }\n" +
                        "}");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String getPagePath(String page) {
        return "/sites/" + SITE_NAME + "/home/" + page;
    }

    private static void createPage(String page) throws Exception {
        JCRNodeWrapper page1 = session.getNode("/sites/" + SITE_NAME +  "/home").addNode(page, "jnt:page");
        page1.setProperty("j:templateName", "simple");
        page1.setProperty("jcr:title", page);
    }

    private static VanityUrl createVanity(boolean isActive, boolean isDefault, String url) {
        VanityUrl vanityUrl = new VanityUrl();
        vanityUrl.setActive(isActive);
        vanityUrl.setDefaultMapping(isDefault);
        vanityUrl.setLanguage("en");
        vanityUrl.setSite(SITE_NAME);
        vanityUrl.setUrl(url);
        return vanityUrl;
    }
}