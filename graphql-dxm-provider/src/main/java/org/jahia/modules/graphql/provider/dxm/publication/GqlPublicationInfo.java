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
