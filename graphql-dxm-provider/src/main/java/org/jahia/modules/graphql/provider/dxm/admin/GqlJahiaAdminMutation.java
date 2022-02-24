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
package org.jahia.modules.graphql.provider.dxm.admin;


import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.osgiconfig.GqlConfigurationMutation;

/**
 * GraphQL root object for Admin related mutations.
 */
@GraphQLName("JahiaAdminMutation")
@GraphQLDescription("Admin mutations")
public class GqlJahiaAdminMutation {

    /**
     * We must have at least one field for the schema to be valid
     *
     * @return true
     */
    @GraphQLField
    @GraphQLDescription("Mutate an OSGi configuration")
    public GqlConfigurationMutation configuration(@GraphQLName("pid") @GraphQLDescription("Configuration pid ot factory pid") @GraphQLNonNull String pid,
                                                  @GraphQLName("identifier") @GraphQLDescription("If factory pid, configiration identifier (filename suffix)") String identifier) {
        return new GqlConfigurationMutation(pid, identifier);
    }
}
