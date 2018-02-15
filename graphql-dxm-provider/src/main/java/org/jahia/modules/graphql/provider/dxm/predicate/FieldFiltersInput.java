/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.predicate;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import java.util.Collection;


/**
 * Filter any GraphQL node based on a sub-fields values
 */
@GraphQLDescription("Filter any GraphQL node based on a sub-fields values")
public class FieldFiltersInput {

    private MulticriteriaEvaluation multicriteriaEvaluation;
    private Collection<FieldFilterInput> filters;

    /**
     * Create a filter instance.
     *
     * @param multicriteriaEvaluation The way to combine multiple individual field filters; null to use ALL by default
     * @param filters Individual field filters
     */
    public FieldFiltersInput(@GraphQLName("multi") MulticriteriaEvaluation multicriteriaEvaluation,
                             @GraphQLName("filters") @GraphQLNonNull Collection<FieldFilterInput> filters) {
        this.multicriteriaEvaluation = multicriteriaEvaluation;
        this.filters = filters;
    }

    /**
     * @return The way to combine multiple individual field filters; null indicates default (ALL)
     */
    @GraphQLField
    @GraphQLName("multi")
    @GraphQLDescription("The way to combine multiple individual property filters; null indicates default (ALL)")
    public MulticriteriaEvaluation getMulticriteriaEvaluation() {
        return multicriteriaEvaluation;
    }

    /**
     * @return Individual field filters
     */
    @GraphQLField
    @GraphQLName("filters")
    @GraphQLNonNull
    @GraphQLDescription("Individual property filters")
    public Collection<FieldFilterInput> getFilters() {
        return filters;
    }
}
