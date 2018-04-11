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
import org.jahia.services.content.JCRNodeWrapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
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
     * Mutate multiple vanity URLs.
     *
     * @param pathsOrIds Paths or UUIDs of vanity URL nodes to mutate
     * @return A collection of mutations, one per vanity URL node
     * @throws GqlJcrWrongInputException In case a path/UUID from the parameter collection does not represent a vanity URL node
     */
    @GraphQLField
    @GraphQLDescription("Vanity URL Mutation")
    public Collection<GqlVanityUrlMappingMutation> mutateVanityUrls(@GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("Paths or UUIDs of vanity URL nodes to mutate") Collection<String> pathsOrIds) throws GqlJcrWrongInputException {

        return new HashSet<>(pathsOrIds).stream().map(new Function<String, GqlVanityUrlMappingMutation>() {

            @Override
            public GqlVanityUrlMappingMutation apply(String pathOrId) {
                JCRNodeWrapper node = getNodeFromPathOrId(mutation.getSession(), pathOrId);
                return new GqlVanityUrlMappingMutation(node);
            }

        }).collect(Collectors.toList());
    }
}
