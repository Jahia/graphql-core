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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintViolationException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.VanityUrlManager;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import java.util.List;

@GraphQLName("VanityUrlMappingMutation")
public class GqlVanityUrlMappingMutation {

    private JCRNodeWrapper vanityUrlNode;
    private JCRNodeWrapper targetNode;
    private VanityUrlService vanityUrlService;
    private VanityUrlMutationService vanityUrlMutationService;

    /**
     * Create a vanity URL mutation extension instance.
     *
     * @param vanityUrlNode The vanity URL JCR node
     */
    public GqlVanityUrlMappingMutation(JCRNodeWrapper vanityUrlNode) {
        try {
            if (!vanityUrlNode.isNodeType(VanityUrlManager.JAHIANT_VANITYURL)) {
                throw new GqlJcrWrongInputException("Vanity URL field can only be used on vanity URL nodes, node: " + vanityUrlNode.getPath() + " is not a vanity URL node");
            }
            this.vanityUrlService = BundleUtils.getOsgiService(VanityUrlService.class, null);
            this.vanityUrlNode = vanityUrlNode;
            this.targetNode = vanityUrlNode.getParent().getParent();
            this.vanityUrlMutationService = new VanityUrlMutationService(targetNode, vanityUrlService);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Get the identifier of the node currently being mutated")
    public String getUuid() throws BaseGqlClientException {
        try {
            return vanityUrlNode.getIdentifier();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }


    /**
     * Update the vanity URL.
     *
     * @param active Desired value of the active flag or null to keep existing value
     * @param defaultMapping Desired value of the default flag or null to keep existing value
     * @param language Desired vanity URL language or null to keep existing value
     * @param url Desired URL value or null to keep existing value
     * @return Always true
     * @throws GqlConstraintViolationException In case the desired values violate a vanity URL uniqueness constraint
     */
    @GraphQLField
    @GraphQLDescription("Update vanity URL")
    public boolean update(@GraphQLName("active") @GraphQLDescription("Desired value of the active flag or null to keep existing value") Boolean active,
                          @GraphQLName("defaultMapping") @GraphQLDescription("Desired value of the default flag or null to keep existing value") Boolean defaultMapping,
                          @GraphQLName("language") @GraphQLDescription("Desired vanity URL language or null to keep existing value") String language,
                          @GraphQLName("url") @GraphQLDescription("Desired URL value or null to keep existing value") String url
    ) throws GqlConstraintViolationException {
        try {
            return vanityUrlMutationService.update(getVanityUrlObject(), active, defaultMapping, language, url);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Move the vanity URL to another node.
     *
     * @param target The target node
     * @return Always true
     */
    @GraphQLField
    @GraphQLDescription("Move the vanity URL to another node")
    public boolean move(@GraphQLName("target") @GraphQLNonNull @GraphQLDescription("The path of the target node") String target) {
        try {
            JCRNodeWrapper targetNode = vanityUrlNode.getSession().getNode(target);

            // add mixin if necessary
            if (!targetNode.isNodeType(VanityUrlManager.JAHIAMIX_VANITYURLMAPPED)) {
                targetNode.addMixin(VanityUrlManager.JAHIAMIX_VANITYURLMAPPED);
            }

            // get or create target mappings node
            JCRNodeWrapper targetMappings = targetNode.hasNode(VanityUrlManager.VANITYURLMAPPINGS_NODE) ?
                    targetNode.getNode(VanityUrlManager.VANITYURLMAPPINGS_NODE) :
                    targetNode.addNode(VanityUrlManager.VANITYURLMAPPINGS_NODE, VanityUrlManager.JAHIANT_VANITYURLS);

            // Do not move vanity already in place
            if (!vanityUrlNode.getPath().startsWith(targetMappings.getPath())) {

                // reset "default" property on moved vanity
                vanityUrlNode.setProperty(VanityUrlManager.PROPERTY_DEFAULT, false);
                vanityUrlNode.getSession().move(vanityUrlNode.getPath(), targetMappings.getPath() + "/" + JCRContentUtils.findAvailableNodeName(targetMappings, vanityUrlNode.getName()));

                // clean nodes if necessary
                // TODO clean, we can't clean because for now we use the mixin to found node that do not have vanity anymore in default but may be still in live
                // cleanVanityUrlParentNodes(sourceNode);
            }

            vanityUrlService.flushCaches();

            return true;
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Deletes the current vanity url.
     *
     * @return operation result
     * @throws BaseGqlClientException in case of an error during node delete operation
     */
    @GraphQLField
    @GraphQLDescription("Deletes the current vanity url")
    public boolean delete() {
        try {
            vanityUrlService.removeVanityUrlMapping(targetNode, getVanityUrlObject());

            return true;
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Get mutation on underlying node
     *
     * @return A node mutation object
     * @throws BaseGqlClientException in case of an error during node delete operation
     */
    @GraphQLField
    @GraphQLDescription("Get mutation on underlying node")
    public GqlJcrNodeMutation getNodeMutation() {
        return new GqlJcrNodeMutation(vanityUrlNode);
    }

    private VanityUrl getVanityUrlObject() throws RepositoryException {
        List<VanityUrl> vanityUrls = vanityUrlService.getVanityUrls(targetNode, null, vanityUrlNode.getSession());
        for (VanityUrl vanityUrl : vanityUrls) {
            if (vanityUrl.getIdentifier().equals(vanityUrlNode.getIdentifier())) {
                return vanityUrl;
            }
        }
        throw new IllegalStateException("Vanity URL node not found by UUID: " + vanityUrlNode.getIdentifier());
    }

}
