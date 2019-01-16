/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

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
    @GraphQLDescription("Gets the required type of the property.")
    @GraphQLNonNull
    public GqlJcrPropertyType getRequiredType() {
        return GqlJcrPropertyType.fromValue(definition.getRequiredType());
    }

}
