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

import org.apache.commons.lang.ArrayUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.Base64.getEncoder;

public class GraphQLConnectionsTest extends GraphQLTestSupport {

    private static String subNodeCursor1;
    private static String subNodeCursor2;
    private static String subNodeCursor3;
    private static String subNodeCursor4;
    private static String subNodeCursor5;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            JCRNodeWrapper subNode = node.addNode("testSubList", "jnt:contentList");
            subNodeCursor1 = encodeCursor(subNode.addNode("testSubSubList1", "jnt:contentList").getIdentifier());
            subNodeCursor2 = encodeCursor(subNode.addNode("testSubSubList2", "jnt:contentList").getIdentifier());
            subNodeCursor3 = encodeCursor(subNode.addNode("testSubSubList3", "jnt:contentList").getIdentifier());
            subNodeCursor4 = encodeCursor(subNode.addNode("testSubSubList4", "jnt:contentList").getIdentifier());
            subNodeCursor5 = encodeCursor(subNode.addNode("testSubSubList5", "jnt:contentList").getIdentifier());
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrieveNodesUsingOffsetAndLimit() throws Exception {
        JSONObject result = executeQuery(getQuery(null, null, null, null, 1, 2));
        insureConnectionResult(result, 5, 2, subNodeCursor2, subNodeCursor3, true, true);

        result = executeQuery(getQuery(null, null, null, null, 2, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, null, null, 2));
        insureConnectionResult(result, 5, 2, subNodeCursor1, subNodeCursor2, true, false);

        result = executeQuery(getQuery(null, null, null, null, 0, 1));
        insureConnectionResult(result, 5, 1, subNodeCursor1, subNodeCursor1, true, false);

        result = executeQuery(getQuery(null, null, null, null, -15, 1));
        validateError(result, "Argument 'offset' can't be negative");

        result = executeQuery(getQuery(null, null, null, null, 4, 1));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, null, 4, 5000));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, null, 6, 1));
        insureConnectionResult(result, 5, 0, "null", "null", false, true);

        result = executeQuery(getQuery(null, null, null, null, 1, -1));
        validateError(result, "Argument 'limit' can't be negative");
    }

    @Test
    public void shouldRetrieveNodesUsingCursor() throws Exception {

        // test before
        JSONObject result = executeQuery(getQuery(subNodeCursor4, null, null, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor1, subNodeCursor3, true, false);

        result = executeQuery(getQuery(subNodeCursor1, null, null, null, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", true, false);

        result = executeQuery(getQuery(subNodeCursor5, null, null, null, null, null));
        insureConnectionResult(result, 5, 4, subNodeCursor1, subNodeCursor4, true, false);

        result = executeQuery(getQuery("wrong_cursor", null, null, null, null, null));
        insureConnectionResult(result, 5, 5, subNodeCursor1, subNodeCursor5, false, false);

        // test after
        result = executeQuery(getQuery(null, subNodeCursor2, null, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, subNodeCursor5, null, null, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", false, true);

        result = executeQuery(getQuery(null, subNodeCursor1, null, null, null, null));
        insureConnectionResult(result, 5, 4, subNodeCursor2, subNodeCursor5, false, true);

        result = executeQuery(getQuery("wrong_cursor", null, null, null, null, null));
        insureConnectionResult(result, 5, 5, subNodeCursor1, subNodeCursor5, false, false);

        // test first
        result = executeQuery(getQuery(null, null, 3, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor1, subNodeCursor3, true, false);

        result = executeQuery(getQuery(null, null, 1, null, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor1, subNodeCursor1, true, false);

        result = executeQuery(getQuery(null, null, 40, null, null, null));
        insureConnectionResult(result, 5, 5, subNodeCursor1, subNodeCursor5, false, false);

        result = executeQuery(getQuery(null, null, -1, null, null, null));
        validateError(result, "Argument 'first' can't be negative");

        // test last
        result = executeQuery(getQuery(null, null, null, 3, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, 1, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, 40, null, null));
        insureConnectionResult(result, 5, 5, subNodeCursor1, subNodeCursor5, false, false);

        result = executeQuery(getQuery(null, null, null, -1, null, null));
        validateError(result, "Argument 'last' can't be negative");

        // test before + first
        result = executeQuery(getQuery(subNodeCursor4, null, 2, null, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor1, subNodeCursor2, true, false);

        result = executeQuery(getQuery(subNodeCursor4, null, 10, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor1, subNodeCursor3, true, false);

        result = executeQuery(getQuery(subNodeCursor4, null, -4, null, null, null));
        validateError(result, "Argument 'first' can't be negative");

        result = executeQuery(getQuery(subNodeCursor1, null, 2, null, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", true, false);

        result = executeQuery(getQuery("wrong_cursor", null, 2, null, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor1, subNodeCursor2, true, false);

        // test before + last
        result = executeQuery(getQuery(subNodeCursor4, null, null, 2, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor2, subNodeCursor3, true, true);

        result = executeQuery(getQuery(subNodeCursor4, null, null, 10, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor1, subNodeCursor3, true, false);

        result = executeQuery(getQuery(subNodeCursor4, null, null, -5, null, null));
        validateError(result, "Argument 'last' can't be negative");

        result = executeQuery(getQuery(subNodeCursor1, null, null, 3, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", true, false);

        result = executeQuery(getQuery("wrong_cursor", null, null, 2, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor4, subNodeCursor5, false, true);

        // test after + first
        result = executeQuery(getQuery(null, subNodeCursor2, 2, null, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor3, subNodeCursor4, true, true);

        result = executeQuery(getQuery(null, subNodeCursor2, 5, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, subNodeCursor2, -4, null, null, null));
        validateError(result, "Argument 'first' can't be negative");

        result = executeQuery(getQuery(null, subNodeCursor5, 2, null, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", false, true);

        result = executeQuery(getQuery(null, "wrong_cursor", 2, null, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", false, true);

        // test after + last
        result = executeQuery(getQuery(null, subNodeCursor2, null, 2, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor4, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, subNodeCursor2, null, 4, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, subNodeCursor2, null, -3, null, null));
        validateError(result, "Argument 'last' can't be negative");

        result = executeQuery(getQuery(null, subNodeCursor5, null, 2, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", false, true);

        result = executeQuery(getQuery(null, "wrong_cursor", null, 2, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", false, true);

        // test after + before
        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, null, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor2, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor4, subNodeCursor4, null, null, null, null));
        // only the after is apply, regarding relay spec, it's normal because after is applied first
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(subNodeCursor2, subNodeCursor3, null, null, null, null));
        // only the after is apply, regarding relay spec, it's normal because after is applied first
        insureConnectionResult(result, 5, 2, subNodeCursor4, subNodeCursor5, false, true);

        result = executeQuery(getQuery("wrong_cursor", subNodeCursor1, null, null, null, null));
        insureConnectionResult(result, 5, 4, subNodeCursor2, subNodeCursor5, false, true);

        result = executeQuery(getQuery(subNodeCursor5, "wrong_cursor", null, null, null, null));
        insureConnectionResult(result, 5, 0, "null", "null", false, true);

        // test after + before + last
        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, null, 2, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor3, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, null, 10, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor2, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor3, subNodeCursor3, null, 1, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(subNodeCursor2, subNodeCursor3, null, 1, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        // test after + before + first
        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, 2, null, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor2, subNodeCursor3, true, true);

        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, 10, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor2, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor3, subNodeCursor3, 1, null, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor4, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor2, subNodeCursor3, 1, null, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor4, subNodeCursor4, true, true);
    }

    @Test
    public void shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination() throws Exception {
        List<boolean[]> cursorPossibilities = getArrayOfPossibilitiesWithAtLeastOneTrue(4);
        List<boolean[]> offsetLimitPossibilties = getArrayOfPossibilitiesWithAtLeastOneTrue(2);

        // calculate all the possibilities of mixed cursor + offsetLimit arguments
        for (boolean[] offsetLimitPossibilty : offsetLimitPossibilties) {
            for (boolean[] cursorPossibility : cursorPossibilities) {
                String before = cursorPossibility[0] ? subNodeCursor5 : null;
                String after = cursorPossibility[1] ? subNodeCursor1 : null;
                Integer first = cursorPossibility[2] ? 2 : null;
                Integer last = cursorPossibility[3] ? 2 : null;
                Integer offset = offsetLimitPossibilty[0] ? 2 : null;
                Integer limit = offsetLimitPossibilty[1] ? 2 : null;

                JSONObject result = executeQuery(getQuery(before, after, first, last, offset, limit));
                validateError(result, "Offset and/or Limit argument(s) can't be used with other pagination arguments");
            }
        }
    }

    private void insureConnectionResult(JSONObject result, long expextedTotalCount, long expectedNodesCount, String expectedStartCursor, String expectedEndCursor, boolean expectedHasNextPage, boolean expextedHasPreviousPage) throws Exception {
        JSONObject pageInfo = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodesByQuery").getJSONObject("pageInfo");
        Assert.assertEquals(expextedTotalCount, pageInfo.getLong("totalCount"));
        Assert.assertEquals(expectedNodesCount, pageInfo.getLong("nodesCount"));
        Assert.assertEquals(expectedStartCursor, pageInfo.getString("startCursor"));
        Assert.assertEquals(expectedEndCursor, pageInfo.getString("endCursor"));
        Assert.assertEquals(expectedHasNextPage, pageInfo.getBoolean("hasNextPage"));
        Assert.assertEquals(expextedHasPreviousPage, pageInfo.getBoolean("hasPreviousPage"));
    }

    private String getQuery(String before, String after, Integer first, Integer last, Integer offset, Integer limit) {
        StringBuilder queryBuilder = new StringBuilder("{"
                + "    jcr {"
                + "         nodesByQuery(query : \"Select * from [jnt:contentList] as cl where isdescendantnode(cl, ['/testList/testSubList']) order by cl.[j:nodename]\"");

        if (before != null) {
            queryBuilder.append(", before: \"").append(before).append("\"");
        }
        if (after != null) {
            queryBuilder.append(", after: \"").append(after).append("\"");
        }
        if (first != null) {
            queryBuilder.append(", first: ").append(first);
        }
        if (last != null) {
            queryBuilder.append(", last: ").append(last);
        }
        if (offset != null) {
            queryBuilder.append(", offset: ").append(offset);
        }
        if (limit != null) {
            queryBuilder.append(", limit: ").append(limit);
        }

        return queryBuilder.append(") {"
                + "             edges {"
                + "                 index,"
                + "                 cursor,"
                + "                 node {"
                + "                     name,"
                + "                     uuid"
                + "                 }"
                + "		        },"
                + "             pageInfo {"
                + "                 totalCount,"
                + "                 nodesCount,"
                + "                 startCursor,"
                + "                 endCursor,"
                + "                 hasNextPage,"
                + "                 hasPreviousPage,"
                + "		        },"
                + "             nodes {"
                + "                 name,"
                + "                 uuid"
                + "		        },"
                + "         }"
                + "    }"
                + "}").toString();
    }

    public static String encodeCursor(String s) {
        return getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    public static List<boolean[]> getArrayOfPossibilitiesWithAtLeastOneTrue(int n) {

        List<boolean[]> possibilites = new ArrayList<>();

        for (int i = 0; i < Math.pow(2, n); i++) {
            String bin = Integer.toBinaryString(i);
            while (bin.length() < n)
                bin = "0" + bin;
            char[] chars = bin.toCharArray();
            boolean[] boolArray = new boolean[n];
            for (int j = 0; j < chars.length; j++) {
                boolArray[j] = chars[j] == '0' ? true : false;
            }

            if(ArrayUtils.contains(boolArray, true)) {
                possibilites.add(boolArray);
            }

        }

        return possibilites;
    }
}
