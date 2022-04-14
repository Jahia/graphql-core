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
    @GraphQLName("fieldFilter")
    @GraphQLDescription("Either a non-null sub-filter, or null in case the input object represents a simple field filter configured via its other properties")
    public FieldFiltersInput getFieldFilter() {
        return fieldFilter;
    }

    @GraphQLField
    @GraphQLName("fieldName")
    @GraphQLDescription("The name of the field or its alias to filter by")
    public String getFieldName() {
        return fieldName;
    }

    @GraphQLField
    @GraphQLName("value")
    @GraphQLDescription("The value to evaluate the field against (for single-valued operations)")
    public String getValue() {
        return value;
    }

    @GraphQLField
    @GraphQLName("values")
    @GraphQLDescription("The values to evaluate the field against (for multi-valued operations)")
    public Collection<String> getValues() {
        return values;
    }

    @GraphQLField
    @GraphQLName("evaluation")
    @GraphQLDescription("The way to evaluate the property; null indicates default (EQUAL)")
    public FilterHelper.FieldEvaluation getEvaluation() {
        return evaluation;
    }
}
