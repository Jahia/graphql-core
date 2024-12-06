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

@GraphQLName("JCRDeletedProperty")
@GraphQLDescription("GraphQL representation of a deleted JCR property")
public class GqlJcrDeletedPropertyInput {
    protected String name;
    protected String language;

    public GqlJcrDeletedPropertyInput(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the property to set") String name,
                                      @GraphQLName("language") @GraphQLNonNull @GraphQLDescription("The language in which the property will be set (for internationalized properties") String language) {
        this.name = name;
        this.language = language;
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLNonNull
    @GraphQLDescription("The name of the property to delete")
    public String getName() {
        return name;
    }

    @GraphQLField
    @GraphQLName("language")
    @GraphQLNonNull
    @GraphQLDescription("The language in which the property will be deleted")
    public String getLanguage() {
        return language;
    }
}
