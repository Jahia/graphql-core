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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import java.util.Collection;

// TODO: When implementing specific criteria fields, review JavaDoc and GraphQL descriptions, as well as specific constructor parameter and getter annotations to see if any of them need @GraphQLNonNull.

/**
 * Criteria to fetch JCR nodes by.
 *
 * @author yousria
 */
@GraphQLDescription("Node criterias")
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

    /**
     * Create a criteria input instance.
     *
     * @param nodeType The type of nodes to fetch
     * @param pathType The exact meaning of the paths parameter, null means the default (ANCESTOR)
     * @param paths Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType parameter; null or empty collection means no path restrictions
     * @param nodeConstraint Additional constraint to filter nodes by their arbitrary properties
     * @param language Language to access node properties in; must be a valid language code in case any internationalized properties are used for filtering, does not matter for non-internationalized ones
     * @param ordering Ordering strategies
     */
    public GqlJcrNodeCriteriaInput(
        @GraphQLName("nodeType") @GraphQLNonNull @GraphQLDescription("The type of nodes to fetch") String nodeType,
        @GraphQLName("pathType") @GraphQLDescription("The exact meaning of the paths parameter, ANCESTOR by default") PathType pathType,
        @GraphQLName("paths") @GraphQLDescription("Paths that restrict areas to fetch nodes from; the exact meaning is defined by the pathType parameter; null or empty array means no path restrictions") Collection<String> paths,
        @GraphQLName("nodeConstraint") @GraphQLDescription("Additional constraint to filter nodes by their arbitrary properties") GqlJcrNodeConstraintInput nodeConstraint,
        @GraphQLName("language") @GraphQLDescription("Language to access node properties in; must be a valid language code in case any internationalized properties are used for filtering, does not matter for non-internationalized ones") String language,
        @GraphQLName("ordering") @GraphQLDescription("Ordering strategies") GqlOrdering ordering
    ) {
        this.nodeType = nodeType;
        this.paths = paths;
        this.pathType = pathType;
        this.nodeConstraint = nodeConstraint;
        this.ordering = ordering;
        this.language = language;
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

    /**
     * @return ordering strategies
     */
    @GraphQLField
    @GraphQLName("ordering")
    @GraphQLDescription("Ordering strategies")
    public GqlOrdering getOrdering() {
        return ordering;
    }
}
