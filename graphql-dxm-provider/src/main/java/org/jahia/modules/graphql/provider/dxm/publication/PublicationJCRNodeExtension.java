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
package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.*;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.ComplexPublicationService;
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

        return new GqlPublicationInfo(aggregatedInfo, gqlJcrNode);
    }

}
