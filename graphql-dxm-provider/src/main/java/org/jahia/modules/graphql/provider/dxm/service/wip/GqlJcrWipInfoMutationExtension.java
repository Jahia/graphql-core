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
package org.jahia.modules.graphql.provider.dxm.service.wip;

import graphql.annotations.annotationTypes.*;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.wip.WIPInfo;
import org.jahia.services.wip.WIPService;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.HashSet;


/**
 * GraphQL Work in progress informations.
 */
@GraphQLTypeExtension(GqlJcrNodeMutation.class)
@GraphQLName("WipInfoMutation")
public class GqlJcrWipInfoMutationExtension extends GqlJcrMutationSupport {

    private GqlJcrNodeMutation node;
    private WIPService wipService;

    public GqlJcrWipInfoMutationExtension(GqlJcrNodeMutation node) {
        this.wipService = BundleUtils.getOsgiService(WIPService.class, null);
        this.node = node;
    }

    @GraphQLField
    @GraphQLDescription("Mutate wip information")
    @GraphQLName("mutateWipInfo")
    public boolean setWipInfo(@GraphQLName("wipInfo") @GraphQLNonNull @GraphQLDescription("Work in progress information to save") GqlJcrWipInfo wipInfo) {
        try {
            final JCRNodeWrapper jcrNode = node.getNode().getNode();
            wipService.saveWipPropertiesIfNeeded(jcrNode, new WIPInfo(wipInfo.getStatus().name(), new HashSet<>(wipInfo.getLanguages())));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add wip information")
    @GraphQLName("createWipInfo")
    public boolean createWipInfo(@GraphQLName("wipInfo") @GraphQLNonNull @GraphQLDescription("Work in progress information to save") GqlJcrWipInfo wipInfo) {
        try {
            final JCRNodeWrapper jcrNode = node.getNode().getNode();
            // TODO : move it to WIP service
            // on create simply update JCR properties
            jcrNode.setProperty(Constants.WORKINPROGRESS_STATUS, wipInfo.getStatus().toString());
            final Collection<String> languages = wipInfo.getLanguages();
            jcrNode.setProperty(Constants.WORKINPROGRESS_LANGUAGES, languages.toArray(new String[0]));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }
}
