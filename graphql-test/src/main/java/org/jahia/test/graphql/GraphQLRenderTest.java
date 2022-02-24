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

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.test.TestHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

import java.util.Locale;

public class GraphQLRenderTest extends GraphQLTestSupport {

    private static final String SITE_NAME = "graphql_test_render";

    private static JCRSessionWrapper session;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();
        session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, null);

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
    public void shouldCheckIfNodeIsADisplayableNode() throws Exception {
        try {
            createPage("page1");
            session.save();

            testDisplayableNode("/sites/" + SITE_NAME + "/home/listA", false);
            testDisplayableNode("/sites/" + SITE_NAME + "/home/page1", true);
        } finally {
            session.getNode(getPagePath("page1")).remove();
            session.save();
        }
    }

    private void testDisplayableNode(String path, boolean expectedResult) throws JSONException {
        JSONObject result = executeQuery("{\n" +
                "  jcr {\n" +
                "    nodeByPath(path: \"" + path + "\") {\n" +
                "      name\n" +
                "      isDisplayableNode\n" +
                "    }\n" +
                "  }\n" +
                "}");

        Assert.assertEquals(path + (expectedResult ? " should " : " should'nt ") + "be a displayable node", expectedResult,
                result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getBoolean("isDisplayableNode"));
    }

    private static String getPagePath(String page) {
        return "/sites/" + SITE_NAME + "/home/" + page;
    }

    private static void createPage(String page) throws Exception {
        JCRNodeWrapper page1 = session.getNode("/sites/" + SITE_NAME +  "/home").addNode(page, "jnt:page");
        page1.setProperty("j:templateName", "simple");
        page1.setProperty("jcr:title", page);
    }
}