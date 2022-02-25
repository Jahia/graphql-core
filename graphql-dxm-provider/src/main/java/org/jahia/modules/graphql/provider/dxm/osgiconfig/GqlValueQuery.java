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
import org.jahia.services.modulemanager.util.PropertiesValues;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GraphQLName("ConfigurationItemValuesQuery")
@GraphQLDescription("Query for configuration value object")
public class GqlValueQuery {
    protected PropertiesValues propertiesValues;

    public GqlValueQuery(PropertiesValues v) {
        this.propertiesValues = v;
    }

    protected PropertiesValues getPropertiesValues() {
        return propertiesValues;
    }

    @GraphQLField
    @GraphQLDescription("Get keys")
    public Set<String> getKeys() {
        return getPropertiesValues().getKeys();
    }

    @GraphQLField
    @GraphQLDescription("Get a sub structured object value")
    public GqlValueQuery getObject(@GraphQLName("name") @GraphQLDescription("property name part") String name) {
        PropertiesValues values = getPropertiesValues().getValues(name);
        if (!values.getKeys().isEmpty()) {
            return new GqlValueQuery(values);
        }
        return null;
    }

    @GraphQLField
    @GraphQLDescription("Get a list of items")
    public GqlListQuery getList(@GraphQLName("name") @GraphQLDescription("property name part") String name) {
        PropertiesList list = getPropertiesValues().getList(name);
        if (list.getSize() > 0) {
            return new GqlListQuery(list);
        }
        return null;
    }

    @GraphQLField
    @GraphQLDescription("Get a property value")
    public String getValue(@GraphQLName("name") @GraphQLDescription("property name part") String name) {
        return getPropertiesValues().getProperty(name);
    }

    @GraphQLField
    @GraphQLDescription("Get property values")
    public List<GqlConfigurationProperty> getValues() {
        return getKeys().stream().map(k -> new GqlConfigurationProperty(k,getValue(k))).collect(Collectors.toList());
    }

}
