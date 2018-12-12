/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
        @GraphQLDescription("The specified path is an ancestor, so all its descendants will be considered in the query")
        ANCESTOR,

        /**
         * The path defines the parent of nodes to fetch.
         */
        @GraphQLDescription("The specified path is a parent, so all its direct children will be considered in the query")
        PARENT,

        /**
         * The path defines own path of the node to fetch.
         */
        @GraphQLDescription("The specified path is a node itself, so only this node will be considered in the query")
        OWN
    }

    private String nodeType;
    private PathType pathType;
    private Collection<String> paths;
    private GqlJcrNodeConstraintInput nodeConstraint;
    private String language;
    private GqlOrdering ordering;
    private List<GqlJcrNodeConstraintInput> all;
    private List<GqlJcrNodeConstraintInput> any;
    private List<GqlJcrNodeConstraintInput> none;

    /**
     * Create a criteria input instance.
     *
     * @param nodeType The type of nodes to fetch
     * @param pathType The exact meaning of the paths parameter, null means the default (ANCESTOR)
     * @param paths Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType parameter; null or empty collection means no path restrictions
     * @param nodeConstraint Additional constraint to filter nodes by their arbitrary properties
     * @param language Language to access node properties in; must be a valid language code in case any internationalized properties are used for filtering, does not matter for non-internationalized ones
     * @param ordering ordering strategies
     */
    public GqlJcrNodeCriteriaInput(
        @GraphQLName("nodeType") @GraphQLNonNull @GraphQLDescription("The type of nodes to fetch") String nodeType,
        @GraphQLName("pathType") @GraphQLDescription("The exact meaning of the paths parameter, ANCESTOR by default") PathType pathType,
        @GraphQLName("paths") @GraphQLDescription("Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType parameter; null or empty array means no path restrictions") Collection<String> paths,
        @GraphQLName("nodeConstraint") @GraphQLDescription("Additional constraint to filter nodes by their arbitrary properties") GqlJcrNodeConstraintInput nodeConstraint,
        @GraphQLName("language") @GraphQLDescription("Language to access node properties in; must be a valid language code in case any internationalized properties are used for filtering, does not matter for non-internationalized ones") String language,
        @GraphQLName("ordering") @GraphQLDescription("ordering strategies") GqlOrdering ordering,
        @GraphQLName("all") @GraphQLDescription("constraints list for all the items should be matched") List<GqlJcrNodeConstraintInput> all,
        @GraphQLName("any") @GraphQLDescription("constraints list for anyone of the items should be matched") List<GqlJcrNodeConstraintInput> any,
        @GraphQLName("none") @GraphQLDescription("constraints list for none of the items should be matched") List<GqlJcrNodeConstraintInput> none
    ) {
        this.nodeType = nodeType;
        this.paths = paths;
        this.pathType = pathType;
        this.nodeConstraint = nodeConstraint;
        this.ordering = ordering;
        this.language = language;
        this.all = all;
        this.any = any;
        this.none = none;
    }

    /**
     * @return The type of nodes to fetch
     */
    @GraphQLField
    @GraphQLName("nodeType")
    @GraphQLNonNull
    @GraphQLDescription("The type of nodes to query")
    public String getNodeType() {
        return nodeType;
    }

    /**
     * @return The exact meaning of the paths field
     */
    @GraphQLField
    @GraphQLName("pathType")
    @GraphQLDescription("The exact meaning of the paths field")
    public PathType getPathType() {
        return pathType;
    }

    /**
     * @return Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType field; null or empty collection means no path restrictions
     */
    @GraphQLField
    @GraphQLName("paths")
    @GraphQLDescription("Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType field; null or empty collection means no path restrictions")
    public Collection<String> getPaths() {
        return paths;
    }

    /**
     * @return Additional constraint to filter nodes by their arbitrary properties
     */
    @GraphQLField
    @GraphQLName("nodeConstraint")
    @GraphQLDescription("Additional constraint to filter nodes by their arbitrary properties")
    public GqlJcrNodeConstraintInput getNodeConstraint() {
        return nodeConstraint;
    }

    /**
     * @return Language to access node properties in
     */
    @GraphQLField
    @GraphQLName("language")
    @GraphQLDescription("Language to access node properties in")
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLName("ordering")
    @GraphQLDescription("ordering strategies")
    public GqlOrdering getOrdering() {
        return ordering;
    }

    @GraphQLField
    @GraphQLDescription("constraints list for all the items should be matched")
    public List<GqlJcrNodeConstraintInput> getAll() {
        return all;
    }

    @GraphQLField
    @GraphQLDescription("constraints list for anyone of the items should be matched")
    public List<GqlJcrNodeConstraintInput> getAny() {
        return any;
    }

    @GraphQLField
    @GraphQLDescription("constraints list for none of the items should be matched")
    public List<GqlJcrNodeConstraintInput> getNone() {
        return none;
    }
}
