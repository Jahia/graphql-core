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
package org.jahia.modules.graphql.provider.dxm.service.vanity;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
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
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.jahia.services.seo.jcr.VanityUrlManager.JAHIANT_VANITYURL;

/**
 * Site Extension for vanity URL.
 */
// NOTE: this extension is currently disabled as no direct use case for it exist 
//@GraphQLTypeExtension(GqlJcrSite.class)
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
            JCRSessionWrapper jcrSessionWrapper = siteNode.getNode().getSession();

            VanityUrlService vanityUrlvanityUrlService = BundleUtils.getOsgiService(VanityUrlService.class, null);
            List<VanityUrl> urls = vanityUrlvanityUrlService.findExistingVanityUrls(url, siteNode.getSiteKey(), jcrSessionWrapper.getWorkspace().getName());
            return urls.stream().filter(vanityUrl -> !activeOnly || vanityUrl.isActive())
                    .map(ThrowingFunction.unchecked(vanityUrl -> new GqlJcrVanityUrl(jcrSessionWrapper.getNode(vanityUrl.getPath()))))
                    .collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get all vanity urls from a site as a connection
     * @param environment status of the connection
     * @return a paginated list of vanity urls
     */
    @SuppressWarnings("unchecked")
    @GraphQLField
    @GraphQLName("getAllVanityURLs")
    @GraphQLDescription("return vanity urls")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrVanityUrl> getAllVanityURLs(DataFetchingEnvironment environment) {
        try {
            PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
            JCRSessionWrapper jcrSession = siteNode.getNode().getSession();

            String vanityQuery = "SELECT * FROM [" + JAHIANT_VANITYURL + "] AS vanityURL WHERE " +
                    "ISDESCENDANTNODE('/sites/" + JCRContentUtils.sqlEncode(siteNode.getSiteKey()) + "')";
            Query query = jcrSession.getWorkspace().getQueryManager().createQuery(vanityQuery, Query.JCR_SQL2);
            NodeIterator it = query.execute().getNodes();
            Stream<GqlJcrVanityUrl> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>)it, Spliterator.ORDERED), false)
                    .map(GqlJcrVanityUrl::new);

            return PaginationHelper.paginate(stream, v -> PaginationHelper.encodeCursor(v.getUuid()), arguments);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
