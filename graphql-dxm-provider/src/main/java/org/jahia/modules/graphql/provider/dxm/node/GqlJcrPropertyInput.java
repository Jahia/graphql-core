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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import java.util.List;

@GraphQLName("JCRProperty")
@GraphQLDescription("GraphQL representation of a JCR property to set")
public class GqlJcrPropertyInput extends GqlJcrPropertyI18nInput {
    private GqlJcrPropertyType type;
    private GqlJcrPropertyOption option;
    private String value;
    private List<String> values;

    public GqlJcrPropertyInput(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the property to set") String name,
                               @GraphQLName("type") @GraphQLDescription("The type of the property") GqlJcrPropertyType type,
                               @GraphQLName("option") @GraphQLDescription("The option of the property") GqlJcrPropertyOption option,
                               @GraphQLName("language") @GraphQLDescription("The language in which the property will be set (for internationalized properties") String language,
                               @GraphQLName("value") @GraphQLDescription("The value to set (for single valued properties)") String value,
                               @GraphQLName("values") @GraphQLDescription("The values to set (for multivalued properties)") List<String> values) {
        super(name, language);
        this.type = type;
        this.option = option;
        this.value = value;
        this.values = values;
    }

    @GraphQLField
    @GraphQLName("type")
    @GraphQLDescription("The type of the property")
    public GqlJcrPropertyType getType() {
        return type;
    }

    @GraphQLField
    @GraphQLName("option")
    @GraphQLDescription("The option of the property")
    public GqlJcrPropertyOption getOption() {
        return option;
    }

    @GraphQLField
    @GraphQLName("value")
    @GraphQLDescription("The value to set (for single valued properties)")
    public String getValue() {
        return value;
    }

    @GraphQLField
    @GraphQLName("values")
    @GraphQLDescription("The values to set (for multivalued properties)")
    public List<String> getValues() {
        return values;
    }
}
