/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.services.content.nodetypes.ExtendedNodeDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GraphQL representation of a JCR node definition
 */
@GraphQLName("JCRNodeDefinition")
@GraphQLDescription("GraphQL representation of a JCR node definition")
public class GqlJcrNodeDefinition implements GqlJcrItemDefinition {

    private ExtendedNodeDefinition definition;

    public GqlJcrNodeDefinition(ExtendedNodeDefinition definition) {
        this.definition = definition;
    }

    @Override
    @GraphQLNonNull
    public String getName() {
        return definition.getName();
    }

    @Override
    @GraphQLNonNull
    public boolean isMandatory() {
        return definition.isMandatory();
    }

    @Override
    @GraphQLNonNull
    public boolean isAutoCreated() {
        return definition.isAutoCreated();
    }

    @Override
    @GraphQLNonNull
    public boolean isProtected() {
        return definition.isProtected();
    }

    @Override
    @GraphQLNonNull
    public boolean isHidden() {
        return definition.isHidden();
    }

    @Override
    @GraphQLNonNull
    public GqlJcrNodeType getDeclaringNodeType() {
        return new GqlJcrNodeType(definition.getDeclaringNodeType());
    }

    @GraphQLField
    @GraphQLDescription("Gets the minimum set of primary node types that the child node must have.")
    public List<GqlJcrNodeType> getRequiredPrimaryType() {
        return Arrays.stream(definition.getRequiredPrimaryTypes()).map(GqlJcrNodeType::new).collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLDescription("Gets the default primary node type that will be assigned to the child node if it is created without an explicitly specified primary node type.")
    public GqlJcrNodeType getDefaultPrimaryType() {
        return new GqlJcrNodeType(definition.getDefaultPrimaryType());
    }

    @GraphQLField
    @GraphQLDescription("Reports whether this child node can have same-name siblings. In other words, whether the parent node can have more than one child node of this name.")
    @GraphQLNonNull
    public boolean allowsSameNameSiblings() {
        return definition.allowsSameNameSiblings();
    }



}
