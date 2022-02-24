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
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintViolationException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@GraphQLTypeExtension(GqlJcrNodeMutation.class)
@GraphQLName("VanityUrlJCRNodeMutationExtensions")
public class VanityUrlJCRNodeMutationExtensions {

    private JCRNodeWrapper node;
    private VanityUrlService vanityUrlService;
    private VanityUrlMutationService vanityUrlMutationService;

    /**
     * Initializes an instance of this class.
     *
     * @param node the corresponding GraphQL node
     */
    public VanityUrlJCRNodeMutationExtensions(GqlJcrNodeMutation node) {
        this.vanityUrlService = BundleUtils.getOsgiService(VanityUrlService.class, null);
        this.node = node.getNode().getNode();
        this.vanityUrlMutationService = new VanityUrlMutationService(this.node, vanityUrlService);
    }

    /**
     * Add the vanity URL.
     *
     * @param vanityUrlInputList list of vanity urls to create
     * @return Always true
     * @throws GqlConstraintViolationException In case the desired values violate a vanity URL uniqueness constraint
     */
    @GraphQLField
    @GraphQLDescription("Add vanity URL")
    @GraphQLName("addVanityUrl")
    public Collection<GqlVanityUrlMappingMutation>  addVanityUrl(@GraphQLName("vanityUrlInputList") @GraphQLNonNull @GraphQLDescription("The list of vanity url to create") List<GqlJcrVanityUrlInput> vanityUrlInputList) throws GqlConstraintViolationException {
        vanityUrlMutationService.add(vanityUrlInputList);

        try {
            JCRSessionWrapper session = node.getSession();
            return vanityUrlService.getVanityUrls(node, null, session).stream()
                    .filter(u -> vanityUrlInputList.stream().anyMatch(input-> u.getUrl().equals(input.getUrl())))
                    .map(u -> new GqlVanityUrlMappingMutation(GqlJcrMutationSupport.getNodeFromPathOrId(session, u.getIdentifier())))
                    .collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Update a vanity URL
     */
    @GraphQLField
    @GraphQLDescription("Update a vanity URL")
    @GraphQLName("mutateVanityUrl")
    public Optional<GqlVanityUrlMappingMutation> mutateVanityUrl(@GraphQLName("url") @GraphQLNonNull @GraphQLDescription("The url to edit") String url) {
        try {
            JCRSessionWrapper session = node.getSession();
            return vanityUrlService.getVanityUrls(node, null, session).stream()
                    .filter(u -> url.equals(u.getUrl()))
                    .map(u -> new GqlVanityUrlMappingMutation(GqlJcrMutationSupport.getNodeFromPathOrId(session, u.getIdentifier())))
                    .findFirst();
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Update vanity URLs
     */
    @GraphQLField
    @GraphQLDescription("Update vanity URLs")
    @GraphQLName("mutateVanityUrls")
    public Collection<GqlVanityUrlMappingMutation> mutateVanityUrls(@GraphQLName("languages") @GraphQLDescription("Filter by languages") Collection<String> languages) throws GqlJcrWrongInputException {
        try {
            JCRSessionWrapper session = node.getSession();
            return vanityUrlService.getVanityUrls(node, null, session).stream()
                    .filter(u -> (languages == null || languages.contains(u.getLanguage())))
                    .map(u -> new GqlVanityUrlMappingMutation(GqlJcrMutationSupport.getNodeFromPathOrId(session, u.getIdentifier()))).collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

}