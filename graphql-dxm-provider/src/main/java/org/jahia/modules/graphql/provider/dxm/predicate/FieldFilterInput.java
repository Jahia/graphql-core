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
package org.jahia.modules.graphql.provider.dxm.predicate;

import java.util.Collection;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * Input object representing either a sub-filter (so that nested conditional logic can be composed), or a condition to filter by a single field.
 */
@GraphQLDescription("Input object representing either a sub-filter (so that nested conditional logic can be composed), or a condition to filter by a single field")
public class FieldFilterInput {

    private FieldFiltersInput fieldFilter;
    private String fieldName;
    private String value;
    private Collection<String> values;
    private FilterHelper.FieldEvaluation evaluation;

    /**
     * Create an input object representing either a sub-filter, or a condition to filter by a single field.
     *
     * @param fieldFilter A non-null sub-filter, so that nested conditional logic can be composed (in this case all other parameters do not matter), or null if the input object represents a simple field filter configured via its other parameters
     *
     * @param fieldName The name of the field or its alias to filter by
     * @param value The value to evaluate the field against (for single-valued evaluations)
     * @param values The values to evaluate the field against (for multi-valued evaluations)
     * @param evaluation The way to evaluate the property; null indicates default (EQUAL)
     */
    public FieldFilterInput(
        @GraphQLName("fieldFilter") FieldFiltersInput fieldFilter,
        @GraphQLName("fieldName") String fieldName,
        @GraphQLName("value") String value,
        @GraphQLName("values") Collection<String> values,
        @GraphQLName("evaluation") FilterHelper.FieldEvaluation evaluation
    ) {
        this.fieldFilter = fieldFilter;
        this.fieldName = fieldName;
        this.value = value;
        this.values = values;
        this.evaluation = evaluation;
    }

    @GraphQLField
    @GraphQLDescription("Either a non-null sub-filter, or null in case the input object represents a simple field filter configured via its other properties")
    public FieldFiltersInput getFieldFilter() {
        return fieldFilter;
    }

    @GraphQLField
    @GraphQLDescription("The name of the field or its alias to filter by")
    public String getFieldName() {
        return fieldName;
    }

    @GraphQLField
    @GraphQLDescription("The value to evaluate the field against (for single-valued operations)")
    public String getValue() {
        return value;
    }

    @GraphQLField
    @GraphQLDescription("The values to evaluate the field against (for multi-valued operations)")
    public Collection<String> getValues() {
        return values;
    }

    @GraphQLField
    @GraphQLDescription("The way to evaluate the property; null indicates default (EQUAL)")
    public FilterHelper.FieldEvaluation getEvaluation() {
        return evaluation;
    }
}
