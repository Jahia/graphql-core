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

package org.jahia.modules.graphql.security;

import graphql.language.Field;
import org.jahia.modules.graphql.provider.dxm.security.GqlAccessDeniedException;
import org.jahia.modules.graphql.provider.dxm.security.GqlJcrPermissionChecker;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.test.framework.AbstractJUnitTest;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GqlJcrPermissionCheckerTest extends AbstractJUnitTest {

    @Test
    public void testSimpleCheckPermission() throws Exception {

        // requested types and fields
        String[] types = new String[]{"JCRNode", "GenericJCRNode"};
        Field[] fields = new Field[]{new Field("getDisplayName")};

        Map<String, String> goodPermissions = new HashMap<>();
        goodPermissions.put("JCRNode.getDisplayName", "jcr:read");

        Map<String, String> badPermissions = new HashMap<>();
        badPermissions.put("JCRNode.getDisplayName", "jcr:write");

        checkPermission(types, fields, goodPermissions, false);
        checkPermission(types, fields, badPermissions, true);
    }

    @Test
    public void testPermissionWildcard() throws Exception {

        // requested types and fields
        String[] types = new String[]{"JCRNode", "GenericJCRNode"};
        Field[] getDisplayNameField = new Field[]{new Field("getDisplayName")};
        Field[] getPropertiesField = new Field[]{new Field("getProperties")};

        Map<String, String> permissions = new HashMap<>();
        permissions.put("JCRNode.*", "jcr:read");
        permissions.put("JCRNode.getDisplayName", "jcr:write");

        Map<String, String> permissions2 = new HashMap<>();
        permissions2.put("JCRNode.*", "jcr:write");
        permissions2.put("JCRNode.getDisplayName", "jcr:read");

        checkPermission(types, getPropertiesField, permissions, false);
        checkPermission(types, getDisplayNameField, permissions, true);
        checkPermission(types, getPropertiesField, permissions2, true);
        checkPermission(types, getDisplayNameField, permissions2, false);
    }

    @Test
    public void testPermissionInheritance() throws Exception {

        // requested types and fields
        // GenericJCRNode should inherit from JCRNode permissions
        String[] types = new String[]{"JCRNode", "GenericJCRNode"};
        Field[] getDisplayNameField = new Field[]{new Field("getDisplayName")};
        Field[] getPropertiesField = new Field[]{new Field("getProperties")};

        Map<String, String> permissions = new HashMap<>();
        permissions.put("JCRNode.*", "jcr:read");
        permissions.put("GenericJCRNode.getDisplayName", "jcr:write");

        Map<String, String> permissions2 = new HashMap<>();
        permissions2.put("JCRNode.*", "jcr:write");
        permissions2.put("GenericJCRNode.getDisplayName", "jcr:read");

        checkPermission(types, getPropertiesField, permissions, false);
        checkPermission(types, getDisplayNameField, permissions, true);
        checkPermission(types, getPropertiesField, permissions2, true);
        checkPermission(types, getDisplayNameField, permissions2, false);
    }

    @Test
    public void testPermissionOnMultipleFields() throws Exception {

        // requested types and fields
        String[] types = new String[]{"JCRNode", "GenericJCRNode"};
        Field[] multipleFields = new Field[]{new Field("getDisplayName"), new Field("getProperties")};

        Map<String, String> goodPermissions = new HashMap<>();
        goodPermissions.put("JCRNode.getProperties", "jcr:read");
        goodPermissions.put("GenericJCRNode.getDisplayName", "jcr:read");

        Map<String, String> badPermissions1 = new HashMap<>();
        badPermissions1.put("JCRNode.getProperties", "jcr:write");
        badPermissions1.put("GenericJCRNode.getDisplayName", "jcr:read");

        Map<String, String> badPermissions2 = new HashMap<>();
        badPermissions2.put("JCRNode.getProperties", "jcr:read");
        badPermissions2.put("GenericJCRNode.getDisplayName", "jcr:write");

        Map<String, String> badPermissions3 = new HashMap<>();
        badPermissions3.put("JCRNode.getProperties", "jcr:write");
        badPermissions3.put("GenericJCRNode.getDisplayName", "jcr:write");

        Map<String, String> badPermissions4 = new HashMap<>();
        badPermissions4.put("GenericJCRNode.getProperties", "jcr:write");
        badPermissions4.put("GenericJCRNode.getDisplayName", "jcr:write");

        Map<String, String> badPermissions5 = new HashMap<>();
        badPermissions5.put("JCRNode.*", "jcr:read");
        badPermissions5.put("GenericJCRNode.getProperties", "jcr:write");
        badPermissions5.put("GenericJCRNode.getDisplayName", "jcr:write");

        Map<String, String> badPermissions6 = new HashMap<>();
        badPermissions6.put("JCRNode.*", "jcr:write");
        badPermissions6.put("GenericJCRNode.getDisplayName", "jcr:read");

        checkPermission(types, multipleFields, goodPermissions, false);
        checkPermission(types, multipleFields, badPermissions1, true);
        checkPermission(types, multipleFields, badPermissions2, true);
        checkPermission(types, multipleFields, badPermissions3, true);
        checkPermission(types, multipleFields, badPermissions4, true);
        checkPermission(types, multipleFields, badPermissions5, true);
        checkPermission(types, multipleFields, badPermissions6, true);
    }

    private void checkPermission(String[] types, Field[] fields, Map<String, String> permissions, boolean shouldfail) throws RepositoryException {
        JCRTemplate.getInstance().doExecute(JahiaUserManagerService.GUEST_USERNAME, null, null, null, new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                try {
                    GqlJcrPermissionChecker.checkPermissions(Arrays.asList(types), Arrays.asList(fields), permissions, jcrSessionWrapper);
                    if (shouldfail) {
                        fail(" should fail ");
                    }
                } catch (GqlAccessDeniedException e) {
                    if (!shouldfail) {
                        fail(" should not fail ");
                    } else {
                        assertEquals(e.getPermission(), "jcr:write");
                    }
                }
                return null;
            }
        });
    }
}
