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

@GraphQLName("JahiaSystemJava")
@GraphQLDescription("Details about the system OS used to run Jahia")
public class GqlJahiaSystemJava {

    private String runtimeName;
    private String runtimeVersion;
    private String vendor;
    private String vendorVersion;

    public GqlJahiaSystemJava() {
    }

    @GraphQLField
    @GraphQLName("runtimeName")
    @GraphQLDescription("Java Runtime name")
    public String getRuntimeName() {
        return runtimeName;
    }

    @GraphQLField
    @GraphQLName("runtimeVersion")
    @GraphQLDescription("Java Runtime version")
    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    @GraphQLField
    @GraphQLName("vendor")
    @GraphQLDescription("Java vendor")
    public String getVendor() {
        return vendor;
    }    

    @GraphQLField
    @GraphQLName("vendorVersion")
    @GraphQLDescription("Java vendor version")
    public String getVendorVersion() {
        return vendorVersion;
    }    

    public void setRuntimeName(String runtimeName) {
        this.runtimeName = runtimeName;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }    

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setVendorVersion(String vendorVersion) {
        this.vendorVersion = vendorVersion;
    }

}


