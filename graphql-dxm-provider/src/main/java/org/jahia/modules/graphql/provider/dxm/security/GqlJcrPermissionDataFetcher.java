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

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.services.content.JCRNodeWrapper;

import java.util.Map;

/**
 * DataFetcher used to check the permission on the GraphQL type and field.
 * @param <T> the type of object returned
 */
public class GqlJcrPermissionDataFetcher<T> implements DataFetcher<T> {

    private DataFetcher<T> originalDataFetcher;
    private Map<String, String> permissions;

    public GqlJcrPermissionDataFetcher(DataFetcher<T> originalDataFetcher, Map<String, String> permissions) {
        this.originalDataFetcher = originalDataFetcher;
        this.permissions = permissions;
    }

    @Override
    public T get(DataFetchingEnvironment environment) throws Exception {

        // check permission
        GqlJcrPermissionChecker.checkPermissions(environment.getParentType(), environment.getFields(), permissions);

        if (!PermissionHelper.hasPermission(null, environment)) {
            throw new GqlAccessDeniedException("Access denied");
        }

        // permission checked
        T res = originalDataFetcher.get(environment);

        if (res instanceof GqlJcrNode) {
            JCRNodeWrapper jcrNode = ((GqlJcrNode) res).getNode();
            if (!PermissionHelper.hasPermission(jcrNode, environment)) {
                throw new GqlAccessDeniedException("Access denied to the node: " + jcrNode.getPath());
            }
        }

        return res;
    }
}