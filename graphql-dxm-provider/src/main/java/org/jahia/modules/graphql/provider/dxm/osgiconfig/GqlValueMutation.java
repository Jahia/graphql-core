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
import org.jahia.services.modulemanager.util.PropertiesValues;

@GraphQLName("ConfigurationItemValuesMutation")
@GraphQLDescription("Mutation for configuration value object")
public class GqlValueMutation {
    protected PropertiesValues propertiesValues;

    public GqlValueMutation(PropertiesValues values) {
        this.propertiesValues = values;
    }

    protected PropertiesValues getPropertiesValues() {
        return propertiesValues;
    }

    @GraphQLField
    @GraphQLDescription("Modify a structured object")
    public GqlValueMutation mutateObject(@GraphQLName("name") @GraphQLDescription("property name part") String name) {
        return new GqlValueMutation(getPropertiesValues().getValues(name));
    }

    @GraphQLField
    @GraphQLDescription("Modify a list of items")
    public GqlListMutation mutateList(@GraphQLName("name") @GraphQLDescription("property name part") String name) {
        return new GqlListMutation(getPropertiesValues().getList(name));
    }

    @GraphQLField
    @GraphQLDescription("Set a property value")
    public String setValue(@GraphQLName("name") @GraphQLDescription("property name part") String name, @GraphQLName("value") String value) {
        getPropertiesValues().setProperty(name, value);
        return value;
    }

    @GraphQLField
    @GraphQLDescription("Remove the specified property and all sub/list properties")
    public boolean remove(@GraphQLName("name") @GraphQLDescription("property name part") String name) {
        getPropertiesValues().remove(name);
        return true;
    }

}
