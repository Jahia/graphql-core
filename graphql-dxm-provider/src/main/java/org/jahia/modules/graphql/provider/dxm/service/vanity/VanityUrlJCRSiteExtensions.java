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

import graphql.annotations.annotationTypes.*;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.site.GqlJcrSite;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.jahia.services.seo.jcr.VanityUrlManager.JAHIANT_VANITYURL;

/**
 * Site Extension for vanity URL.
 */
@GraphQLTypeExtension(GqlJcrSite.class)
@GraphQLDescription("Site Extension for vanity URL")
public class VanityUrlJCRSiteExtensions {

    private GqlJcrSite siteNode;

    public VanityUrlJCRSiteExtensions(GqlJcrSite node) {
        this.siteNode = node;
    }

    /**
     * find the matching active vanity url for the site
     * if zero or more than one is returned, throw an exception
     * @param url url to fine
     * @return the matching url
     */
    @GraphQLField
    @GraphQLName("findActiveVanityURL")
    @GraphQLDescription("return vanity urls")
    public GqlJcrVanityUrl findActiveVanityURL(@GraphQLNonNull @GraphQLName("url") String url) {
        List<GqlJcrVanityUrl> gqlJcrVanityUrls = getGqlJcrVanityUrls(url, true);
        if (gqlJcrVanityUrls.size() > 1) {
            throw new RuntimeException("more than one active vanity url matches " + url);
        } else if (gqlJcrVanityUrls.isEmpty()) {
            throw new RuntimeException("no active vanity url matches " + url);
        }
        return gqlJcrVanityUrls.get(0);
    }

    /**
     * find any matching url in the site
     * @param url url to find
     * @return a list of matching
     */
    @GraphQLField
    @GraphQLName("findVanityURLs")
    @GraphQLDescription("return vanity urls")
    public Collection<GqlJcrVanityUrl> findVanityURLs(@GraphQLNonNull @GraphQLName("url") String url) {
        return getGqlJcrVanityUrls(url, false);
    }

    private List<GqlJcrVanityUrl> getGqlJcrVanityUrls(String url, boolean activeOnly) {
        try {
            List<GqlJcrVanityUrl> vanityUrls = new LinkedList<>();
            JCRSessionWrapper jcrSessionWrapper = siteNode.getNode().getSession();

            VanityUrlService vanityUrlvanityUrlService = BundleUtils.getOsgiService(VanityUrlService.class, null);
            List<VanityUrl> urls = vanityUrlvanityUrlService.findExistingVanityUrls(url, siteNode.getSiteKey(), jcrSessionWrapper.getWorkspace().getName());
            urls.forEach(vanityUrl -> {
                try {
                    if (!activeOnly || vanityUrl.isActive()) {
                        vanityUrls.add(new GqlJcrVanityUrl(jcrSessionWrapper.getNode(vanityUrl.getPath())));
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

    /**
     * get all vanity urls from a site as a connection
     * @param environment status of the connection
     * @return a paginated list of vanity urls
     */
    @GraphQLField
    @GraphQLName("getAllVanityURLs")
    @GraphQLDescription("return vanity urls")
    @GraphQLConnection(connection = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrVanityUrl> getAllVanityURLs(DataFetchingEnvironment environment) {
        try {
            List<GqlJcrVanityUrl> vanityUrls = new LinkedList<>();
            PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
            JCRSessionWrapper jcrSession = siteNode.getNode().getSession();

            StringBuilder vanityQuery = new StringBuilder("SELECT * FROM [").append(JAHIANT_VANITYURL).append("] AS vanityURL WHERE ");
            vanityQuery.append("ISDESCENDANTNODE('/sites/").append(JCRContentUtils.sqlEncode(siteNode.getSiteKey())).append("')");

            Query query = jcrSession.getWorkspace().getQueryManager().createQuery(vanityQuery.toString(), Query.JCR_SQL2);

            query.execute().getNodes().forEachRemaining(vanityUrl -> {
                vanityUrls.add(new GqlJcrVanityUrl((JCRNodeWrapper) vanityUrl));
            });
            return PaginationHelper.paginate(vanityUrls, v -> PaginationHelper.encodeCursor(v.getUuid()), arguments);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
