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

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.wip.WIPInfo;
import org.jahia.services.wip.WIPService;

import javax.jcr.RepositoryException;
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
            wipService.createWipPropertiesOnNewNode(jcrNode, new WIPInfo(wipInfo.getStatus().name(), new HashSet<>(wipInfo.getLanguages())));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }
}
