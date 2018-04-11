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
import org.jahia.modules.graphql.provider.dxm.GqlConstraintViolationException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.NonUniqueUrlMappingException;
import org.jahia.services.seo.jcr.VanityUrlManager;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GraphQLName("VanityUrlMappingMutation")
public class GqlVanityUrlMappingMutation {

    private JCRNodeWrapper vanityUrlNode;
    private JCRNodeWrapper targetNode;
    private VanityUrlService vanityUrlService;

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
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
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
            VanityUrl vanityUrl = getVanityUrlObject();
            if (active != null) {
                vanityUrl.setActive(active);
            }
            if (defaultMapping != null) {
                vanityUrl.setDefaultMapping(defaultMapping);
            }
            if (language != null) {
                vanityUrl.setLanguage(language);
            }
            if (url != null) {
                vanityUrl.setUrl(url);
            }
            vanityUrlService.saveVanityUrlMapping(targetNode, vanityUrl, false);
            return true;
        } catch (NonUniqueUrlMappingException e) {
            Map<String,Object> extensions = new HashMap<>();
            extensions.put("type",e.getClass().getName());
            extensions.put("existingNodePath",e.getExistingNodePath());
            extensions.put("urlMapping",e.getUrlMapping());
            extensions.put("workspace",e.getWorkspace());
            extensions.put("nodePath",e.getNodePath());
            throw new GqlConstraintViolationException(e, extensions);
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

            BundleUtils.getOsgiService(VanityUrlService.class, null).flushCaches();

            return true;
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
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

    private void cleanVanityUrlParentNodes(JCRNodeWrapper vanityUrlParentNode) throws RepositoryException {
        if (vanityUrlParentNode.getNodes().getSize() == 0) {
            vanityUrlParentNode.getParent().removeMixin(VanityUrlManager.JAHIAMIX_VANITYURLMAPPED);
            vanityUrlParentNode.remove();
        }
    }
}
