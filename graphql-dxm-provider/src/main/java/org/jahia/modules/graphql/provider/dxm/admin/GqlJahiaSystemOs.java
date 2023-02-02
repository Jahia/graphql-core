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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("JahiaSystemOs")
@GraphQLDescription("Details about the system OS used to run Jahia")
public class GqlJahiaSystemOs {

    private String name;
    private String architecture;
    private String version;

    public GqlJahiaSystemOs() {
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLDescription("Operating System Name")
    public String getName() {
        return name;
    }

    @GraphQLField
    @GraphQLName("architecture")
    @GraphQLDescription("Operating System Architecture (amd64, arm64, ...)")
    public String getArchitecture() {
        return architecture;
    }

    @GraphQLField
    @GraphQLName("version")
    @GraphQLDescription("Operating System Version")
    public String getVersion() {
        return version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}


