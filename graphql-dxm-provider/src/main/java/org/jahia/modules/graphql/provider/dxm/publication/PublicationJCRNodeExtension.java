/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.ajax.gwt.client.data.publication.GWTJahiaPublicationInfo;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.services.content.*;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Set;

/**
 * Extensions for JCRNode
 */
@GraphQLTypeExtension(GqlJcrNode.class)
public class PublicationJCRNodeExtension {

    private GqlJcrNode gqlJcrNode;

    public PublicationJCRNodeExtension(GqlJcrNode node) {
        this.gqlJcrNode = node;
    }

    @GraphQLField
    public GqlPublicationInfo getAggregatedPublicationInfo(@GraphQLName("language") String language,
                                                           @GraphQLName("includesSubNodes") Boolean includesSubNodes,
                                                           @GraphQLName("includesReferences") Boolean includesReferences) {
        try {
            JCRPublicationService publicationService = JCRPublicationService.getInstance();
            if (includesSubNodes == null) {
                includesSubNodes = false;
            }
            if (includesReferences == null) {
                includesReferences = false;
            }
            JCRNodeWrapper node = gqlJcrNode.getNode();
            PublicationInfo pubInfo = publicationService.getPublicationInfo(node.getIdentifier(), Collections.singleton(language), includesReferences, includesSubNodes, false, Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE).get(0);
            if (!includesSubNodes) {
                // We don't include subnodes, but we still need the translation nodes to get the correct status
                final JCRSessionWrapper unlocalizedSession = JCRSessionFactory.getInstance().getCurrentUserSession();

                final JCRNodeWrapper nodeByIdentifier = unlocalizedSession.getNodeByIdentifier(node.getIdentifier());
                String langNodeName = "j:translation_" + language;
                if (nodeByIdentifier.hasNode(langNodeName)) {
                    JCRNodeWrapper next = nodeByIdentifier.getNode(langNodeName);
                    PublicationInfo translationInfo = publicationService.getPublicationInfo(next.getIdentifier(), Collections.singleton(language), includesReferences, false, false, Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE).get(0);
                    pubInfo.getRoot().addChild(translationInfo.getRoot());
                }
            }

            GqlPublicationInfo gqlPublicationInfo = new GqlPublicationInfo(pubInfo.getRoot().getUuid(), GqlPublicationStatus.fromValue(pubInfo.getRoot().getStatus()));

            String translationNodeName = pubInfo.getRoot().getChildren().size() > 0 ? "/j:translation_"+language : null;
            for (PublicationInfoNode sub : pubInfo.getRoot().getChildren()) {
                if (sub.getPath().contains(translationNodeName)) {
                    if (sub.getStatus() > gqlPublicationInfo.getStatus().getValue()) {
                        gqlPublicationInfo.setStatus(GqlPublicationStatus.fromValue(sub.getStatus()));
                    }
                    if (gqlPublicationInfo.getStatus().getValue() == GWTJahiaPublicationInfo.UNPUBLISHED && sub.getStatus() != GWTJahiaPublicationInfo.UNPUBLISHED) {
                        gqlPublicationInfo.setStatus(GqlPublicationStatus.fromValue(sub.getStatus()));
                    }
                    if (sub.isLocked()) {
                        gqlPublicationInfo.setLocked(true);
                    }
                    if (sub.isWorkInProgress()) {
                        gqlPublicationInfo.setWorkInProgress(true);
                    }
                }
            }

            gqlPublicationInfo.setAllowedToPublishWithoutWorkflow(node.hasPermission("publish"));
//            gqlPublicationInfo.setIsNonRootMarkedForDeletion(gqlPublicationInfo.getStatus() == GWTJahiaPublicationInfo.MARKED_FOR_DELETION && !node.isNodeType("jmix:markedForDeletionRoot"));

            if (gqlPublicationInfo.getStatus().getValue() == GWTJahiaPublicationInfo.PUBLISHED) {
                // the item status is published: check if the tree status or references are modified or unpublished
                Set<Integer> status = pubInfo.getTreeStatus(language);
                boolean overrideStatus = !status.isEmpty()
                        && Collections.max(status) > GWTJahiaPublicationInfo.PUBLISHED;
                if (!overrideStatus) {
                    // check references
                    for (PublicationInfo refInfo : pubInfo.getAllReferences()) {
                        status = refInfo.getTreeStatus(language);
                        if (!status.isEmpty() && Collections.max(status) > GWTJahiaPublicationInfo.PUBLISHED) {
                            overrideStatus = true;
                            break;
                        }
                    }
                }
                if (overrideStatus) {
                    gqlPublicationInfo.setStatus(GqlPublicationStatus.fromValue(GWTJahiaPublicationInfo.MODIFIED));
                }
            }

            return gqlPublicationInfo;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

}
