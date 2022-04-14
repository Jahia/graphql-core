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
package org.jahia.modules.graphql.provider.dxm.site;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
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
