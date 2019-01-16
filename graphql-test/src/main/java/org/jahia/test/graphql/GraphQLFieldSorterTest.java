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
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

/**
 * Test to verify Field Sorter functionnality
 *
 * @author yousria
 */
public class GraphQLFieldSorterTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            node.addNode("ZHello", "jnt:contentList");
            node.addNode("abonjour", "jnt:contentList");

            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }


    @Test
    public void shouldSortDisplayNames() throws Exception {
        JSONObject result = executeQuery("{"
                + "  jcr (workspace:EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", language:\"en\"}, fieldSorter: "
                + "{fieldName: \"displayName\", sortType: DESC, ignoreCase:false}) {"
                + "      nodes {"
                + "        displayName(language:\"en\")"
                + "      }"
                + "    }"
                + "  }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes.length() == 2);
        Assert.assertTrue(nodes.getJSONObject(0).getString("displayName").equals("abonjour"));
        Assert.assertTrue(nodes.getJSONObject(1).getString("displayName").equals("ZHello"));

        JSONObject result2 = executeQuery("{"
                + "  jcr (workspace:EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", language:\"en\"}, fieldSorter: "
                + "{fieldName: \"displayName\", sortType: ASC, ignoreCase:false}) {"
                + "      nodes {"
                + "        displayName(language:\"en\")"
                + "      }"
                + "    }"
                + "  }"
                + "}");

        JSONArray nodes2 = result2.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes2.length() == 2);
        Assert.assertTrue(nodes2.getJSONObject(0).getString("displayName").equals("ZHello"));
        Assert.assertTrue(nodes2.getJSONObject(1).getString("displayName").equals("abonjour"));

        JSONObject result3 = executeQuery("{"
                + "  jcr (workspace:EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", language:\"en\"}, fieldSorter: "
                + "{fieldName: \"displayName\", sortType: ASC, ignoreCase:true}) {"
                + "      nodes {"
                + "        displayName(language:\"en\")"
                + "      }"
                + "    }"
                + "  }"
                + "}");

        JSONArray nodes3 = result3.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes3.length() == 2);
        Assert.assertTrue(nodes3.getJSONObject(0).getString("displayName").equals("abonjour"));
        Assert.assertTrue(nodes3.getJSONObject(1).getString("displayName").equals("ZHello"));

        JSONObject result4 = executeQuery("{"
                + "  jcr (workspace:EDIT) {"
                + "    nodesByCriteria(criteria: {paths: \"/testList\", nodeType: \"jnt:content\", language:\"en\"}, fieldSorter: "
                + "{fieldName: \"displayName\", sortType: DESC, ignoreCase:true}) {"
                + "      nodes {"
                + "        displayName(language:\"en\")"
                + "      }"
                + "    }"
                + "  }"
                + "}");

        JSONArray nodes4 = result4.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByCriteria").getJSONArray("nodes");
        Assert.assertTrue(nodes4.length() == 2);
        Assert.assertTrue(nodes4.getJSONObject(0).getString("displayName").equals("ZHello"));
        Assert.assertTrue(nodes4.getJSONObject(1).getString("displayName").equals("abonjour"));



    }
}
