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

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.NonUniqueUrlMappingException;
import org.jahia.services.seo.jcr.VanityUrlService;
import org.jahia.test.TestHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

import java.util.*;

public class GraphQLVanityUrlsTest extends GraphQLTestSupport {

    private static final String SITE_NAME = "graphql_test_vanity";
    private static final String SITE_NAME_OTHER = "graphql_test_vanity_other";

    private static JCRSessionWrapper session;
    private static VanityUrlService vanityUrlService;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();
        session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, null);

        vanityUrlService = BundleUtils.getOsgiService(VanityUrlService.class, null);

        TestHelper.createSite(SITE_NAME);
        TestHelper.createSite(SITE_NAME_OTHER);

        session.save();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        try {
            session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, null);
            session.getNode("/sites/" + SITE_NAME).remove();
            session.getNode("/sites/" + SITE_NAME_OTHER).remove();
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

            executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    mutateNode(pathOrId: \"" +  getPagePath("page10") + "\") {\n" +
                    "      vanity: addVanityUrl(vanityUrlInputList: [{defaultMapping: " + v1.isDefaultMapping() +
                    ", active: " + v1.isActive()  + ", url: \"" + v1.getUrl() + "\" " +
                    ", language: \"" + v1.getLanguage() + "\"},\n" +
                    "      {defaultMapping: " + v2.isDefaultMapping() +
                    ", active: " + v2.isActive()  + ", url: \"" + v2.getUrl() + "\" " +
                    ", language: \"" + v2.getLanguage() + "\"}]) { uuid }\n" +
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

            JSONObject result = executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    mutateNode(pathOrId: \"" +  getPagePath("page10") + "\") {\n" +
                    "      vanity: addVanityUrl(vanityUrlInputList: [{defaultMapping: " + v1.isDefaultMapping() +
                    ", active: " + v1.isActive()  + ", url: \"" + v1.getUrl() + "\" " +
                    ", language: \"" + v1.getLanguage() + "\"},\n" +
                    "      {defaultMapping: " + v2.isDefaultMapping() +
                    ", active: " + v2.isActive()  + ", url: \"" + v2.getUrl() + "\" " +
                    ", language: \"" + v2.getLanguage() + "\"}]) { uuid }\n" +
                    "    }  \n" +
                    "  }" +
                    "}");

            JSONObject extensions = ((JSONObject) result.getJSONArray("errors").get(0)).getJSONObject("extensions");
            Assert.assertTrue(extensions.has("/vanity1"));
            Assert.assertTrue(extensions.has("/vanity2"));

            result = executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    mutateNode(pathOrId: \"" +  getPagePath("page10") + "\") {\n" +
                    "      vanity: addVanityUrl(vanityUrlInputList: [{defaultMapping: " + v1.isDefaultMapping() +
                    ", active: " + v1.isActive()  + ", url: \"/" + v1.getUrl() + "\" " +
                    ", language: \"" + v1.getLanguage() + "\"},\n" +
                    "      {defaultMapping: " + v2.isDefaultMapping() +
                    ", active: " + v2.isActive()  + ", url: \"" + v2.getUrl() + "/\" " +
                    ", language: \"" + v2.getLanguage() + "\"}]) { uuid }\n" +
                    "    }  \n" +
                    "  }" +
                    "}");

            extensions = ((JSONObject) result.getJSONArray("errors").get(0)).getJSONObject("extensions");
            Assert.assertTrue(extensions.has("//vanity1"));
            Assert.assertEquals(NonUniqueUrlMappingException.class.getName(), ((JSONObject)extensions.get("//vanity1")).get("type"));
            Assert.assertTrue(extensions.has("/vanity2/"));
            Assert.assertEquals(NonUniqueUrlMappingException.class.getName(), ((JSONObject)extensions.get("/vanity2/")).get("type"));
            
            JCRNodeWrapper page0 = createPage("page0", SITE_NAME_OTHER);
            session.save();

            executeQuery("mutation {\n" +
                    "  jcr {\n" +
                    "    mutateNode(pathOrId: \"" +  page0.getPath() + "\") {\n" +
                    "      vanity: addVanityUrl(vanityUrlInputList: [{defaultMapping: " + v1.isDefaultMapping() +
                    ", active: " + v1.isActive()  + ", url: \"" + v1.getUrl() + "\" " +
                    ", language: \"" + v1.getLanguage() + "\"},\n" +
                    "      {defaultMapping: " + v2.isDefaultMapping() +
                    ", active: " + v2.isActive()  + ", url: \"" + v2.getUrl() + "\" " +
                    ", language: \"" + v2.getLanguage() + "\"}]) { uuid }\n" +
                    "    }  \n" +
                    "  }" +
                    "}");

            List<VanityUrl> otherVanityUrls = vanityUrlService.getVanityUrls(page0, null, session);
            Assert.assertEquals(2, otherVanityUrls.size());
            VanityUrl otherVanity1 = otherVanityUrls.stream().filter((v)->v.getUrl().equals("/vanity1")).findFirst().get();
            VanityUrl otherVanity2 = otherVanityUrls.stream().filter((v)->v.getUrl().equals("/vanity2")).findFirst().get();
            Assert.assertTrue("Vanity url is not active", otherVanity2.isActive());
            Assert.assertTrue("Vanity url is not default", otherVanity1.isDefaultMapping());

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

    private static JCRNodeWrapper createPage(String page, String siteName) throws Exception {
        JCRNodeWrapper page1 = session.getNode("/sites/" + siteName +  "/home").addNode(page, "jnt:page");
        page1.setProperty("j:templateName", "simple");
        page1.setProperty("jcr:title", page);
        return page1;
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