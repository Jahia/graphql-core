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
