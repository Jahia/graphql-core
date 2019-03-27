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
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.touk.throwing.ThrowingPredicate;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.stream.Collectors;

public class GraphQLPermissionsTest extends GraphQLTestSupport {

    private static JahiaUser user;
    private static JahiaUser currentUserBackup;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {

            user = JahiaUserManagerService.getInstance().createUser("testUser", null, "testPassword", new Properties(), session).getJahiaUser();
            JahiaGroupManagerService.getInstance().lookupGroup(null, "privileged", session).addMember(user);


            JCRNodeWrapper node = addTestNodeWithUserRoles(session.getNode("/"), "jnt:contentList", "testList", user, true);
            addTestNodeWithUserRoles(node, "jnt:contentList", "testSubList1", user, false);
            JCRNodeWrapper subNode2 = addTestNodeWithUserRoles(node, "jnt:contentList", "testSubList2", user, true);
            JCRNodeWrapper ref1 = addTestNodeWithUserRoles(node, "jnt:contentReference", "reference1", user, false);
            JCRNodeWrapper ref2 = addTestNodeWithUserRoles(node, "jnt:contentReference", "reference2", user, true);
            ref1.setProperty("j:node", subNode2);
            ref2.setProperty("j:node", subNode2);

            session.save();
            return null;
        });

        JCRSessionFactory sessionFactory = JCRSessionFactory.getInstance();
        currentUserBackup = sessionFactory.getCurrentUser();
        sessionFactory.setCurrentUser(user);
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {

        JCRSessionFactory.getInstance().setCurrentUser(currentUserBackup);
        GraphQLTestSupport.removeTestNodes();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JahiaUserManagerService.getInstance().deleteUser(user.getLocalPath(), session);
            session.save();
            return null;
        });
    }

    private static JCRNodeWrapper addTestNodeWithUserRoles(JCRNodeWrapper parent, String type, String name, JahiaUser user, boolean grantOwnerRole) throws RepositoryException {

        JCRNodeWrapper node = parent.addNode(name, type);
        node.setAclInheritanceBreak(true);
        String principalKey = "u:" + user.getUsername();
        node.grantRoles(principalKey, Collections.singleton("privileged"));
        if (grantOwnerRole) {
            node.grantRoles(principalKey, Collections.singleton("owner"));
        }

        return node;
    }


    @Test
    public void shouldGetErrorNotRetrieveProtectedNode() throws Exception {

        JSONObject result = executeQuery(""
                + "{"
                + "    jcr {"
                + "        nodeByPath(path: \"/testList/testSubList1\") {"
                + "            uuid"
                + "        }"
                + "    }"
                + "}"
        );

        validateError(result, "Permission denied");
    }

    @Test
    public void shouldRetrieveFilteredChildNodes() throws Exception {

        JSONObject result = executeQuery(""
                + "{"
                + "    jcr {"
                + "        nodeByPath(path: \"/testList\") {"
                + "            children {"
                + "                nodes {"
                + "                    name"
                + "                }"
                + "            }"
                + "        }"
                + "    }"
                + "}"
        );

        List<JSONObject> children = getList(result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("children").getJSONArray("nodes"));
        Assert.assertEquals(2, children.size());
        validateNode(children.get(0), "testSubList2");
        validateNode(children.get(1), "reference2");
    }

    private List<JSONObject> getList(JSONArray children) throws JSONException {
        List<JSONObject> listdata = new ArrayList<>();
        if (children != null) {
            for (int i = 0; i < children.length(); i++) {
                listdata.add(children.getJSONObject(i));
            }
        }
        return listdata.stream().filter(ThrowingPredicate.unchecked(o -> !o.getString("name").equals("j:acl") && !o.getString("name").startsWith("GRANT"))).collect(Collectors.toList());
    }

    @Test
    public void shouldRetrieveFilteredDescendantNodes() throws Exception {

        JSONObject result = executeQuery(""
                + "{"
                + "    jcr {"
                + "        nodeByPath(path: \"/testList\") {"
                + "            descendants {"
                + "                nodes {"
                + "                    name"
                + "                }"
                + "            }"
                + "        }"
                + "    }"
                + "}"
        );

        List<JSONObject> descendants = getList(result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("descendants").getJSONArray("nodes"));
        Assert.assertEquals(2, descendants.size());
        validateNode(descendants.get(0), "testSubList2");
        validateNode(descendants.get(1), "reference2");
    }

    @Test
    public void shouldRetrieveFilteredReferences() throws Exception {

        JSONObject result = executeQuery(""
                + "{"
                + "    jcr {"
                + "        nodeByPath(path: \"/testList/testSubList2\") {"
                + "            references {"
                + "                nodes {"
                + "                    node {"
                + "                        name"
                + "                    }"
                + "                }"
                + "            }"
                + "        }"
                + "    }"
                + "}"
        );

        JSONArray references = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONObject("references").getJSONArray("nodes");
        Assert.assertEquals(1, references.length());
        validateNode(references.getJSONObject(0).getJSONObject("node"), "reference2");
    }
}
