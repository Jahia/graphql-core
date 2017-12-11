package org.jahia.modules.graphql.provider.dxm.security;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.Map;

/**
 * DataFetcher used to check the permission on the GraphQL type and field
 * @param <T>
 */
public class GqlJcrPermissionDataFetcher<T> implements DataFetcher<T> {

    private DataFetcher<T> originalDataFetcher;
    private Map<String, String> permissions;

    public GqlJcrPermissionDataFetcher(DataFetcher<T> originalDataFetcher, Map<String, String> permissions) {
        this.originalDataFetcher = originalDataFetcher;
        this.permissions = permissions;
    }

    @Override
    public T get(DataFetchingEnvironment environment) {
        // check permission
        GqlJcrPermissionChecker.checkPermissions(environment.getParentType(), environment.getFields(), permissions);

        // permission checked
        return originalDataFetcher.get(environment);
    }
}