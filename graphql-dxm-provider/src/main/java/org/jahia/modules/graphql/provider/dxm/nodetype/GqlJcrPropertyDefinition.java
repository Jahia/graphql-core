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
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * GraphQL representation of a JCR property definition
 */
@GraphQLName("JCRPropertyDefinition")
@GraphQLDescription("GraphQL representation of a JCR property definition")
public class GqlJcrPropertyDefinition implements GqlJcrItemDefinition {

    private ExtendedPropertyDefinition definition;

    public GqlJcrPropertyDefinition(ExtendedPropertyDefinition definition) {
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

    @GraphQLNonNull
    @GraphQLField
    @GraphQLDescription("Property constraints")
    public List<String> getConstraints() {
        return Arrays.stream(definition.getValueConstraintObjects()).map(ValueConstraint::getString).collect(Collectors.toList());
    }

    @Override
    @GraphQLName("declaringNodeType")
    @GraphQLNonNull
    @GraphQLDescription("Gets the node type that contains the declaration of this definition.")
    public GqlJcrNodeType getDeclaringNodeType() {
        return new GqlJcrNodeType(definition.getDeclaringNodeType());
    }

    @GraphQLField
    @GraphQLName("internationalized")
    @GraphQLDescription("Reports whether this property has language dependant values.")
    @GraphQLNonNull
    public boolean isInternationalized() {
        return definition.isInternationalized();
    }

    @GraphQLField
    @GraphQLDescription("Reports whether this property can have multiple values.")
    @GraphQLNonNull
    public boolean isMultiple() {
        return definition.isMultiple();
    }

    @GraphQLField
    @GraphQLName("requiredType")
    @GraphQLDescription("Gets the required type of the property.")
    @GraphQLNonNull
    public GqlJcrPropertyType getRequiredType() {
        return GqlJcrPropertyType.fromValue(definition.getRequiredType());
    }

    @GraphQLField
    @GraphQLName("displayName")
    @GraphQLDescription("Gets the displayable name of the property for the given language code. Return the system name in case the label doesn't exists")
    @GraphQLNonNull
    public String getDisplayName(@GraphQLName("language") @GraphQLDescription("Language") @GraphQLNonNull String language) {
        String displayName = definition.getLabel(new Locale(language));
        return StringUtils.isNotEmpty(displayName) ? displayName : getName();
    }
}
