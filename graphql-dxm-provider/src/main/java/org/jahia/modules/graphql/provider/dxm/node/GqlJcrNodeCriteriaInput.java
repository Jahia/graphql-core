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
     * Possible meanings of path values passed as a part of the criteria.
     */
    public enum PathType {

        /**
         * The path defines the ancestor of nodes to fetch.
         */
        ANCESTOR,

        /**
         * The path defines the parent of nodes to fetch.
         */
        PARENT,

        /**
         * The path defines own path of the node to fetch.
         */
        OWN
    }

    /**
     * A part of the criteria to filter JCR nodes, specifically by their arbitrary properties.
     */
    @GraphQLDescription("A part of the criteria to filter JCR nodes, specifically by their arbitrary properties")
    public static class NodeConstraint {
        // TODO : Implement in scope of BACKLOG-8027.
    }

    private String nodeType;
    private PathType pathType;
    private Collection<String> paths;
//    private NodeConstraint nodeConstraint;
    private String language;
    private List<String> ordering;

    /**
     * Create a criteria input instance.
     *
     * @param nodeType The type of nodes to fetch
     * @param pathType The exact meaning of the paths parameter, null means OWN (default)
     * @param paths Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType parameter
     * //@param nodeConstraint
     * @param language Language to access node properties in; must be a valid language code in case any internationalized properties are analyzed or fetched, does not matter for non-internationalized ones
     * @param ordering
     */
    public GqlJcrNodeCriteriaInput(
            @GraphQLName("nodeType") @GraphQLNonNull @GraphQLDescription("The type of nodes to fetch") String nodeType,
            @GraphQLName("pathType") @GraphQLDescription("The exact meaning of the paths parameter, (OWN by default)") PathType pathType,
            @GraphQLName("paths") @GraphQLNonNull @GraphQLDescription("Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType parameter") Collection<String> paths,
//            @GraphQLName("nodeConstraint") @GraphQLDescription("Additional constraint to filter nodes by their arbitrary properties") NodeConstraint nodeConstraint,
            @GraphQLName("language") @GraphQLDescription("Language to access node properties in; must be a valid language code in case any internationalized properties are analyzed or fetched, does not matter for non-internationalized ones") String language,
            @GraphQLName("ordering") @GraphQLDescription("ordering strategies") List<String> ordering
    ) {
        this.nodeType = nodeType;
        this.paths = paths;
        this.pathType = pathType;
//        this.nodeConstraint = nodeConstraint;
        this.ordering = ordering;
        this.language = language;
    }

    /**
     * @return The type of nodes to fetch
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("The type of nodes to query")
    public String getNodeType() {
        return nodeType;
    }

    /**
     * @return The exact meaning of the paths field, null means OWN (default)
     */
    @GraphQLField
    @GraphQLDescription("The exact meaning of the paths parameter, null means OWN (default)")
    public PathType getPathType() {
        return pathType;
    }

    /**
     * @return
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType field")
    public Collection<String> getPaths() {
        return paths;
    }

//    /**
//     * @return Additional constraint to filter nodes by their arbitrary properties
//     */
//    @GraphQLField
//    @GraphQLDescription("Additional constraint to filter nodes by their arbitrary properties")
//    public NodeConstraint getNodeConstraint() {
//        return nodeConstraint;
//    }

    /**
     * @return Language to access node properties in
     */
    @GraphQLField
    @GraphQLDescription("Language to access node properties in")
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLDescription("ordering strategies")
    public List<String> getOrdering() {
        return ordering;
    }
}
