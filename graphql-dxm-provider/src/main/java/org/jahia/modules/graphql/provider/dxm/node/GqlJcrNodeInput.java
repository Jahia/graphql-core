/*
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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import java.util.List;

@GraphQLName("JCRNode")
@GraphQLDescription("GraphQL representation of a JCR node to be created")
public class GqlJcrNodeInput {

    public GqlJcrNodeInput(@GraphQLName("name") @GraphQLNonNull String name,
                           @GraphQLName("primaryNodeType") @GraphQLNonNull String primaryNodeType,
                           @GraphQLName("mixins") List<String> mixins,
                           @GraphQLName("properties") List<GqlJcrPropertyInput> properties,
                           @GraphQLName("children") List<GqlJcrNodeInput> children) {
        this.name = name;
        this.primaryNodeType = primaryNodeType;
        this.mixins = mixins;
        this.properties = properties;
        this.children = children;
    }

    @GraphQLField
    @GraphQLDescription("The name of the node to create")
    public String name;

    @GraphQLField
    @GraphQLDescription("The primary node type of the node to create")
    public String primaryNodeType;

    @GraphQLField
    @GraphQLDescription("The list of mixins to add on the node")
    public List<String> mixins;

    @GraphQLField
    @GraphQLDescription("The list of properties to set on the node")
    public List<GqlJcrPropertyInput> properties;

    @GraphQLField
    @GraphQLDescription("The list of sub nodes to create")
    public List<GqlJcrNodeInput> children;
}
