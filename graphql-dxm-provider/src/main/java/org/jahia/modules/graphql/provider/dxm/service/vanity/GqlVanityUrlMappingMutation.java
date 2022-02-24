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
    @GraphQLName("uuid")
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
            JCRNodeWrapper targetNode = vanityUrlNode.getSession().getNode(JCRContentUtils.escapeNodePath(target));

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
    @GraphQLName("nodeMutation")
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
