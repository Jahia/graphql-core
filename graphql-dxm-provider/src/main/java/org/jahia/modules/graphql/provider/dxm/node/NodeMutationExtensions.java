/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
