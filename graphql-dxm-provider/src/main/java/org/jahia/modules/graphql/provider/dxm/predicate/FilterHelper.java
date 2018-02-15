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
import graphql.execution.ValuesResolver;
import graphql.language.*;
import graphql.relay.Connection;
import graphql.schema.*;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.functors.TruePredicate;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static graphql.schema.DataFetchingEnvironmentBuilder.newDataFetchingEnvironment;

public class FilterHelper {

    enum FieldEvaluation {


        /**
         * The field value is equal to given one.
         */
        @GraphQLDescription("The property value is equal to given one")
        EQUAL,

        /**
         * The field value is different from given one.
         */
        @GraphQLDescription("The property value is different from given one")
        DIFFERENT,

        /**
         * The field value is empty
         */
        @GraphQLDescription("The field value is empty - either null value, or no items for a list")
        EMPTY,

        @GraphQLDescription("The field value is not empty - if a list, must contains at least one item")
        NOT_EMPTY
    }

    private static HashMap<FieldEvaluation, FieldEvaluationAlgorithm> ALGORITHM_BY_EVALUATION = new HashMap<>();

    @FunctionalInterface
    interface FieldEvaluationAlgorithm {
        boolean evaluate(Object source, String fieldName, String fieldValue, FieldEvaluationEnvironment environment);
    }

    static {
        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.EQUAL, ((source, fieldName, fieldValue, environment) -> {
            Object value = getField(source, fieldName, environment);
            return value != null && value.toString().equals(fieldValue);
        }));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.DIFFERENT, ((source, fieldName, fieldValue, environment) ->
                !ALGORITHM_BY_EVALUATION.get(FieldEvaluation.EQUAL).evaluate(source, fieldName, fieldValue, environment)
        ));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.EMPTY, ((source, fieldName, fieldValue, environment) -> {
            Object value = getField(source, fieldName, environment);
            if (value instanceof Connection) {
                return ((Connection) value).getEdges().size() == 0;
            } else if (value instanceof Collection) {
                return ((Collection) value).size() == 0;
            } else {
                return value == null;
            }
        }));

        ALGORITHM_BY_EVALUATION.put(FieldEvaluation.NOT_EMPTY, ((source, fieldName, fieldValue, environment) ->
                !ALGORITHM_BY_EVALUATION.get(FieldEvaluation.EMPTY).evaluate(source, fieldName, fieldValue, environment)
        ));
    }

    /**
     * Evaluate a field value on a given object
     *
     * @param source The source object on which we will get the field value
     * @param fieldName The field name or alias
     * @param environment The environment
     * @return The value, as returned by the DataFetcher
     */
    private static Object getField(Object source, String fieldName, FieldEvaluationEnvironment environment) {
        GraphQLFieldsContainer objectType = environment.getObjectType(source);
        DataFetchingEnvironmentBuilder fieldEnv = newDataFetchingEnvironment().source(source);

        if (environment.getSelectionSet() != null) {
            // Try to find field in selection set to reuse alias/arguments
            Field field = findField(environment.getSelectionSet(), fieldName);
            if (field != null) {
                GraphQLFieldDefinition fieldDefinition = objectType.getFieldDefinition(field.getName());
                if (fieldDefinition == null) {
                    // Definition not present on current type (can be a field in a non-matching fragment), returns null
                    return null;
                }

                ValuesResolver valuesResolver = new ValuesResolver();
                Map<String, Object> argumentValues = valuesResolver.getArgumentValues(fieldDefinition.getArguments(), field.getArguments(), environment.getVariables());
                fieldEnv.arguments(argumentValues);
                return fieldDefinition.getDataFetcher().get(fieldEnv.build());
            }
        }

        // Otherwise, directly look in field definitions
        GraphQLFieldDefinition fieldDefinition = objectType.getFieldDefinition(fieldName);
        if (fieldDefinition == null) {
            // Definition not present on current type (can be a field in a non-matching fragment), returns null
            return null;
        }
        return fieldDefinition.getDataFetcher().get(fieldEnv.build());
    }

    private static Field findField(SelectionSet set, String name) {
        for (Selection selection : set.getSelections()) {
            if (selection instanceof Field) {
                Field field = (Field) selection;
                String nameOrAlias = field.getAlias() != null ? field.getAlias() : field.getName();
                if (nameOrAlias.equals(name)) {
                    return field;
                }
            } else if (selection instanceof InlineFragment) {
                Field f = findField(((InlineFragment) selection).getSelectionSet(), name);
                if (f != null) {
                    return f;
                }
            } else if (selection instanceof FragmentSpread) {
                // Not supported, skip
            }
        }
        return null;
    }

    /**
     * Get a predicate based on the value of a sub field evaluation
     */
    public static Predicate<Object> getFieldPredicate(FieldFiltersInput fieldFilters, FieldEvaluationEnvironment environment) {
        Predicate<Object> fieldPredicate;
        if (fieldFilters == null) {
            fieldPredicate = TruePredicate.truePredicate();
        } else {
            LinkedList<Predicate<Object>> predicates = new LinkedList<>();
            for (FieldFilterInput fieldFilter : fieldFilters.getFilters()) {
                FieldEvaluation evaluation = fieldFilter.getEvaluation();
                if (evaluation == null) {
                    evaluation = FieldEvaluation.EQUAL;
                }
                FieldEvaluationAlgorithm evaluationAlgorithm = ALGORITHM_BY_EVALUATION.get(evaluation);
                if (evaluationAlgorithm == null) {
                    throw new IllegalArgumentException("Unknown field evaluation: " + evaluation);
                }

                predicates.add(object -> evaluationAlgorithm.evaluate(object, fieldFilter.getFieldName(), fieldFilter.getValue(), environment));
            }
            fieldPredicate = PredicateHelper.getCombinedPredicate(predicates, fieldFilters.getMulticriteriaEvaluation(), MulticriteriaEvaluation.ALL);
        }
        return fieldPredicate;
    }

}
