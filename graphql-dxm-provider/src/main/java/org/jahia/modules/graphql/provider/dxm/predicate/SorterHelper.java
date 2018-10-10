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
import org.apache.commons.collections.ComparatorUtils;
import org.jahia.modules.graphql.provider.dxm.node.FieldSorterInput;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

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

    private static HashMap<SortType, SorterHelper.FieldSorterAlgorithm> SORT_BY_DIRECTION = new HashMap<>();

    @FunctionalInterface interface FieldSorterAlgorithm {
        int evaluate(Object source, String fieldName, String fieldValue, FieldEvaluator environment);
    }

    static {
        SORT_BY_DIRECTION.put(SortType.ASC, ((source, fieldName, fieldValue, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            return value != null ? value.toString().compareTo(fieldValue) : 0;
        }));

        SORT_BY_DIRECTION.put(SortType.DESC, ((source, fieldName, fieldValue, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            return value != null ? -(value.toString().compareTo(fieldValue)) : 0;
        }));

    }

    public static Comparator<Object> getFieldComparator(FieldSorterInput sortFilter, FieldEvaluator environment) {
        SorterHelper.SortType sortType = sortFilter.getSortType();
        if (sortType == null) {
            sortType = SorterHelper.SortType.ASC;
        }
        SorterHelper.FieldSorterAlgorithm SortAlgorithm = SORT_BY_DIRECTION.get(sortType);
        if (SortAlgorithm == null) {
            throw new IllegalArgumentException("Unknown sort direction : " + sortType);
        }
        return ((object, obj) -> SortAlgorithm.evaluate(object, sortFilter.getFieldName(), (String)environment.getFieldValue(obj, sortFilter.getFieldName()), environment));
    }
}
