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
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FilterHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
     * @return a collection of matching vanity URLs
     */
    @GraphQLField
    @GraphQLName("vanityUrls")
    @GraphQLDescription("Get vanity URLs from the current node filtered by the parameters")
    public Collection<GqlJcrVanityUrl> getVanityUrls(@GraphQLName("languages") @GraphQLDescription("Languages") Collection<String> languages,
                                                     @GraphQLName("fieldFilter") @GraphQLDescription("Filter results based on graphql field values") FieldFiltersInput fieldFilter,
                                                     DataFetchingEnvironment environment) {

        JCRNodeIteratorWrapper vanityUrls;
        try {
            if (!node.getNode().hasNode(VANITYURLMAPPINGS_NODE)) {
                return Collections.emptyList();
            }
            vanityUrls = node.getNode().getNode(VANITYURLMAPPINGS_NODE).getNodes();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) vanityUrls, Spliterator.ORDERED), false)
                .map(GqlJcrVanityUrl::new)
                .filter(gqlJcrVanityUrl -> languages == null || languages.contains(gqlJcrVanityUrl.getLanguage()))
                .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forList(environment)))
                .collect(Collectors.toList());
    }
}
