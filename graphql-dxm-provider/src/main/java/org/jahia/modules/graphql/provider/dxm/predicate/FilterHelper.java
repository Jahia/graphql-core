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
import graphql.relay.Connection;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilterHelper {

    enum FieldEvaluation {

        /**
         * The field value is equal to given one.
         */
        @GraphQLDescription("The field value is equal to given one")
        EQUAL,

        /**
         * The field value is different from given one.
         */
        @GraphQLDescription("The field value is different from given one")
        DIFFERENT,

        /**
         * The field value is empty
         */
        @GraphQLDescription("The field value is empty - either null value, or no items for a list")
        EMPTY,

        @GraphQLDescription("The field value is not empty - if a list, must contain at least one item")
        NOT_EMPTY,

        @GraphQLDescription("The property value contains given String ")
        CONTAINS,

        @GraphQLDescription("The property value contains given String ignoring the case")
        CONTAINS_IGNORE_CASE,

        @GraphQLDescription("The property value is among given Strings")
        AMONG
    }

    private static HashMap<FieldEvaluation, FieldEvaluationAlgorithm> ALGORITHM_BY_EVALUATION = new HashMap<>();

    @FunctionalInterface
    interface FieldEvaluationAlgorithm {
        boolean evaluate(Object source, String fieldName, String fieldValue, Collection<String> fieldValues, FieldEvaluator environment);
    }

    static {
        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.EQUAL, ((source, fieldName, fieldValue, fieldValues, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            return value != null && value.toString().equals(fieldValue);
        }));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.DIFFERENT, ((source, fieldName, fieldValue, fieldValues, environment) ->
            !ALGORITHM_BY_EVALUATION.get(FieldEvaluation.EQUAL).evaluate(source, fieldName, fieldValue, fieldValues, environment)
        ));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.EMPTY, ((source, fieldName, fieldValue, fieldValues, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            if (value instanceof Connection) {
                return ((Connection<?>) value).getEdges().size() == 0;
            } else if (value instanceof Collection) {
                return ((Collection<?>) value).size() == 0;
            } else {
                return value == null;
            }
        }));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.NOT_EMPTY, ((source, fieldName, fieldValue, fieldValues, environment) ->
            !ALGORITHM_BY_EVALUATION.get(FieldEvaluation.EMPTY).evaluate(source, fieldName, fieldValue, fieldValues, environment)
        ));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.CONTAINS, ((source, fieldName, fieldValue, fieldValues, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            return value != null && StringUtils.contains(value.toString(), fieldValue);
        }));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.CONTAINS_IGNORE_CASE, ((source, fieldName, fieldValue, fieldValues, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            return value != null && StringUtils.containsIgnoreCase(value.toString(), fieldValue);
        }));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.AMONG, ((source, fieldName, fieldValue, fieldValues, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            return value != null && fieldValues.contains(value.toString());
        }));
    }

    /**
     * Get a predicate based on evaluation of parameter field filters
     */
    public static Predicate<Object> getFieldPredicate(FieldFiltersInput filters, FieldEvaluator environment) {
        Predicate<Object> fieldPredicate;
        if (filters == null) {
            fieldPredicate = PredicateHelper.truePredicate();
        } else {
            LinkedList<Predicate<Object>> predicates = new LinkedList<>();
            for (FieldFilterInput filter : filters.getFilters()) {
                predicates.add(getFieldPredicate(filter, environment));
            }
            fieldPredicate = PredicateHelper.getCombinedPredicate(predicates, filters.getMulticriteriaEvaluation(), MulticriteriaEvaluation.ALL);
        }
        return fieldPredicate;
    }

    private static Predicate<Object> getFieldPredicate(FieldFilterInput filter, FieldEvaluator environment) {
        FieldFiltersInput subFilter = filter.getFieldFilter();
        if (subFilter == null) {
            FieldEvaluation evaluation = filter.getEvaluation();
            if (evaluation == null) {
                evaluation = FieldEvaluation.EQUAL;
            }
            FieldEvaluationAlgorithm evaluationAlgorithm = ALGORITHM_BY_EVALUATION.get(evaluation);
            if (evaluationAlgorithm == null) {
                throw new IllegalArgumentException("Unknown field evaluation: " + evaluation);
            }
            return (object -> evaluationAlgorithm.evaluate(object, filter.getFieldName(), filter.getValue(), filter.getValues(), environment));
        } else {
            return getFieldPredicate(subFilter, environment);
        }
    }

    public static <T> List<T> filterList(List<T> list, FieldFiltersInput fieldFilter, DataFetchingEnvironment environment) {
        if (fieldFilter == null || fieldFilter.getFilters().isEmpty()) {
            return list;
        }
        return list.stream().filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forList(environment))).collect(Collectors.toList());
    }

    public static <T> List<T> filterConnection(List<T> list, FieldFiltersInput fieldFilter, DataFetchingEnvironment environment) {
        if (fieldFilter == null || fieldFilter.getFilters().isEmpty()) {
            return list;
        }
        return list.stream().filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forConnection(environment))).collect(Collectors.toList());
    }
}
