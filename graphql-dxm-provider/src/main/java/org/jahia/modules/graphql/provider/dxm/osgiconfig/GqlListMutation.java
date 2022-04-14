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

@GraphQLName("ConfigurationItemsListMutation")
@GraphQLDescription("Mutation for configuration list of values")
public class GqlListMutation {
    PropertiesList propertiesList;

    public GqlListMutation(PropertiesList propertiesList) {
        this.propertiesList = propertiesList;
    }

    @GraphQLField
    @GraphQLDescription("Adds a new structured object to the list")
    public GqlValueMutation addObject() {
        return new GqlValueMutation(propertiesList.addValues());
    }

    @GraphQLField
    @GraphQLDescription("Adds a new sub list to the list")
    public GqlListMutation addList() {
        return new GqlListMutation(propertiesList.addList());
    }

    @GraphQLField
    @GraphQLDescription("Adds a property value to the list")
    public String addValue(@GraphQLName("value") String value) {
        propertiesList.addProperty(value);
        return value;
    }
}
