/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm.search;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.Collection;
import java.util.List;

/**
 * Criteria input passed to the getNodesByCriteria graphql field
 * contains attributes required for the query
 *
 * @author yousria
 */
public class JCRNodesQueryInput {

    private String nodeType;
    private Collection<String> basePaths;
    private boolean includeDescendants;
    private Constraint constraint;
    private List<String> ordering;
    private String language;

    /**
     * TODO : review what's required here and put the @GraphQLNonNull, for now only nodeType is required
     * @param nodeType
     * @param basePaths
     * @param includeDescendants
     * //@param constraint
     * @param ordering
     * @param language
     */
    public JCRNodesQueryInput(@GraphQLName("nodeType") @GraphQLDescription("type of nodes to query") String nodeType,
            @GraphQLName("basePaths") @GraphQLDescription("paths of nodes queried") List<String> basePaths,
            @GraphQLName("includeDescendants") @GraphQLDescription("include or not descendants of nodes queried") boolean includeDescendants,
            //@GraphQLName("constraint") @GraphQLDescription("constraint object for the where clause") Constraint constraint,
            @GraphQLName("ordering") @GraphQLDescription("orderings strategies") List<String> ordering,
            @GraphQLName("language") @GraphQLDescription("language") String language) {

        this.nodeType = nodeType;
        this.basePaths = basePaths;
        this.includeDescendants = includeDescendants;
        //this.constraint = constraint;
        this.ordering = ordering;
        this.language = language;
    }

    @GraphQLField
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

    // TODO: uncomment after implementing the Constraint Object
    /*@GraphQLField
    @GraphQLDescription("constraint object for the where clause")
    public Constraint getConstraint() {
        return constraint;
    }*/

    @GraphQLField
    @GraphQLDescription("orderings strategies")
    public List<String> getOrdering() {
        return ordering;
    }

    @GraphQLField
    @GraphQLDescription("language")
    public String getLanguage() {
        return language;
    }
}
