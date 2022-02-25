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
    @GraphQLName("name")
    @GraphQLNonNull
    @GraphQLDescription("Gets the name of the child item.")
    public String getName() {
        return definition.getName();
    }

    @Override
    @GraphQLNonNull
    @GraphQLDescription("Reports whether the item is mandatory. A mandatory item is one that, if its parent node exists, must also exist.")
    public boolean isMandatory() {
        return definition.isMandatory();
    }

    @Override
    @GraphQLNonNull
    @GraphQLDescription("Reports whether the item is to be automatically created when its parent node is created.")
    public boolean isAutoCreated() {
        return definition.isAutoCreated();
    }

    @Override
    @GraphQLNonNull
    @GraphQLDescription("Reports whether the child item is protected.")
    public boolean isProtected() {
        return definition.isProtected();
    }

    @Override
    @GraphQLNonNull
    @GraphQLDescription("Reports whether the child item is hidden from UI.")
    public boolean isHidden() {
        return definition.isHidden();
    }

    @Override
    @GraphQLName("declaringNodeType")
    @GraphQLNonNull
    @GraphQLDescription("Gets the node type that contains the declaration of this definition.")
    public GqlJcrNodeType getDeclaringNodeType() {
        return new GqlJcrNodeType(definition.getDeclaringNodeType());
    }

    @GraphQLField
    @GraphQLName("requiredPrimaryType")
    @GraphQLDescription("Gets the minimum set of primary node types that the child node must have.")
    public List<GqlJcrNodeType> getRequiredPrimaryType() {
        return Arrays.stream(definition.getRequiredPrimaryTypes()).map(GqlJcrNodeType::new).collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLName("defaultPrimaryType")
    @GraphQLDescription("Gets the default primary node type that will be assigned to the child node if it is created without an explicitly specified primary node type.")
    public GqlJcrNodeType getDefaultPrimaryType() {
        return new GqlJcrNodeType(definition.getDefaultPrimaryType());
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Reports whether this child node can have same-name siblings. In other words, whether the parent node can have more than one child node of this name.")
    public boolean allowsSameNameSiblings() {
        return definition.allowsSameNameSiblings();
    }



}
