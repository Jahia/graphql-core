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
    public FieldFiltersInput(@GraphQLName("multi") @GraphQLDescription("The way to combine multiple individual field filters; null to use ALL by default") MulticriteriaEvaluation multicriteriaEvaluation,
                             @GraphQLName("filters") @GraphQLNonNull @GraphQLDescription("Individual field filters") Collection<FieldFilterInput> filters) {
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
