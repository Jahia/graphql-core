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
package org.jahia.modules.graphql.provider.dxm.osgiconfig;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.services.modulemanager.util.PropertiesList;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@GraphQLName("ConfigurationItemsListQuery")
@GraphQLDescription("Query for configuration list of values")
public class GqlListQuery {
    PropertiesList propertiesList;

    public GqlListQuery(PropertiesList propertiesList) {
        this.propertiesList = propertiesList;
    }

    @GraphQLField
    @GraphQLDescription("Adds a new structured object to the list")
    public int getSize() {
        return propertiesList.getSize();
    }

    @GraphQLField
    @GraphQLDescription("Get sub structured object values")
    public List<GqlValueQuery> getObjects() {
        return IntStream.range(0, propertiesList.getSize()).boxed()
                .map(propertiesList::getValues).map(GqlValueQuery::new)
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLDescription("Get sub lists of items")
    public List<GqlListQuery> getLists() {
        return IntStream.range(0, propertiesList.getSize()).boxed()
                .map(propertiesList::getList).map(GqlListQuery::new)
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLDescription("Get property values")
    public List<String> getValues() {
        return IntStream.range(0, propertiesList.getSize()).boxed()
                .map(propertiesList::getProperty)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
