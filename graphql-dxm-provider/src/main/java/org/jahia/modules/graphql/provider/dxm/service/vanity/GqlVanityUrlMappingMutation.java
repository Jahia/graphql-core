/**
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
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.seo.jcr.VanityUrlManager;

import javax.jcr.RepositoryException;

@GraphQLName("VanityUrlMappingMutation")
public class GqlVanityUrlMappingMutation {

    private JCRNodeWrapper vanityUrlNode;

    /**
     * Create a vanity url mutation extension instance.
     *
     * @param vanityUrlNode The JCR vanity url node
     */
    public GqlVanityUrlMappingMutation(JCRNodeWrapper vanityUrlNode) {
        this.vanityUrlNode = vanityUrlNode;
    }

    /**
     * Move the vanity url to the given targeted node
     * @param target the target node
     * @return "true" in case the move of the vanity url succeeded
     */
    @GraphQLField
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
            }

            return true;
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }
}
