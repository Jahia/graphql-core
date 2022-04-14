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

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Class with sorting algorithm
 *
 * @author ybenchadi
 */
public class SorterHelper {

    public enum SortType {

        /**
         * Ascendant order
         */
        @GraphQLDescription("Ascendant order")
        ASC,

        /**
         * Descendant order
         */
        @GraphQLDescription("Descendant order")
        DESC
    }

    private static final Map<SortType, FieldSorterAlgorithm> SORT_BY_DIRECTION = new EnumMap<>(SortType.class);

    @FunctionalInterface interface FieldSorterAlgorithm {
        int evaluate(Object source, String fieldName, Object fieldValue, boolean ignoreCase, FieldEvaluator environment);
    }

    static {
        FieldSorterAlgorithm fieldSorterAlgorithm = (source, fieldName, fieldValue, ignoreCase, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            if (value == null && fieldValue == null) {
                return 0;
            } else if (value == null) {
                return -1;
            } else if (fieldValue == null) {
                return 1;
            } else if (fieldValue instanceof Boolean) {
                return Boolean.compare((Boolean) value, (Boolean) fieldValue);
            } else if (fieldValue instanceof Long) {
                return Long.compare((Long) value, (Long) fieldValue);
            } else if (fieldValue instanceof Double) {
                return Double.compare((Double) value, (Double) fieldValue);
            } else if (fieldValue instanceof Integer) {
                return Integer.compare((Integer) value, (Integer) fieldValue);
            } else if (fieldValue instanceof String) {
                return ignoreCase ? ((String)value).compareToIgnoreCase(((String)fieldValue)) : ((String)value).compareTo(((String)fieldValue));
            } else if (fieldValue instanceof Enum) {
                return value.toString().compareTo(fieldValue.toString());
            } else {
                return 0;
            }
        };

        SORT_BY_DIRECTION.put(SortType.ASC, fieldSorterAlgorithm);
        SORT_BY_DIRECTION.put(SortType.DESC, ((source, fieldName, fieldValue, ignoreCase, environment) ->
            -fieldSorterAlgorithm.evaluate(source, fieldName, fieldValue, ignoreCase, environment)
        ));

    }

    public static Comparator<Object> getFieldComparator(FieldSorterInput sortFilter, FieldEvaluator environment) {
        SorterHelper.SortType sortType = sortFilter.getSortType();
        if (sortType == null) {
            sortType = SorterHelper.SortType.ASC;
        }
        SorterHelper.FieldSorterAlgorithm sorterAlgorithm = SORT_BY_DIRECTION.get(sortType);
        if (sorterAlgorithm == null) {
            throw new IllegalArgumentException("Unknown sort direction : " + sortType);
        }
        return ((object, obj) -> sorterAlgorithm.evaluate(object, sortFilter.getFieldName(), environment.getFieldValue(obj,
                sortFilter.getFieldName()), (sortFilter.isIgnoreCase()==null || sortFilter.isIgnoreCase()), environment));
    }
}
