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
