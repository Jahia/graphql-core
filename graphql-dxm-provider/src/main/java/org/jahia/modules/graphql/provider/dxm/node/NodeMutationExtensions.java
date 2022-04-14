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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;

import javax.jcr.RepositoryException;

/**
 * A mutation extension that adds a possibility to modify JCR nodes.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLDescription("A mutation extension that adds a possibility to modify JCR nodes")
public class NodeMutationExtensions {

    /**
     * Root for all JCR mutations.
     * 
     * @param workspace the name of the workspace to fetch the node from; either 'edit', 'live', or null to use 'edit' by default
     * @return GraphQL root object for JCR related mutations
     * @throws RepositoryException in case of JCR related errors
     */
    @GraphQLField
    @GraphQLName("jcr")
    @GraphQLDescription("JCR Mutation")
    public static GqlJcrMutation getJcr(@GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'edit', 'live', or null to use 'edit' by default") NodeQueryExtensions.Workspace workspace,
                                        @GraphQLName("save") @GraphQLDescription("Should save") @GraphQLDefaultValue(GqlUtils.SupplierTrue.class) boolean save) throws RepositoryException {
        return new GqlJcrMutation(workspace != null ? workspace.getValue() : null, save);
    }
}
