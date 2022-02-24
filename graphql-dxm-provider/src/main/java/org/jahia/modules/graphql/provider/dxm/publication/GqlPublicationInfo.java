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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.services.content.ComplexPublicationService;
import org.jahia.services.content.JCRTemplate;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Aggregated publication info about a JCR node.
 */
@GraphQLDescription("Publication status information for a JCR node")
public class GqlPublicationInfo {

    private ComplexPublicationService.AggregatedPublicationInfo aggregatedInfo;
    private GqlJcrNode node;

    public GqlPublicationInfo(ComplexPublicationService.AggregatedPublicationInfo aggregatedInfo, GqlJcrNode node) {
        this.aggregatedInfo = aggregatedInfo;
        this.node = node;
    }

    /**
     * @return Aggregated publication status of the node
     */
    @GraphQLField
    @GraphQLName("publicationStatus")
    @GraphQLNonNull
    @GraphQLDescription("Aggregated publication status of the node")
    public GqlPublicationStatus getPublicationStatus() {
        return GqlPublicationStatus.fromStatusValue(aggregatedInfo.getPublicationStatus());
    }

    /**
     * @return Aggregated locked status of the node
     */
    @GraphQLField
    @GraphQLName("locked")
    @GraphQLDescription("Aggregated locked status of the node")
    public boolean isLocked() {
        return aggregatedInfo.isLocked();
    }

    /**
     * @return Aggregated work-in-progress status of the node
     */
    @GraphQLField
    @GraphQLName("workInProgress")
    @GraphQLDescription("Aggregated work-in-progress status of the node")
    public boolean isWorkInProgress() {
        return aggregatedInfo.isWorkInProgress();
    }

    /**
     * @return Whether current user is allowed to publish the node omitting any workflows
     */
    @GraphQLField
    @GraphQLName("allowedToPublishWithoutWorkflow")
    @GraphQLDescription("Whether current user is allowed to publish the node omitting any workflows")
    public boolean isAllowedToPublishWithoutWorkflow() {
        return aggregatedInfo.isAllowedToPublishWithoutWorkflow();
    }

    /**
     * @return Whether node exists in live workspace
     */
    @GraphQLField
    @GraphQLName("existsInLive")
    @GraphQLDescription("Whether node exists in live workspace")
    public boolean existsInLive() {
        try {
            JCRTemplate.getInstance().getSessionFactory().getCurrentUserSession(Constants.LIVE_WORKSPACE).getNodeByIdentifier(node.getUuid());
            return true;
        } catch (ItemNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
