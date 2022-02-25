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
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.services.modulemanager.spi.Config;
import org.jahia.services.modulemanager.spi.ConfigService;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesValues;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@GraphQLDescription("Query for OSGi configuration")
public class GqlConfigurationQuery extends GqlValueQuery {

    private final String pid;
    private final String identifier;

    @Inject
    @GraphQLOsgiService
    private ConfigService configService;

    private Config configuration = null;

    public GqlConfigurationQuery(String pid, String identifier) {
        super(null);
        this.pid = pid;
        this.identifier = identifier;
    }

    @Override
    protected PropertiesValues getPropertiesValues() {
        if (propertiesValues == null) {
            propertiesValues = getConfiguration().getValues();
        }
        return super.getPropertiesValues();
    }

    @GraphQLField
    @GraphQLDescription("Get all properties of the configuration, as they are stored in OSGi")
    public List<GqlConfigurationProperty> getFlatProperties() {
        return getConfiguration().getRawProperties().entrySet().stream()
                .map(entry -> new GqlConfigurationProperty(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLDescription("Get all properties of the configuration, as they are stored in OSGi")
    public Set<String> getFlatKeys() {
        return getConfiguration().getRawProperties().keySet();
    }

    private Config getConfiguration() {
        try {
            if (identifier == null) {
                configuration = configService.getConfig(pid);
            } else {
                configuration = configService.getConfig(pid, identifier);
            }
        } catch (IOException e) {
            throw new DataFetchingException(e);
        }
        return configuration;
    }

}
