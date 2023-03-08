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

@GraphQLName("JahiaDatabase")
@GraphQLDescription("Details about the database Jahia is connected to")
public class GqlJahiaDatabase {

    private String type;
    private String name;
    private String version;
    private String driverName;
    private String driverVersion;

    public GqlJahiaDatabase() {
    }

    @GraphQLField
    @GraphQLName("type")
    @GraphQLDescription("Type of database specified in Jahia configuration")
    public String getType() {
        return type;
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLDescription("Name of the database vendor")
    public String getName() {
        return name;
    }

    @GraphQLField
    @GraphQLName("version")
    @GraphQLDescription("Version of the database")
    public String getVersion() {
        return version;
    }

    @GraphQLField
    @GraphQLName("driverName")
    @GraphQLDescription("Name of the driver used to connect to the database")
    public String getDriverName() {
        return driverName;
    }

    @GraphQLField
    @GraphQLName("driverVersion")
    @GraphQLDescription("Version of the driver used to connect to the database")
    public String getDriverVersion() {
        return driverVersion;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }

}


