package org.jahia.modules.graphql.provider.dxm.security;

import graphql.ErrorType;
import graphql.language.Field;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.services.content.JCRSessionFactory;

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
     * throw
     */
    public static void checkPermissions(final GraphQLType type, final List<Field> fields, final Map<String, String> permissions) {
        if (permissions == null || permissions.size() == 0 || StringUtils.equals(type.getName(), "JCRNodeConnection")) {
            // if no permissions configured or the current type is a connection ( because the parent type have already be checked )
            return;
        }

        List<String> types = resolveTypes(type);
        Map<String, String> requiredPermissionPerFields = resolvePermissionPerFields(types, fields, permissions);

        if (requiredPermissionPerFields.size() > 0) {
            // iterate on permissions to check
            for (Map.Entry<String, String> requiredPermissionForField : requiredPermissionPerFields.entrySet()) {

                try {
                    if (!JCRSessionFactory.getInstance().getCurrentUserSession().getNode("/").hasPermission(requiredPermissionForField.getValue())) {
                        throw new GqlAccessDeniedException();
                    }
                } catch (RepositoryException e) {
                    throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
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
            String permission = getPermissionForTypes(types, field.getName(), permissions);

            // fallback on "*" wildcard
            if (permission == null) {
                permission = getPermissionForTypes(types, "*", permissions);
            }

            if (permission != null) {
                requiredPermissionForFields.put(field.getName(), permission);
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
