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
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FilterHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jahia.services.seo.jcr.VanityUrlManager.VANITYURLMAPPINGS_NODE;

/**
 * Node extension for vanity URL.
 */
@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Node extension for vanity URL")
public class VanityUrlJCRNodeExtensions {

    private GqlJcrNode node;

    /**
     * Initializes an instance of this class.
     * 
     * @param node the corresponding GraphQL node
     */
    public VanityUrlJCRNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    /**
     * Get vanity URLs from the current node filtered by the parameters.
     * 
     * @param languages a collection of languages to retrieve the corresponding vanity URLs
     * @param onlyActive if <code>true</code>, get only active vanity URLs
     * @param onlyDefault if <code>true</code>, get only default vanity URLs
     * @return a collection of matching vanity URLs
     */
    @GraphQLField
    @GraphQLName("vanityUrls")
    @GraphQLDescription("Get vanity URLs from the current node filtered by the parameters")
    public Collection<GqlJcrVanityUrl> getVanityUrls(@GraphQLName("languages") Collection<String> languages,
                                                     @GraphQLName("onlyActive") Boolean onlyActive,
                                                     @GraphQLName("onlyDefault") Boolean onlyDefault,
                                                     @GraphQLName("fieldFilter") FieldFiltersInput fieldFilter, DataFetchingEnvironment environment) {
        List<GqlJcrVanityUrl> result = null;
        try {
            if (node.getNode().hasNode(VANITYURLMAPPINGS_NODE)) {
                boolean getOnlyActive = onlyActive != null && onlyActive.booleanValue();
                boolean getOnlyDefault = onlyDefault != null && onlyDefault.booleanValue();
                List<GqlJcrVanityUrl> vanityUrls = new LinkedList<>();
                JCRNodeIteratorWrapper urls = node.getNode().getNode(VANITYURLMAPPINGS_NODE).getNodes();
                urls.forEach(vanityUrl -> {
                    GqlJcrVanityUrl gqlVanityUrl = new GqlJcrVanityUrl(vanityUrl);
                    if ((languages == null || languages.contains(gqlVanityUrl.getLanguage()))
                            && (!getOnlyActive || gqlVanityUrl.isActive())
                            && (!getOnlyDefault || gqlVanityUrl.isDefault())) {
                        vanityUrls.add(gqlVanityUrl);
                    }
                });
                result = vanityUrls;
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        List<GqlJcrVanityUrl> gqlJcrVanityUrls = result != null ? result : Collections.emptyList();
        return FilterHelper.filterList(gqlJcrVanityUrls, fieldFilter, environment);
    }
}
