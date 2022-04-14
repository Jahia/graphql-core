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
package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;

/**
 * Input for nodetypes list
 */
@GraphQLDescription("Input for nodetypes list")
public class NodeTypesListInput {

    private List<String> modules;
    private Boolean includeMixins;
    private Boolean includeNonMixins;
    private String siteKey;
    private Boolean includeAbstract;
    private List<String> includeTypes;
    private Boolean considerSubTypes;
    private List<String> excludeTypes;

    public NodeTypesListInput(
            @GraphQLName("siteKey") String siteKey,
            @GraphQLName("modules") List<String> modules,
            @GraphQLName("includeMixins") Boolean includeMixins,
            @GraphQLName("includeNonMixins") Boolean includeNonMixins,
            @GraphQLName("includeAbstract") Boolean includeAbstract,
            @GraphQLName("includeTypes") List<String> includeTypes,
            @GraphQLName("excludeTypes") List<String> excludeTypes,
            @GraphQLName("considerSubTypes") Boolean considerSubTypes
    ) throws BaseGqlClientException {
        if (modules != null && siteKey != null) {
            throw new GqlJcrWrongInputException("Either a site key or a list of modules can be specified, but not both");
        }
        this.siteKey = siteKey;
        this.modules = modules;
        this.includeMixins = includeMixins;
        this.includeNonMixins = includeNonMixins;
        this.includeAbstract = includeAbstract;
        this.includeTypes = includeTypes;
        this.excludeTypes = excludeTypes;
        this.considerSubTypes = considerSubTypes;
    }

    @GraphQLField
    @GraphQLName("modules")
    @GraphQLDescription("Filter on nodetypes defined in these modules")
    public List<String> getModules() {
        return modules;
    }

    @GraphQLField
    @GraphQLName("includeMixins")
    @GraphQLDescription("Include mixin types (default true)")
    public Boolean getIncludeMixins() {
        return includeMixins != null ? includeMixins : true;
    }

    @GraphQLField
    @GraphQLName("includeNonMixins")
    @GraphQLDescription("Include non mixin types (default true)")
    public Boolean getIncludeNonMixins() {
        return includeNonMixins != null ? includeNonMixins : true;
    }

    @GraphQLField
    @GraphQLName("siteKey")
    @GraphQLDescription("Consider only nodetypes for the specified site")
    public String getSiteKey() {
        return siteKey;
    }

    @GraphQLField
    @GraphQLName("includeAbstract")
    @GraphQLDescription("Include abstract types (default true)")
    public Boolean getIncludeAbstract() {
        return includeAbstract != null ? includeAbstract : true;
    }

    @GraphQLField
    @GraphQLName("includeTypes")
    @GraphQLDescription("Only include types specified by this list (also considering sub-types, if considerSubTypes is true)")
    public List<String> getIncludeTypes() {
        return includeTypes;
    }

    @GraphQLField
    @GraphQLName("considerSubTypes")
    @GraphQLDescription("Consider sub-types when checking for included/excluded nodetypes (default true)")
    public Boolean getConsiderSubTypes() {
        return considerSubTypes != null ? considerSubTypes : true;
    }

    @GraphQLField
    @GraphQLName("excludeTypes")
    @GraphQLDescription("Exclude the types, specified by this list (also considering sub-types, if considerSubTypes is true)")
    public List<String> getExcludeTypes() {
        return excludeTypes;
    }
}
