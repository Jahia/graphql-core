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
package org.jahia.modules.graphql.provider.dxm.security;

import graphql.language.Field;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Permission checker can handle permission checks on graphQL types and fields
 */
public class GqlJcrPermissionChecker {

    /**
     * Check the permissions on the given graphQL type and fields
     * @param type the GraphQL type
     * @param fields the GraphQL fields
     * @param permissions the map of all the permissions ( key format: {type.field}, value format: {permission} )
     * @throws GqlAccessDeniedException in case of permission denied
     */
    public static void checkPermissions(final GraphQLType type, final List<Field> fields, final Map<String, String> permissions) {
        if (permissions == null || permissions.size() == 0 || StringUtils.equals(type.getName(), "JCRNodeConnection")) {
            // if no permissions configured or the current type is a connection ( because the parent type have already be checked )
            return;
        }

        List<String> types = resolveTypes(type);

        try {
            checkPermissions(types, fields, permissions, JCRSessionFactory.getInstance().getCurrentUserSession());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Check the permissions on the given types and fields
     * @param types the types, primary type should be the latest in the list to support inheritance of permission properties
     * @param fields the fields
     * @param permissions the map of all the permissions ( key format: {type.field}, value format: {permission} )
     * @param session current JCR session instance
     * @throws RepositoryException in case of JCR-related errors
     * @throws GqlAccessDeniedException in case of permission denied
     */
    public static void checkPermissions(final List<String> types, final List<Field> fields, final Map<String, String> permissions, JCRSessionWrapper session) throws RepositoryException {
        Map<String, String> requiredPermissionPerFields = resolvePermissionPerFields(types, fields, permissions);

        if (requiredPermissionPerFields.size() > 0) {
            // iterate on permissions to check
            for (Map.Entry<String, String> requiredPermissionForField : requiredPermissionPerFields.entrySet()) {

                if (!session.getNode("/").hasPermission(requiredPermissionForField.getValue())) {
                    throw new GqlAccessDeniedException(requiredPermissionForField.getValue());
                }
            }
        }
    }

    private static List<String> resolveTypes(GraphQLType type) {
        // resolved types
        List<String> types = new ArrayList<>();

        // resolve super types
        if (type instanceof GraphQLObjectType) {
            GraphQLObjectType objectType = (GraphQLObjectType) type;
            if (objectType.getInterfaces().size() > 0) {
                for (GraphQLOutputType graphQLOutputType : objectType.getInterfaces()) {
                    types.add(graphQLOutputType.getName());
                }
            }
        }

        // add exact type at the end of the list
        types.add(type.getName());

        return types;
    }

    private static Map<String, String> resolvePermissionPerFields(List<String> types, List<Field> fields, Map<String, String> permissions) {
        // resolved permissions
        Map<String, String> requiredPermissionForFields = new HashMap<>();

        for (Field field : fields) {
            if (field != null) {
                String permission = getPermissionForTypes(types, field.getName(), permissions);

                // fallback on "*" wildcard
                if (permission == null) {
                    permission = getPermissionForTypes(types, "*", permissions);
                }

                if (permission != null) {
                    requiredPermissionForFields.put(field.getAlias() != null ? field.getAlias() : field.getName(), permission);
                }
            }
        }

        return requiredPermissionForFields;
    }

    private static String getPermissionForTypes(List<String> types, String field, Map<String, String> permissions) {
        String permission = null;

        // iterate on types from interfaces to exact type precision. Exact type will always have the priority
        for (String type : types) {
            String permissionKey = type + "." + field;

            if (permissions.containsKey(permissionKey)) {
                permission = permissions.get(permissionKey);
            }
        }

        return permission;
    }
}
