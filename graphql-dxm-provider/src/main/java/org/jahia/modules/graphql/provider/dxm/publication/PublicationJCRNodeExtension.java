/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.*;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.ComplexPublicationService;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.RepositoryException;

/**
 * Publication extensions for the JCR node.
 *
 * These extensions can only be applied to a node from EDIT workspace, not LIVE.
 */
@GraphQLTypeExtension(GqlJcrNode.class)
public class PublicationJCRNodeExtension extends PublicationJCRExtensionSupport {

    private GqlJcrNode gqlJcrNode;

    /**
     * Create a publication extension instance.
     *
     * @param node JCR node representation to apply the extension to
     * @throws GqlJcrWrongInputException In case the parameter represents a node from LIVE rather than EDIT workspace
     */
    public PublicationJCRNodeExtension(GqlJcrNode node) throws GqlJcrWrongInputException {
        validateNodeWorkspace(node);
        this.gqlJcrNode = node;
    }

    /**
     * Retrieve aggregated publication info about the JCR node.
     *
     * @param language Publication language
     * @param subNodes Whether to take sub-nodes of the node into account when calculating the aggregated publication status
     * @param references Whether to take references into account when calculating the aggregated publication status
     * @return Aggregated publication info about the node
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Aggregated publication info about the JCR node")
    public GqlPublicationInfo getAggregatedPublicationInfo(
        @GraphQLName("language") @GraphQLNonNull @GraphQLDescription("Publication language") String language,
        @GraphQLName("subNodes") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLDescription("Whether to take sub-nodes into account when calculating the aggregated publication status") boolean subNodes,
        @GraphQLName("references") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLDescription("Whether to take references into account when calculating the aggregated publication status") boolean references
    ) {

        ComplexPublicationService publicationService = BundleUtils.getOsgiService(ComplexPublicationService.class, null);

        JCRSessionWrapper session;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession();
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }

        final ComplexPublicationService.AggregatedPublicationInfo aggregatedInfo = publicationService.getAggregatedPublicationInfo(gqlJcrNode.getUuid(), language, subNodes, references, session);

        return new GqlPublicationInfo(aggregatedInfo);
    }

    /**
     * Returns if the node supports publication
     *
     * @return  does the node supports publication
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("does the node supports publication")
    public boolean supportsPublication() {
        try {
            return JCRPublicationService.supportsPublication(gqlJcrNode.getNode().getSession(), gqlJcrNode.getNode());
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }
}
