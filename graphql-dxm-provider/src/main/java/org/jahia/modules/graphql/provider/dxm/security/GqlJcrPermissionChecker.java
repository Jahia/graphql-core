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
package org.jahia.modules.graphql.provider.dxm.security;

import graphql.language.Field;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.PathNotFoundException;
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

                String path = "/";
                String perm = requiredPermissionForField.getValue();
                if (perm.contains("/")) {
                    path = "/" + StringUtils.substringAfter(perm, "/");
                    perm = StringUtils.substringBefore(perm, "/");
                }
                try {
                    if (!session.getNode(path).hasPermission(perm)) {
                        throw new GqlAccessDeniedException(perm);
                    }
                } catch (PathNotFoundException e) {
                    throw new GqlAccessDeniedException(perm);
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
