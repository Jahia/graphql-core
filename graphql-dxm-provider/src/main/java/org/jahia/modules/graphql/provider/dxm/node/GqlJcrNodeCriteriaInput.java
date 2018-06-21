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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import java.util.Collection;
import java.util.List;

// TODO: When implementing specific criteria fields, review JavaDoc and GraphQL descriptions, as well as specific constructor parameter and getter annotations to see if any of them need @GraphQLNonNull.

/**
 * Criteria to fetch JCR nodes by.
 *
 * @author yousria
 */
public class GqlJcrNodeCriteriaInput {

    /**
     * A part of the criteria to filter JCR nodes, specifically by their arbitrary properties.
     */
    @GraphQLDescription("A part of the criteria to filter JCR nodes, specifically by their arbitrary properties")
    public static class NodeConstraint {
        // TODO : Implement in scope of BACKLOG-8027.
    }

    private String nodeType;
    private Collection<String> basePaths;
    private boolean includeDescendants;
//    private NodeConstraint constraint;
    private List<String> ordering;
    private String language;

    /**
     * Create a criteria input instance.
     *
     * @param nodeType The type of nodes to fetch
     * @param basePaths
     * @param includeDescendants
     * //@param constraint
     * @param ordering
     * @param language
     */
    public GqlJcrNodeCriteriaInput(
        @GraphQLName("nodeType") @GraphQLNonNull @GraphQLDescription("The type of nodes to fetch") String nodeType,
        @GraphQLName("basePaths") @GraphQLDescription("paths of nodes queried") List<String> basePaths,
        @GraphQLName("includeDescendants") @GraphQLDescription("include or not descendants of nodes queried") boolean includeDescendants,
//        @GraphQLName("constraint") @GraphQLDescription("Additional constraint to filter nodes by their arbitrary properties") Constraint constraint,
        @GraphQLName("ordering") @GraphQLDescription("ordering strategies") List<String> ordering,
        @GraphQLName("language") @GraphQLDescription("language") String language)
    {
        this.nodeType = nodeType;
        this.basePaths = basePaths;
        this.includeDescendants = includeDescendants;
//        this.constraint = constraint;
        this.ordering = ordering;
        this.language = language;
    }

    /**
     * @return The type of nodes to fetch
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("type of nodes to query")
    public String getNodeType() {
        return nodeType;
    }

    @GraphQLField
    @GraphQLDescription("paths of nodes queried")
    public Collection<String> getBasePaths() {
        return basePaths;
    }

    @GraphQLField
    @GraphQLDescription("include or not descendants of nodes queried")
    public boolean isIncludeDescendants() {
        return includeDescendants;
    }

//    /**
//     * @return Additional constraint to filter nodes by their arbitrary properties
//     */
//    @GraphQLField
//    @GraphQLDescription("Additional constraint to filter nodes by their arbitrary properties")
//    public NodeConstraint getNodeConstraint() {
//        return constraint;
//    }

    @GraphQLField
    @GraphQLDescription("ordering strategies")
    public List<String> getOrdering() {
        return ordering;
    }

    @GraphQLField
    @GraphQLDescription("language")
    public String getLanguage() {
        return language;
    }
}
