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
