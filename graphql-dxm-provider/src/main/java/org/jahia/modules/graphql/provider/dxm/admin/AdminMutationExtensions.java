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
package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

import javax.jcr.RepositoryException;

/**
 * A mutation extension that adds admin query endpoint
 */
@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLDescription("A mutation extension that adds admin query endpoint")
public class AdminMutationExtensions {
    /**
     * Root for all Admin mutations.
     * @return GraphQL root object for Admin related mutations
     * @throws RepositoryException in case of JCR related errors
     */
    @GraphQLField
    @GraphQLName("admin")
    @GraphQLNonNull
    @GraphQLDescription("Admin Mutation")
    @GraphQLRequiresPermission(value = "jcr:read/jcr:system")
    public static GqlAdminMutation getAdmin() throws RepositoryException {
        return new GqlAdminMutation();
    }

}
