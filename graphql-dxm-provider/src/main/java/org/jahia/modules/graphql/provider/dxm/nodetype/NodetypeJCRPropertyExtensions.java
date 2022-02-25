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

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrProperty;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

import javax.jcr.RepositoryException;

/**
 * Extensions for JCRProperty
 */
@GraphQLTypeExtension(GqlJcrProperty.class)
@GraphQLDescription("Extensions for JCRProperty")
public class NodetypeJCRPropertyExtensions {

    private GqlJcrProperty property;

    public NodetypeJCRPropertyExtensions(GqlJcrProperty property) {
        this.property = property;
    }

    @GraphQLField
    @GraphQLName("definition")
    @GraphQLDescription("Returns the property definition that applies to this property.")
    public GqlJcrPropertyDefinition getDefinition() {
        ExtendedPropertyDefinition definition;

        try {
            definition = (ExtendedPropertyDefinition) property.getProperty().getDefinition();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        if (definition != null) {
            return new GqlJcrPropertyDefinition(definition);
        }
        return null;
    }

}
