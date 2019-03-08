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
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

/**
 * Test field grouping functionality
 *
 * @author akarmanov
 */
public class GraphQLFieldGroupingTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("groupingRoot", "jnt:contentFolder");
            node.addNode("nodeGroup1-0", "jnt:text");
            node.addNode("nodeGroup2-1", "jnt:contentList");
            node.addNode("noGroup1-0", "jnt:bigText");
            node.addNode("nodeGroup2-0", "jnt:contentList");
            node.addNode("noGroup1-1", "jnt:bigText");
            node.addNode("nodeGroup1-1", "jnt:text");

            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldGroupNodesWithoutSort() throws JSONException {
        JSONObject result = executeQuery("{" +
                "jcr {" +
                "    nodeByPath(path:\"/groupingRoot\") {" +
                "      result : descendants(fieldGrouping:{fieldName:\"type.value\", groups:[\"jnt:text\", \"jnt:fakeName\", \"jnt:contentList\"], groupingType:START}) {" +
                "        nodes {" +
                "          type:property(name:\"jcr:primaryType\") {" +
                "            value" +
                "          }" +
                "          name" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("result").getJSONArray("nodes");
        Assert.assertEquals(6, nodes.length());
        Assert.assertEquals("nodeGroup1-0", nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals("nodeGroup1-1", nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals("nodeGroup2-1", nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals("nodeGroup2-0", nodes.getJSONObject(3).getString("name"));
        Assert.assertEquals("noGroup1-0", nodes.getJSONObject(4).getString("name"));
        Assert.assertEquals("noGroup1-1", nodes.getJSONObject(5).getString("name"));

        result = executeQuery("{" +
                "jcr {" +
                "    nodeByPath(path:\"/groupingRoot\") {" +
                "      result : descendants(fieldGrouping:{fieldName:\"type.value\", groups:[\"jnt:fakeName\", \"jnt:text\", \"jnt:contentList\"], groupingType:END}) {" +
                "        nodes {" +
                "          type:property(name:\"jcr:primaryType\") {" +
                "            value" +
                "          }" +
                "          name" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("result").getJSONArray("nodes");
        Assert.assertEquals(6, nodes.length());
        Assert.assertEquals("noGroup1-0", nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals("noGroup1-1", nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals("nodeGroup1-0", nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals("nodeGroup1-1", nodes.getJSONObject(3).getString("name"));
        Assert.assertEquals("nodeGroup2-1", nodes.getJSONObject(4).getString("name"));
        Assert.assertEquals("nodeGroup2-0", nodes.getJSONObject(5).getString("name"));
    }

    @Test
    public void shouldGroupNodesWithSort() throws JSONException {
        JSONObject result = executeQuery("{" +
                "jcr {" +
                "    nodeByPath(path:\"/groupingRoot\") {" +
                "      result : descendants(fieldGrouping:{fieldName:\"type.value\", groups:[\"jnt:contentList\", \"jnt:text\", \"jnt:fakeName\"], groupingType:START}, fieldSorter:{sortType:ASC, fieldName: \"name\"}) {" +
                "        nodes {" +
                "          type:property(name:\"jcr:primaryType\") {" +
                "            value" +
                "          }" +
                "          name" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("result").getJSONArray("nodes");
        Assert.assertEquals(6, nodes.length());
        Assert.assertEquals("nodeGroup2-0", nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals("nodeGroup2-1", nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals("nodeGroup1-0", nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals("nodeGroup1-1", nodes.getJSONObject(3).getString("name"));
        Assert.assertEquals("noGroup1-0", nodes.getJSONObject(4).getString("name"));
        Assert.assertEquals("noGroup1-1", nodes.getJSONObject(5).getString("name"));
    }
}
