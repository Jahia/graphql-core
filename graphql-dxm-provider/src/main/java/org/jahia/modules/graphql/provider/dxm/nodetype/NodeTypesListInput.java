/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

/**
 * Input for nodetypes list
 */
public class NodeTypesListInput {

    private List<String> modules;
    private Boolean includeMixins;
    private Boolean includeNonMixins;
    private String siteKey;
    private Boolean includeAbstract;
    private List<String> includedTypes;
    private Boolean considerSubTypes;
    private List<String> excludedTypes;

    public NodeTypesListInput(@GraphQLName("siteKey") String siteKey,
            @GraphQLName("modules") List<String> modules,
            @GraphQLName("includeMixins") Boolean includeMixins,
            @GraphQLName("includeNonMixins") Boolean includeNonMixins,
            @GraphQLName("includeAbstract") Boolean includeAbstract,
            @GraphQLName("includedTypes") List<String> includedTypes,
            @GraphQLName("excludedTypes") List<String> excludedTypes,
            @GraphQLName("considerSubTypes") Boolean considerSubTypes) {
        this.siteKey = siteKey;
        this.modules = modules;
        this.includeMixins = includeMixins;
        this.includeNonMixins = includeNonMixins;
        this.includeAbstract = includeAbstract;
        this.includedTypes = includedTypes;
        this.excludedTypes = excludedTypes;
        this.considerSubTypes = considerSubTypes;
    }

    @GraphQLField
    @GraphQLDescription("Filter on nodetypes defined in these modules")
    public List<String> getModules() {
        return modules;
    }

    @GraphQLField
    @GraphQLDescription("Include mixin types (default true)")
    public Boolean getIncludeMixins() {
        return includeMixins != null ? includeMixins : true;
    }

    @GraphQLField
    @GraphQLDescription("Include non mixin types (default true)")
    public Boolean getIncludeNonMixins() {
        return includeNonMixins != null ? includeNonMixins : true;
    }

    @GraphQLField
    @GraphQLDescription("Consider only nodetypes for the specified site")
    public String getSiteKey() {
        return siteKey;
    }

    @GraphQLField
    @GraphQLDescription("Include abstract types (default true)")
    public Boolean getIncludeAbstract() {
        return includeAbstract != null ? includeAbstract : true;
    }

    @GraphQLField
    @GraphQLDescription("Only include types specified by this list (also considering sy-types, if considerSubTypes is true)")
    public List<String> getIncludedTypes() {
        return includedTypes;
    }

    @GraphQLField
    @GraphQLDescription("Consider sub-types when checking for included/excluded nodetypes (default true)")
    public Boolean getConsiderSubTypes() {
        return considerSubTypes != null ? considerSubTypes : true;
    }

    @GraphQLField
    @GraphQLDescription("Exclude the types, specified by this list (also considering sy-types, if considerSubTypes is true)")
    public List<String> getExcludedTypes() {
        return excludedTypes;
    }
}
