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
package org.jahia.modules.graphql.provider.dxm.site;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.services.content.decorator.JCRSiteNode;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Extension for the JCR site node")
public class SiteJCRNodeExtensions {

    private GqlJcrNode node;

    public SiteJCRNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    /**
     * @return GraphQL representation of the site the JCR node belongs to, or the system site in case the node does not belong to any site
     */
    @GraphQLField
    @GraphQLName("site")
    @GraphQLDescription("GraphQL representation of the site the JCR node belongs to, or the system site in case the node does not belong to any site")
    @GraphQLNonNull
    public GqlJcrSite getSite() {
        try {
            JCRSiteNode resolveSite = node.getNode().getResolveSite();
            if (resolveSite == null) {
                return null;
            }
            return new GqlJcrSite(resolveSite);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }


}
