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
import org.jahia.modules.graphql.provider.dxm.DXGraphQLFieldCompleter;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.services.modulemanager.spi.Config;
import org.jahia.services.modulemanager.spi.ConfigService;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesValues;

import javax.inject.Inject;
import java.io.IOException;

@GraphQLDescription("Mutation for OSGi configuration")
public class GqlConfigurationMutation extends GqlValueMutation implements DXGraphQLFieldCompleter {

    private final String pid;
    private final String identifier;

    @Inject
    @GraphQLOsgiService
    private ConfigService configService;

    private Config configuration = null;

    public GqlConfigurationMutation(String pid, String identifier) {
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

    @Override
    public void completeField() {
        if (configuration != null) {
            try {
                configService.storeConfig(configuration);
            } catch (IOException e) {
                throw new DataFetchingException(e);
            }
        }
    }

}
