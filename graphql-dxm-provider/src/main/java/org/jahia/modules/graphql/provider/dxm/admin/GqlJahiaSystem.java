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
import graphql.annotations.annotationTypes.GraphQLNonNull;

@GraphQLName("JahiaSystem")
@GraphQLDescription("Details about the system used to run Jahia")
public class GqlJahiaSystem {
    public GqlJahiaSystem() {
    }

    /**
     * Get getJahiaSystemOs
     *
     * @return GqlJahiaSystemOs
     */
    @GraphQLField
    @GraphQLName("os")    
    @GraphQLDescription("Details about the operating system")
    public GqlJahiaSystemOs getSystemOs() {
        GqlJahiaSystemOs gqlJahiaSystemOs = new GqlJahiaSystemOs();
        gqlJahiaSystemOs.setName(System.getProperty("os.name"));
        gqlJahiaSystemOs.setArchitecture(System.getProperty("os.arch"));
        gqlJahiaSystemOs.setVersion(System.getProperty("os.version"));
        return gqlJahiaSystemOs;
    }

    /**
     * Get getJahiaSystemJava
     *
     * @return GqlJahiaSystemJava
     */
    @GraphQLField
    @GraphQLName("java")    
    @GraphQLDescription("Details about the operating system")
    public GqlJahiaSystemJava getSystemJava() {
        GqlJahiaSystemJava gqlJahiaSystemJava = new GqlJahiaSystemJava();
        gqlJahiaSystemJava.setRuntimeName(System.getProperty("java.runtime.name"));
        gqlJahiaSystemJava.setRuntimeVersion(System.getProperty("java.runtime.version"));
        gqlJahiaSystemJava.setVendor(System.getProperty("java.vendor"));
        gqlJahiaSystemJava.setVendorVersion(System.getProperty("java.vendor.version"));
        return gqlJahiaSystemJava;
    }   

}


