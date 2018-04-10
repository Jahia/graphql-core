/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.service.vanity;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * A mutation extension that adds a possibility to modify Vanity URLs.
 */
@GraphQLTypeExtension(GqlJcrMutation.class)
@GraphQLDescription("A mutation extension that adds a possibility to modify Vanity URLs")
public class VanityUrlJCRMutationExtensions extends GqlJcrMutationSupport {

    private GqlJcrMutation mutation;

    public VanityUrlJCRMutationExtensions(GqlJcrMutation mutation) {
        this.mutation = mutation;
    }

    /**
     * Mutate multiple vanities at the same time
     * @param pathsOrIds paths or ids of vanities node to mutate
     * @return the Mutation
     * @throws GqlJcrWrongInputException In case the parameter is not a vanity node
     */
    @GraphQLField
    @GraphQLDescription("Vanity URL Mutation")
    public Collection<GqlVanityUrlMappingMutation> mutateVanityUrls(@GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("The paths or ids of the vanity urls to mutate") Collection<String> pathsOrIds) throws GqlJcrWrongInputException {
        return new HashSet<>(pathsOrIds).stream().map((pathOrId) -> new GqlVanityUrlMappingMutation(getNodeFromPathOrId(mutation.getSession(), pathOrId))).collect(Collectors.toList());
    }
}
