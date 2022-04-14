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
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedType;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@GraphQLName("JCRSite")
@GraphQLDescription("GraphQL representation of a site node")
@SpecializedType(Constants.JAHIANT_VIRTUALSITE)
public class GqlJcrSite extends GqlJcrNodeImpl implements GqlJcrNode {

    private JCRSiteNode siteNode;

    public GqlJcrSite(JCRNodeWrapper node) throws RepositoryException {
        super(node);

        if (node instanceof JCRSiteNode) {
            this.siteNode = (JCRSiteNode) node;
        } else if (node.isNodeType(Constants.JAHIANT_VIRTUALSITE)) {
            // Workaround when site node is instanceof JCRNodeWrapperImpl
            this.siteNode = node.getResolveSite();
        }
    }

    @GraphQLField
    @GraphQLName("sitekey")
    @GraphQLDescription("Site key")
    public String getSiteKey() {
        return siteNode.getSiteKey();
    }

    @GraphQLField
    @GraphQLName("serverName")
    @GraphQLDescription("Site server name")
    public String getServerName() {
        return siteNode.getServerName();
    }

    @GraphQLField
    @GraphQLName("description")
    @GraphQLDescription("Site description")
    public String getDescription() {
        return siteNode.getDescription();
    }

    @GraphQLField
    @GraphQLName("defaultLanguage")
    @GraphQLDescription("Site default language")
    public String getDefaultLanguage() {
        return siteNode.getDefaultLanguage();
    }

    @GraphQLField
    @GraphQLName("installedModules")
    @GraphQLDescription("Retrieves a collection of module IDs, which are installed on the site, the node belongs to")
    public Collection<String> getInstalledModules() {
        return siteNode.getInstalledModules();
    }

    @GraphQLField
    @GraphQLName("installedModulesWithAllDependencies")
    @GraphQLDescription("Retrieves a collection of module IDs, which are installed on the site, the node belongs to, as well as dependencies of those modules")
    public Collection<String> getInstalledModulesWithAllDependencies() {
        return siteNode.getInstalledModulesWithAllDependencies();
    }

    @GraphQLField
    @GraphQLName("languages")
    @GraphQLDescription("Site languages")
    public Collection<GqlSiteLanguage> getLanguages() {
        List<GqlSiteLanguage> result = new ArrayList<>();

        for (String s : siteNode.getLanguages()) {
            result.add(new GqlSiteLanguage(s, true, siteNode.getActiveLiveLanguages().contains(s), siteNode.getMandatoryLanguages().contains(s)));
        }
        for (String s : siteNode.getInactiveLanguages()) {
            result.add(new GqlSiteLanguage(s, false, false, siteNode.getMandatoryLanguages().contains(s)));
        }

        return result;
    }

    @GraphQLField
    @GraphQLName("homePage")
    @GraphQLDescription("Returns the node of the home page")
    public GqlJcrNode getHomePage() throws RepositoryException {
        return SpecializedTypesHandler.getNode(siteNode.getHome());
    }

}
