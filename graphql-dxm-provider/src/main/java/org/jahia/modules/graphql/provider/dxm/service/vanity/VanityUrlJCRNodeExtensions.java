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
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.LinkedList;

import static org.jahia.services.seo.jcr.VanityUrlManager.VANITYURLMAPPINGS_NODE;

/**
 * Node extension for vanity URL.
 */
@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Node extension for vanity URL")
public class VanityUrlJCRNodeExtensions {

    private GqlJcrNode node;

    public VanityUrlJCRNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    /**
     * Get vanity url from the current node filtered by the parameters
     * @param languages an array of languages to filter
     * @param onlyActive get only active vanity urls
     * @param onlyDefault get only default vanity urls
     * @return a list of vanity urls
     */
    @SuppressWarnings("unchecked")
    @GraphQLField
    @GraphQLName("vanityUrls")
    @GraphQLDescription("return vanity urls")
    public Collection<GqlJcrVanityUrl> getVanityUrls(@GraphQLName("languages") Collection<String>  languages, @GraphQLName("onlyActive") Boolean onlyActive, @GraphQLName("onlyDefault") Boolean onlyDefault ) {
        try {
            Collection<GqlJcrVanityUrl> vanityUrls = new LinkedList<>();
            JCRNodeIteratorWrapper urls = node.getNode().getNode(VANITYURLMAPPINGS_NODE).getNodes();
            urls.forEachRemaining(vanityUrl -> {
                GqlJcrVanityUrl gqlVanityUrl = new GqlJcrVanityUrl((JCRNodeWrapper) vanityUrl);
                try {
                    if ((languages == null || languages.contains(gqlVanityUrl.getLanguage())) && ((onlyActive != null && !onlyActive) || gqlVanityUrl.isActive()) && ((onlyDefault != null &&!onlyDefault) || gqlVanityUrl.isDefault())) {
                        vanityUrls.add(gqlVanityUrl);
                    }
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            });

            return vanityUrls;

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
