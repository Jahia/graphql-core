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
package org.jahia.modules.graphql.provider.dxm.user;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.services.content.decorator.JCRGroupNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;

import javax.inject.Inject;

@GraphQLName("UserGroupQuery")
@GraphQLDescription("User group queries")
public class GqlUserGroup {

    @Inject
    @GraphQLOsgiService
    private JahiaGroupManagerService groupManagerService;

    @GraphQLField
    @GraphQLDescription("Get a group")
    public GqlGroup getGroup(@GraphQLName("groupName") @GraphQLDescription("Group name") @GraphQLNonNull String groupName,
                             @GraphQLName("site") @GraphQLDescription("Site where the group is defined") String site) {
        JCRGroupNode jcrGroupNode = groupManagerService.lookupGroup(site, groupName);
        if (jcrGroupNode == null) {
            return null;
        }
        return new GqlGroup(jcrGroupNode.getJahiaGroup());
    }
}
