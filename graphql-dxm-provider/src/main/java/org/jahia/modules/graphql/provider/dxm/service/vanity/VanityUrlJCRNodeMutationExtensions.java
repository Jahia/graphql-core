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