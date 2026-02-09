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
package org.jahia.modules.graphql.provider.dxm.extensions;


import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

@GraphQLTypeExtension(GqlJcrNode.class)
public class JCRNodeExtensions {

    private GqlJcrNode node;

    public JCRNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    @GraphQLField
    @GraphQLDescription("Sample extension")
    public String testExtension(@GraphQLName("arg") @GraphQLDescription("Sample extension argument") String arg) {
        return "test " + node.getName() + " - " + arg;
    }

    /**
     * To allow access to this endpoint, add user to a server-type role e.g. web designer role,
     * and enable myApiAdmin permissions
     */
    @GraphQLField
    @GraphQLDescription("Sample extension")
    @GraphQLRequiresPermission("myApiAdmin")
    public String testRequiresPermission(@GraphQLName("arg") @GraphQLDescription("Sample extension argument") String arg) {
        return "protected endpoint:  " + node.getName() + " - " + arg;
    }

    /**
     * This endpoint is restricted using OSGi configuration entry:
     * permission.JCRNode.testPermissionConfiguration = myApiAdmin
     *
     * To allow access to this endpoint, add user to a server-type role e.g. web designer role,
     * and enable myApiAdmin permissions
     */
    @GraphQLField
    @GraphQLDescription("Sample extension")
    public String testPermissionConfiguration(@GraphQLName("arg") @GraphQLDescription("Sample extension argument") String arg) {
        return "protected endpoint:  " + node.getName() + " - " + arg;
    }
}
