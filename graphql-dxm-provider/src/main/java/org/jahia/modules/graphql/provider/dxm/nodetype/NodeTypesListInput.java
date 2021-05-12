/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
