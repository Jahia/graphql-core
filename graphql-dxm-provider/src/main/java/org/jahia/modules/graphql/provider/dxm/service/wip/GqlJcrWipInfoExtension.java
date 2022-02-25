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
package org.jahia.modules.graphql.provider.dxm.service.wip;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.wip.WIPInfo;
import org.jahia.services.wip.WIPService;

import javax.jcr.RepositoryException;

/**
 * GraphQL Work in progress informations for a given node.
 */
@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Node extension for Work in progress")
public class GqlJcrWipInfoExtension {

    private GqlJcrNode node;
    private WIPService wipService;

    /**
     * Default constructor
     *
     * @param node
     */
    public GqlJcrWipInfoExtension(GqlJcrNode node) {

        this.node = node;
        this.wipService = BundleUtils.getOsgiService(WIPService.class, null);
    }

    @GraphQLField
    @GraphQLName("wipInfo")
    @GraphQLDescription("Read work in progress information for a given node")
    public GqlJcrWipInfo getWipInfo() {
        try {
            WIPInfo wipInfo = wipService.getWipInfo(node.getNode());
            return new GqlJcrWipInfo(GqlJcrWipInfo.WipStatus.valueOf(wipInfo.getStatus()), wipInfo.getLanguages());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLName("defaultWipInfo")
    @GraphQLDescription("Read default Work in progress information. Set by \"wip.checkbox.checked\" system proprety")
    public GqlJcrWipInfo getDefaultWipInfo() {
        WIPInfo wipInfo = wipService.getDefaultWipInfo();
        return new GqlJcrWipInfo(GqlJcrWipInfo.WipStatus.valueOf(wipInfo.getStatus()), wipInfo.getLanguages());
    }
}
