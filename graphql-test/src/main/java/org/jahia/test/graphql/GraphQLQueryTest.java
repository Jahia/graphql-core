/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */
package org.jahia.test.graphql;

import java.util.Locale;

import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public class GraphQLQueryTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");

            node.addNode("testSubList1", "jnt:contentList");
            node.addNode("testSubList2", "jnt:contentList");
            node.addNode("testSubList3", "jnt:contentList");

            JCRNodeWrapper subNode4 = node.addNode("testSubList4", "jnt:contentList");
            subNode4.addNode("testSubList4_1", "jnt:contentList");
            subNode4.addNode("testSubList4_2", "jnt:contentList");
            subNode4.addNode("testSubList4_3", "jnt:contentList");

            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrieveNodesBySql2Query() throws Exception {
        testQuery("select * from [jnt:contentList] where isdescendantnode('/testList')", GqlJcrQuery.QueryLanguage.SQL2, 7);
    }

    @Test
    public void shouldRetrieveNodesByXpathQuery() throws Exception {
        testQuery("/jcr:root/testList//element(*, jnt:contentList)", GqlJcrQuery.QueryLanguage.XPATH, 7);
    }

    @Test
    public void shouldGetErrorNotRetrieveNodesByWrongQuery() throws Exception {
        JSONObject result = runQuery("slct from [jnt:contentList]", GqlJcrQuery.QueryLanguage.SQL2);
        validateError(result, "javax.jcr.query.InvalidQueryException: Query:\nslct(*)from [jnt:contentList]; expected: SELECT");
    }

    private static JSONObject runQuery(String query, GqlJcrQuery.QueryLanguage language) throws JSONException {
        return executeQuery("{"
                          + "    jcr {"
                          + "    nodesByQuery(query: \"" + query + "\", queryLanguage: " + language.name() + ") {"
                          + "        edges {"
                          + "            node {"
                          + "                name"
                          + "            }"
                          + "		  }"
                          + "    }"
                          + "    }"
                          + "}");
    }

    private static void testQuery(String query, GqlJcrQuery.QueryLanguage language, long expectedNodesNumber) throws JSONException {
        JSONObject result = runQuery(query, language);
        JSONArray nodes = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByQuery").getJSONArray("edges");
        Assert.assertEquals(expectedNodesNumber, nodes.length());
    }
}
