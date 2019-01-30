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
package org.jahia.modules.graphql.provider.dxm.predicate;

import graphql.annotations.annotationTypes.GraphQLDescription;
import org.jahia.modules.graphql.provider.dxm.node.FieldSorterInput;

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
        SORT_BY_DIRECTION.put(SortType.ASC, ((source, fieldName, fieldValue, ignoreCase, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            if (value == null && fieldValue == null) {
                return 0;
            } else if (value == null && fieldValue != null) {
                return -1;
            } else if (value != null && fieldValue == null) {
                return 1;
            } else if (fieldValue instanceof Boolean) {
                return Boolean.compare((Boolean)value, (Boolean)fieldValue);
            } else if (fieldValue instanceof Long) {
                return Long.compare((Long)value, (Long)fieldValue);
            } else if (fieldValue instanceof Double) {
                return Double.compare((Double)value, (Double)fieldValue);
            } else {
                return ignoreCase ? value.toString().compareToIgnoreCase((String)fieldValue) : value.toString().compareTo((String)fieldValue);
            }
        }));

        SORT_BY_DIRECTION.put(SortType.DESC, ((source, fieldName, fieldValue, ignoreCase, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            if (value == null && fieldValue == null) {
                return 0;
            } else if (value == null && fieldValue != null) {
                return 1;
            } else if (value != null && fieldValue == null) {
                return -1;
            } else if (fieldValue instanceof Boolean) {
                return Boolean.compare((Boolean)fieldValue, (Boolean)value);
            } else if (fieldValue instanceof Long) {
                return Long.compare((Long)fieldValue, (Long)value);
            } else if (fieldValue instanceof Double) {
                return Double.compare((Double)fieldValue, (Double)value);
            } else {
                return ignoreCase ? -(value.toString().compareToIgnoreCase((String)fieldValue)) : -(value.toString().compareTo((String)fieldValue));
            }
        }));

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
