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
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.PublicationInfo;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Extensions for JCRNodeMutation
 */
@GraphQLTypeExtension(GqlJcrNodeMutation.class)
public class PublicationJCRNodeMutationExtension {

    private GqlJcrNodeMutation nodeMutation;

    public PublicationJCRNodeMutationExtension(GqlJcrNodeMutation nodeMutation) {
        this.nodeMutation = nodeMutation;
    }

    @GraphQLField
    public boolean publish(@GraphQLName("languages") Collection<String> languages) {
        try {
            JCRPublicationService publicationService = JCRPublicationService.getInstance();
            List<PublicationInfo> infos = publicationService.getPublicationInfo(nodeMutation.getNode().getNode().getIdentifier(), new HashSet<>(languages), false, false, false, Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE);
            publicationService.publishByInfoList(infos, Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE,null);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

}
