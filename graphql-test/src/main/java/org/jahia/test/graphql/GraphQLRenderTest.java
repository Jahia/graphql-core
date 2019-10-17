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