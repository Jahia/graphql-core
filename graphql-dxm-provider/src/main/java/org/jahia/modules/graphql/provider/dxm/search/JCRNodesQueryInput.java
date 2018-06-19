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

import java.util.List;

/**
 * Short description of the class
 *
 * @author yousria
 */
public class JCRNodesQueryInput {

    private String nodeType;
    private List<String> basePaths;
    private boolean includeDescendants;
    private Constraint constraint;
    private List<String> ordering;
    private String language;

    public JCRNodesQueryInput(@GraphQLName("nodeType") @GraphQLDescription("type of node") String nodeType,
            @GraphQLName("basePaths") @GraphQLDescription("paths") List<String> basePaths,
            @GraphQLName("includeDescendants") @GraphQLDescription("include or not descendants") boolean includeDescendants,
            @GraphQLName("constraint") @GraphQLDescription("object for constraints") Constraint constraint,
            @GraphQLName("ordering") @GraphQLDescription("orderings") List<String> ordering,
            @GraphQLName("language") @GraphQLDescription("language") String language) {

        this.nodeType = nodeType;
        this.basePaths = basePaths;
        this.includeDescendants = includeDescendants;
        this.constraint = constraint;
        this.ordering = ordering;
        this.language = language;
    }

    @GraphQLField
    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    @GraphQLField
    public List<String> getBasePaths() {
        return basePaths;
    }

    public void setBasePaths(List<String> basePaths) {
        this.basePaths = basePaths;
    }

    @GraphQLField
    public boolean isIncludeDescendants() {
        return includeDescendants;
    }

    public void setIncludeDescendants(boolean includeDescendants) {
        this.includeDescendants = includeDescendants;
    }

    @GraphQLField
    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    @GraphQLField
    public List<String> getOrdering() {
        return ordering;
    }

    public void setOrdering(List<String> ordering) {
        this.ordering = ordering;
    }

    @GraphQLField
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
